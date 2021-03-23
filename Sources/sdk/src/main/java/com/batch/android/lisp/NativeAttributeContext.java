package com.batch.android.lisp;

import android.content.Context;

import com.batch.android.core.SystemParameterHelper;

public final class NativeAttributeContext implements EvaluationContext
{
    final Context context;

    public NativeAttributeContext(Context context)
    {
        this.context = context;
    }

    @Override
    public Value resolveVariableNamed(String name)
    {
        if (name.startsWith("b.") && name.length() > 2) {
            String parameter = SystemParameterHelper.getValue(name.substring(2), context);

            if (parameter != null && parameter.length() > 0) {
                return new PrimitiveValue(parameter);
            } else {
                return PrimitiveValue.nilValue();
            }
        }

        return null;
    }
}

