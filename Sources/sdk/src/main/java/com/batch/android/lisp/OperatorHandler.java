package com.batch.android.lisp;

import java.util.ArrayList;

public interface OperatorHandler {
    Value run(EvaluationContext context, ArrayList<PrimitiveValue> values);
}
