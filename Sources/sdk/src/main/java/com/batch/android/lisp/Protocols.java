package com.batch.android.lisp;

import java.util.Set;

interface NumberOperation {
    boolean performOperation(Number referenceValue, Number currentValue);
}

interface SetOperation {
    boolean performOperation(Set<String> source, Set<String> target);
}

interface StringOperation {
    String performOperation(String string);
}
