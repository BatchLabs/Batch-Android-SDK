package com.batch.android.lisp;

public interface EvaluationContext
{
    Value resolveVariableNamed(String name);
}
