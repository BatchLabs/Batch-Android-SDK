package com.batch.android.lisp;

public class OperatorValue extends Value {

    Operator operator;

    public OperatorValue(Operator operator) {
        this.operator = operator;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof OperatorValue)) {
            return false;
        } else {
            return this.operator.symbol.equals(((OperatorValue) obj).operator.symbol);
        }
    }

    @Override
    public String toString() {
        return "<Operator> Symbol: " + operator.symbol;
    }
}
