package com.batch.android.lisp;

import androidx.annotation.NonNull;
import java.util.Locale;

public class VariableValue extends Value implements Reduceable {

    final String name;

    public VariableValue(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof VariableValue)) {
            return false;
        } else {
            return name.equals(((VariableValue) obj).name);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "`" + name + "`";
    }

    @Override
    public Value reduce(EvaluationContext context) {
        Value resolved = context.resolveVariableNamed(name.toLowerCase(Locale.US));
        if (resolved == null) {
            resolved = PrimitiveValue.nilValue();
        }
        return resolved;
    }
}
