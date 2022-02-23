package com.batch.android.lisp;

/**
 * Defines reducable values
 * <p>
 * A reduceable value can be reduced to another Value (usually a Primitive or Error)
 */
public interface Reduceable {
    Value reduce(EvaluationContext context);
}
