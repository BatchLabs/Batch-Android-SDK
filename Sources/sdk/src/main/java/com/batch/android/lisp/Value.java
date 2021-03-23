package com.batch.android.lisp;

import java.util.Set;

/**
 * Root of everything.
 * <p>
 * Everything in our LISP is a Value.
 * <p>
 * A value is abstract, but has different concrete implementations:
 * - SExpression: An executable list of values, which should always begin by a Operator, if not empty
 * - Variable: A variable reference
 * - Error: An error, described by kind and message. Errors should bubble as soon as possible
 * - Primitive: A primitive value (Nil, Double, Bool, String, String Array)
 * - Operator: A builtin function
 * <p>
 * Reducable values are not usuable as-is, and must be resolved to get a primitive value.
 * Reducing SExpressions is how you compute a result, as the program's root must be a S-Expression.
 * Operators cannot be reduced, and must only be used as the first value of a S-Expression
 * <p>
 * Please note that all concepts here are the result of our take on LISP, and might be different than existing
 * implementations.
 */
public class Value
{
    final static String escapedString(String string)
    {
        String value = string.replaceAll("\n", "\\n");
        value = value.replaceAll("\r", "\\r");
        value = value.replaceAll("\t", "\\t");
        value = value.replaceAll("\'", "\\'");
        value = value.replaceAll("\"", "\\\"");
        return value;
    }

    final static String setToString(Set<String> set)
    {
        if (!set.getClass().isInstance(Set.class)) {
            return "[error]";
        }

        StringBuilder toString = new StringBuilder();

        toString.append("[");

        int idx = 0;
        for (String value : set) {
            if (idx > 0) {
                toString.append(" ");
            }
            toString.append("\"" + escapedString(value) + "\"");
            idx += 1;
        }

        toString.append("]");

        return toString.toString();
    }
}
