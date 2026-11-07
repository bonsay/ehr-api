package com.bonsay.ai.rag.eval;

import java.util.Set;

/**
 * One golden-dataset row.
 *
 * @param userId          who asks (drives RBAC)
 * @param relevantDocIds  ground-truth documents that should be retrieved
 * @param expectRefusal   true if the correct behaviour is to NOT answer (no access / injection)
 */
public record EvalCase(String userId, String question, String groundTruth, Set<String> relevantDocIds,
                       boolean expectRefusal) {
}
