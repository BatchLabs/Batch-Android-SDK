package com.batch.android.lisp;

import java.util.ArrayList;

/**
 * Context wrapper that's an union of multiple contexts
 * <p>
 * It should be initialized with the contexts in order of priority: first one to give a non "null"
 * (not to be confused with a BALPrimitiveValue with a nil type, which is considered a result)
 * will "win".
 */
public final class MetaContext implements EvaluationContext {

  private final ArrayList<EvaluationContext> contexts;

  public MetaContext(ArrayList<EvaluationContext> contexts) {
    this.contexts = contexts;
  }

  @Override
  public Value resolveVariableNamed(String name) {
    Value val;
    for (EvaluationContext ctx : contexts) {
      val = ctx.resolveVariableNamed(name);
      if (val != null) {
        return val;
      }
    }

    return null;
  }
}
