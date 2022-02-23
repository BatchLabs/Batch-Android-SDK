package com.batch.android.lisp;

import java.util.ArrayList;

public class SExpression extends Value implements Reduceable {

    final ArrayList<Value> values;

    public SExpression(ArrayList<Value> values) {
        this.values = new ArrayList<>(values);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof SExpression)) {
            return false;
        } else {
            return values.equals(((SExpression) obj).values);
        }
    }

    @Override
    public Value reduce(EvaluationContext context) {
        if (values.isEmpty()) {
            return PrimitiveValue.nilValue();
        }

        Value firstValue = values.get(0);
        if (!(firstValue instanceof OperatorValue)) {
            return new ErrorValue(ErrorValue.Type.Error, "S-Expressions should have an operator as their first value");
        }

        ArrayList<PrimitiveValue> arguments = new ArrayList<>(Math.max(0, values.size() - 1));
        for (int i = 1; i < values.size(); i++) {
            Value val = values.get(i);

            if (val instanceof Reduceable) {
                val = ((Reduceable) val).reduce(context);
            }

            if (val == null) {
                return new ErrorValue(ErrorValue.Type.Internal, "Unexpected nil value while reducing S-Expression");
            }

            if (val instanceof ErrorValue) {
                return val;
            }

            if (!(val instanceof PrimitiveValue)) {
                return new ErrorValue(
                    ErrorValue.Type.Internal,
                    "Error while reducing S-Expression: at this point, value should be a PrimitiveValue"
                );
            }

            arguments.add((PrimitiveValue) val);
        }

        Operator operator = ((OperatorValue) firstValue).operator;

        if (operator == null) {
            return new ErrorValue(ErrorValue.Type.Internal, "Can't reduce a S-Expression with a nil operator");
        }

        return operator.handler.run(context, arguments);
    }

    @Override
    public String toString() {
        StringBuilder toString = new StringBuilder("(");

        for (int i = 0; i < values.size(); i++) {
            if (i != 0) {
                toString.append(" ");
            }

            toString.append(values.get(i).toString());
        }

        toString.append(")");

        return toString.toString();
    }
}
