package com.bonsay.ai.mcp;

import org.sqlite.SQLiteConfig;

import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SQLite-backed task store. Writes go through parameterised statements on a read-write
 * connection; ad-hoc agent SQL runs on a separate connection opened read-only at the driver
 * level AND with {@code PRAGMA query_only}, so even a validator bypass cannot mutate data.
 */
public class TaskRepository implements AutoCloseable {

    private static final Pattern SINGLE_SELECT =
            Pattern.compile("^\\s*(select|with)\\b[^;]*;?\\s*$", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(insert|update|delete|drop|alter|create|replace|attach|detach|pragma|vacuum|reindex)\\b",
            Pattern.CASE_INSENSITIVE);
    public static final int MAX_ROWS = 200;

    private final Connection rw;
    private final Connection ro;

    public TaskRepository(Path dbFile) throws SQLException {
        String url = "jdbc:sqlite:" + dbFile.toAbsolutePath();
        this.rw = DriverManager.getConnection(url);
        migrate();
        SQLiteConfig cfg = new SQLiteConfig();
        cfg.setReadOnly(true);
        this.ro = cfg.createConnection(url);
        try (Statement s = ro.createStatement()) {
            s.execute("PRAGMA query_only = ON");
        }
    }

    private void migrate() throws SQLException {
        try (Statement s = rw.createStatement()) {
            s.execute("""
                    CREATE TABLE IF NOT EXISTS tasks (
                        id           INTEGER PRIMARY KEY AUTOINCREMENT,
                        title        TEXT NOT NULL,
                        description  TEXT,
                        status       TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open','in_progress','done')),
                        priority     TEXT NOT NULL DEFAULT 'medium' CHECK (priority IN ('low','medium','high')),
                        due_date     TEXT,
                        created_at   TEXT NOT NULL DEFAULT (datetime('now')),
                        completed_at TEXT
                    )""");
            s.execute("CREATE INDEX IF NOT EXISTS idx_tasks_status ON tasks(status)");
        }
    }

    public synchronized Task create(String title, String description, String priority, String dueDate) {
        if (dueDate != null) {
            try {
                LocalDate.parse(dueDate);
            } catch (DateTimeParseException e) {
                throw new ToolException("due_date must be ISO-8601 (YYYY-MM-DD)");
            }
        }
        String sql = "INSERT INTO tasks(title, description, priority, due_date) VALUES (?,?,?,?)";
        try (PreparedStatement ps = rw.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, description);
            ps.setString(3, priority == null ? "medium" : priority);
            ps.setString(4, dueDate);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return findById(keys.getLong(1)).orElseThrow();
            }
        } catch (SQLException e) {
            throw new ToolException("could not create task: " + e.getMessage(), e);
        }
    }

    public synchronized List<Task> list(String status, String priority, int limit) {
        StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE 1=1");
        List<String> params = new ArrayList<>();
        if (status != null) { sql.append(" AND status = ?"); params.add(status); }
        if (priority != null) { sql.append(" AND priority = ?"); params.add(priority); }
        sql.append(" ORDER BY CASE priority WHEN 'high' THEN 0 WHEN 'medium' THEN 1 ELSE 2 END,"
                + " due_date IS NULL, due_date, id LIMIT ?");
        return query(sql.toString(), params, Math.min(limit, MAX_ROWS));
    }

    public synchronized List<Task> search(String text, int limit) {
        String like = "%" + text.replace("%", "\\%").replace("_", "\\_") + "%";
        return query("SELECT * FROM tasks WHERE title LIKE ? ESCAPE '\\' OR description LIKE ? ESCAPE '\\'"
                + " ORDER BY id DESC LIMIT ?", List.of(like, like), Math.min(limit, MAX_ROWS));
    }

    public synchronized Optional<Task> findById(long id) {
        try (PreparedStatement ps = rw.prepareStatement("SELECT * FROM tasks WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new ToolException(e.getMessage(), e);
        }
    }

    public synchronized Task updateStatus(long id, String status) {
        String sql = "UPDATE tasks SET status = ?, completed_at = CASE WHEN ? = 'done' THEN datetime('now') END WHERE id = ?";
        try (PreparedStatement ps = rw.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, status);
            ps.setLong(3, id);
            if (ps.executeUpdate() == 0) throw new ToolException("task " + id + " not found");
            return findById(id).orElseThrow();
        } catch (SQLException e) {
            throw new ToolException(e.getMessage(), e);
        }
    }

    public synchronized boolean delete(long id) {
        try (PreparedStatement ps = rw.prepareStatement("DELETE FROM tasks WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new ToolException(e.getMessage(), e);
        }
    }

    public synchronized Map<String, Integer> stats() {
        Map<String, Integer> out = new LinkedHashMap<>();
        try (Statement s = rw.createStatement();
             ResultSet rs = s.executeQuery("""
                     SELECT
                       SUM(status='open') AS open, SUM(status='in_progress') AS in_progress,
                       SUM(status='done') AS done,
                       SUM(status!='done' AND due_date < date('now')) AS overdue,
                       COUNT(*) AS total
                     FROM tasks""")) {
            rs.next();
            for (String c : List.of("open", "in_progress", "done", "overdue", "total")) out.put(c, rs.getInt(c));
            return out;
        } catch (SQLException e) {
            throw new ToolException(e.getMessage(), e);
        }
    }

    /** Runs a single, validated SELECT on the read-only connection and returns rows as maps. */
    public synchronized List<Map<String, Object>> readOnlyQuery(String sql) {
        if (!SINGLE_SELECT.matcher(sql).matches() || FORBIDDEN.matcher(sql).find()) {
            throw new ToolException("only a single read-only SELECT statement is allowed");
        }
        try (Statement s = ro.createStatement()) {
            s.setMaxRows(MAX_ROWS);
            s.setQueryTimeout(5);
            try (ResultSet rs = s.executeQuery(sql)) {
                ResultSetMetaData md = rs.getMetaData();
                List<Map<String, Object>> rows = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                    rows.add(row);
                }
                return rows;
            }
        } catch (SQLException e) {
            throw new ToolException("query failed: " + e.getMessage(), e);
        }
    }

    public String schema() {
        try (Statement s = rw.createStatement();
             ResultSet rs = s.executeQuery("SELECT sql FROM sqlite_master WHERE type IN ('table','index') AND sql IS NOT NULL")) {
            StringBuilder sb = new StringBuilder();
            while (rs.next()) sb.append(rs.getString(1)).append(";\n");
            return sb.toString();
        } catch (SQLException e) {
            throw new ToolException(e.getMessage(), e);
        }
    }

    private List<Task> query(String sql, List<String> params, int limit) {
        try (PreparedStatement ps = rw.prepareStatement(sql)) {
            int i = 1;
            for (String p : params) ps.setString(i++, p);
            ps.setInt(i, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> out = new ArrayList<>();
                while (rs.next()) out.add(map(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new ToolException(e.getMessage(), e);
        }
    }

    private static Task map(ResultSet rs) throws SQLException {
        return new Task(rs.getLong("id"), rs.getString("title"), rs.getString("description"),
                rs.getString("status"), rs.getString("priority"), rs.getString("due_date"),
                rs.getString("created_at"), rs.getString("completed_at"));
    }

    @Override
    public void close() throws SQLException {
        ro.close();
        rw.close();
    }
}
