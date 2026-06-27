package com.ehrapi.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * A clinical institution (hospital, clinic, private practice) participating in
 * the network. Patients have a home institution and may consent to share their
 * record with other institutions.
 */
@Entity
@Table(name = "institutions")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "institutions_seq")
    @SequenceGenerator(name = "institutions_seq", sequenceName = "INSTITUTIONS_ID_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 50)
    private String type;

    @Column(length = 500)
    private String address;

    @Column(length = 50)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "date_created")
    private LocalDateTime dateCreated;

    public Institution() {
        this.dateCreated = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getDateCreated() { return dateCreated; }
    public void setDateCreated(LocalDateTime dateCreated) { this.dateCreated = dateCreated; }
}
