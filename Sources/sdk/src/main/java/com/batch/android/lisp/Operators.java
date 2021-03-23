package com.batch.android.lisp;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class OperatorProvider
{
    private final HashMap<String, Operator> _operators;

    private static NumberFormat numberFormatter;

    OperatorProvider()
    {
        _operators = new HashMap<>();
        setupOperators();
    }

    @Nullable
    Operator operatorForSymbol(String symbol)
    {
        return _operators.get(symbol);
    }

    private void setupOperators()
    {
        addOperator(new Operator("if", new IfOperatorHandler()));
        addOperator(new Operator("and", new AndOperatorHandler()));
        addOperator(new Operator("or", new OrOperatorHandler()));
        addOperator(new Operator("=", new EqualOperatorHandler()));
        addOperator(new Operator("not", new NotOperatorHandler()));
        addOperator(new Operator(">", new GreaterThanOperatorHandler()));
        addOperator(new Operator(">=", new GreaterThanOrEqualOperatorHandler()));
        addOperator(new Operator("<", new LessThanOperatorHandler()));
        addOperator(new Operator("<=", new LessThanOrEqualOperatorHandler()));
        addOperator(new Operator("contains", new ContainsOperatorHandler()));
        addOperator(new Operator("containsAll", new ContainsAllOperatorHandler()));
        addOperator(new Operator("upper", new UpperOperatorHandler()));
        addOperator(new Operator("lower", new LowerOperatorHandler()));
        addOperator(new Operator("parse-string", new ParseStringOperatorHandler()));
        addOperator(new Operator("write-to-string", new WriteToStringOperatorHandler()));
    }

    private void addOperator(Operator operator)
    {
        _operators.put(operator.symbol.toLowerCase(Locale.US), operator);
    }

    static String numberToString(Number number)
    {
        if (numberFormatter == null) {
            numberFormatter = NumberFormat.getNumberInstance(Locale.US);
            numberFormatter.setParseIntegerOnly(false);
            numberFormatter.setGroupingUsed(false);
        }
        return numberFormatter.format(number);
    }

    static Value performNumberOperationOnValues(ArrayList<PrimitiveValue> values,
                                                String symbol,
                                                NumberOperation operation)
    {
        if (values.size() < 2) {
            return new ErrorValue(ErrorValue.Type.Error,
                    symbol + ": requires at least two arguments");
        }

        PrimitiveValue referenceValue = values.get(0);
        if (referenceValue.type != PrimitiveValue.Type.Double) {
            return new ErrorValue(ErrorValue.Type.Error, symbol + ": arguments should be numbers");
        }

        if (!( referenceValue.value instanceof Number )) {
            return new ErrorValue(ErrorValue.Type.Error,
                    symbol + ": consistency error: underlying types should be NSNumbers");
        }

        Number referenceNumber = (Number) referenceValue.value;

        for (int i = 1; i < values.size(); i++) {
            PrimitiveValue atomValue = values.get(i);

            if (atomValue == null) {
                return new ErrorValue(ErrorValue.Type.Internal,
                        "=: value can't be nil. Are we out of bounds?");
            }

            if (atomValue.type != PrimitiveValue.Type.Double) {
                return new ErrorValue(ErrorValue.Type.Error,
                        symbol + ": arguments should be numbers");
            }

            if (!( atomValue.value instanceof Number )) {
                return new ErrorValue(ErrorValue.Type.Error,
                        symbol + ": consistency error: underlying types should be NSNumbers");
            }

            if (!operation.performOperation(referenceNumber, (Number) atomValue.value)) {
                return new PrimitiveValue(false);
            }
        }

        return new PrimitiveValue(true);
    }

    static Value performSetOperationOnValues(ArrayList<PrimitiveValue> values,
                                             String symbol,
                                             SetOperation operation)
    {
        if (values.size() != 2) {
            return new ErrorValue(ErrorValue.Type.Error, symbol + ": only accepts two arguments");
        }

        // The set that the target should contain
        PrimitiveValue wantedSetPrimitive = values.get(0);
        // Automatically convert string primitives
        if (wantedSetPrimitive.type == PrimitiveValue.Type.String) {
            Set<String> set = new HashSet<>();
            set.add((String) wantedSetPrimitive.value);
            wantedSetPrimitive = new PrimitiveValue(set);
        }

        // The set that should contains the wanted values
        PrimitiveValue targetSetPrimitive = values.get(1);
        // If the searched set is nil, return false
        if (targetSetPrimitive.type == PrimitiveValue.Type.Nil) {
            return new PrimitiveValue(false);
        }

        if (wantedSetPrimitive.type != PrimitiveValue.Type.StringSet ||
                targetSetPrimitive.type != PrimitiveValue.Type.StringSet)
        {
            return new ErrorValue(ErrorValue.Type.Error,
                    symbol + ": all arguments should be string sets");
        }

        if (!( wantedSetPrimitive.value instanceof Set ) || !( targetSetPrimitive.value instanceof Set )) {
            return new ErrorValue(ErrorValue.Type.Error,
                    symbol + ": internal consistency error: all arguments should be of underlying type Set");
        }

        @SuppressWarnings("unchecked")
        Set<String> wantedSet = (Set<String>) wantedSetPrimitive.value;
        @SuppressWarnings("unchecked")
        Set<String> targetSet = (Set<String>) targetSetPrimitive.value;

        if (operation.performOperation(wantedSet, targetSet)) {
            return new PrimitiveValue(true);
        }

        return new PrimitiveValue(false);
    }

    /**
     * Perform a block on a string or every string in a string set
     * Errors out if variable isn't a string/string set
     */
    static Value performStringOperationOnValues(ArrayList<PrimitiveValue> values,
                                                String symbol,
                                                StringOperation operation)
    {
        if (values.size() != 1) {
            return new ErrorValue(ErrorValue.Type.Error,
                    symbol + ": requires only one string/set argument");
        }

        PrimitiveValue referenceValue = values.get(0);
        Object value = referenceValue.value;
        if (referenceValue.type == PrimitiveValue.Type.Nil) {
            return PrimitiveValue.nilValue();
        } else if (referenceValue.type == PrimitiveValue.Type.String) {
            if (!( value instanceof String )) {
                return new ErrorValue(ErrorValue.Type.Internal,
                        symbol + ": consistency error: underlying types should be String");
            }
            return new PrimitiveValue(operation.performOperation((String) value));
        } else if (referenceValue.type == PrimitiveValue.Type.StringSet) {
            if (!( referenceValue.value instanceof Set )) {
                return new ErrorValue(ErrorValue.Type.Internal,
                        symbol + ": consistency error: underlying types should be String");
            }

            @SuppressWarnings("unchecked")
            Set<String> setValue = (Set<String>) value;
            Set<String> resultSet = new HashSet<>(setValue.size());
            for (String element : setValue) {
                resultSet.add(operation.performOperation(element));
            }

            return new PrimitiveValue(resultSet);
        }


        return new ErrorValue(ErrorValue.Type.Error,
                symbol + ": argument should be a string or a set");
    }
}

class IfOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        int nbArgs = values.size();
        if (nbArgs < 2 || nbArgs > 3) {
            return new ErrorValue(ErrorValue.Type.Error,
                    "if: should be called with 2 or 3 arguments");
        }

        PrimitiveValue trueValue = values.get(1);
        PrimitiveValue falseValue;
        if (nbArgs == 3) {
            falseValue = values.get(2);
        } else {
            falseValue = PrimitiveValue.nilValue();
        }

        PrimitiveValue condition = values.get(0);
        if (condition.type == PrimitiveValue.Type.Nil) {
            return falseValue;
        } else if (condition.type == PrimitiveValue.Type.Bool) {
            if (!( condition.value instanceof Boolean )) {
                return new ErrorValue(ErrorValue.Type.Internal,
                        "if: internal consistency error: boolean value should have an underlying Boolean");
            }
            return ( (Boolean) condition.value ) ? trueValue : falseValue;
        } else {
            return new ErrorValue(ErrorValue.Type.Error,
                    "if: condition should be nil or a boolean value");
        }
    }
}

class AndOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        if (values.isEmpty()) {
            return new PrimitiveValue(true);
        }

        for (PrimitiveValue atomValue : values) {
            if (atomValue.type == PrimitiveValue.Type.Nil) {
                // Nil values are like false
                return new PrimitiveValue(false);
            }

            if (atomValue.type != PrimitiveValue.Type.Bool || !( atomValue.value instanceof Boolean )) {
                return new ErrorValue(ErrorValue.Type.Error,
                        "and: Cannot compare non boolean values");
            }

            if (!(Boolean) atomValue.value) {
                return new PrimitiveValue(false);
            }
        }
        return new PrimitiveValue(true);
    }
}

class OrOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        if (values.isEmpty()) {
            return new PrimitiveValue(true);
        }

        for (PrimitiveValue atomValue : values) {
            if (atomValue.type == PrimitiveValue.Type.Nil) {
                // Nil values are like false
                continue;
            }

            if (atomValue.type != PrimitiveValue.Type.Bool || !( atomValue.value instanceof Boolean )) {
                return new ErrorValue(ErrorValue.Type.Error,
                        "or: Cannot compare non boolean values");
            }

            if ((Boolean) atomValue.value) {
                return new PrimitiveValue(true);
            }
        }
        return new PrimitiveValue(false);
    }
}

class EqualOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        if (values.size() <= 1) {
            return new PrimitiveValue(true);
        }

        PrimitiveValue firstAtomValue = values.get(0);
        if (firstAtomValue == null) {
            return new ErrorValue(ErrorValue.Type.Internal, "=: first value shouldn't be nil");
        }

        Object firstValue = firstAtomValue.value;

        for (int i = 1; i < values.size(); i++) {
            PrimitiveValue atomValue = values.get(i);

            if (atomValue == null) {
                return new ErrorValue(ErrorValue.Type.Internal,
                        "=: value can't be nil. Are we out of bounds?");
            }

            if (firstAtomValue.type != atomValue.type) {
                return new PrimitiveValue(false);
            }

            Object value = atomValue.value;

            if (firstValue == value) { // Handles 'nil' and static strings
                continue;
            }

            if (!firstValue.equals(value)) {
                return new PrimitiveValue(false);
            }
        }

        return new PrimitiveValue(true);
    }
}

class NotOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        if (values.size() != 1) {
            return new ErrorValue(ErrorValue.Type.Error, "not: only accepts one argument");
        }

        PrimitiveValue firstAtomValue = values.get(0);

        if (firstAtomValue.type != PrimitiveValue.Type.Bool || !( firstAtomValue.value instanceof Boolean )) {
            return new ErrorValue(ErrorValue.Type.Error, "not: argument should be a boolean");
        }

        return new PrimitiveValue(!(Boolean) firstAtomValue.value);
    }
}

class GreaterThanOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        return OperatorProvider.performNumberOperationOnValues(values, ">",
                (referenceValue, currentValue) -> {
                    return referenceValue.doubleValue() > currentValue.doubleValue(); // dirty for now... dirty... forever?
                });
    }
}

class GreaterThanOrEqualOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        return OperatorProvider.performNumberOperationOnValues(values, ">=",
                (referenceValue, currentValue) -> referenceValue.doubleValue() > currentValue.doubleValue() || referenceValue.equals(
                        currentValue));
    }
}

class LessThanOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        return OperatorProvider.performNumberOperationOnValues(values, "<",
                (referenceValue, currentValue) -> {
                    return referenceValue.doubleValue() < currentValue.doubleValue(); // dirty for now... dirty... forever?
                });
    }
}

class LessThanOrEqualOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        return OperatorProvider.performNumberOperationOnValues(values, "<=",
                (referenceValue, currentValue) -> referenceValue.doubleValue() < currentValue.doubleValue() || referenceValue.equals(
                        currentValue));
    }
}

/**
 * Contains takes two arguments:
 * - First is a set. If the argument is a string, it is automatically converted to a set
 * - Second is a set (the searched one)
 * It returns true if the second set contains ANY of the elements of the first set.
 */
class ContainsOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        return OperatorProvider.performSetOperationOnValues(values,
                "contains",
                (source, target) -> {
                    for (String element : source) {
                        if (target.contains(element)) {
                            return true;
                        }
                    }
                    return false;
                });
    }
}

/**
 * ContainsAll takes two arguments:
 * - First is a set. If the argument is a string, it is automatically converted to a set
 * - Second is a set (the searched one)
 * It returns true if the second set contains ALL of the elements of the first set.
 */
class ContainsAllOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        return OperatorProvider.performSetOperationOnValues(values,
                "contains-all",
                (source, target) -> target.containsAll(source));
    }
}

class LowerOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        return OperatorProvider.performStringOperationOnValues(values,
                "lower",
                string -> string.toLowerCase(Locale.US));
    }
}

class UpperOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        return OperatorProvider.performStringOperationOnValues(values,
                "upper",
                string -> string.toUpperCase(Locale.US));
    }
}

/**
 * Returns the double value of a string
 */
class ParseStringOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        if (values.size() != 1) {
            return new ErrorValue(ErrorValue.Type.Error,
                    "parse-string: only accepts one string argument");
        }

        PrimitiveValue value = values.get(0);
        if (value.type == PrimitiveValue.Type.Nil) {
            return PrimitiveValue.nilValue();
        }

        if (value.type != PrimitiveValue.Type.String) {
            return new ErrorValue(ErrorValue.Type.Error,
                    "parse-string: only accepts a string argument");
        }

        String strToParse = (String) value.value;

        return new PrimitiveValue(Double.valueOf(strToParse));
    }
}

/**
 * Returns the string value of anything but a set
 * Returns a nil value for nil inputs
 */
class WriteToStringOperatorHandler implements OperatorHandler
{
    @Override
    public Value run(EvaluationContext context, ArrayList<PrimitiveValue> values)
    {
        if (values.size() != 1) {
            return new ErrorValue(ErrorValue.Type.Error,
                    "write-to-string: only accepts a single, non-set argument");
        }

        PrimitiveValue value = values.get(0);
        switch (value.type) {
            case String:
                return value;
            case Nil:
                return PrimitiveValue.nilValue();
            case Bool:
                if (!( value.value instanceof Boolean )) {
                    return new ErrorValue(ErrorValue.Type.Internal,
                            "write-to-string: internal consistency error: argument should be of underlying type Boolean");
                }
                return new PrimitiveValue((Boolean) value.value ? "true" : "false");
            case Double:
                if (!( value.value instanceof Number )) {
                    return new ErrorValue(ErrorValue.Type.Internal,
                            "write-to-string: internal consistency error: argument should be of underlying type Number");
                }
                return new PrimitiveValue(OperatorProvider.numberToString((Number) value.value));
            default:
                return new ErrorValue(ErrorValue.Type.Error,
                        "write-to-string: only accepts a single, non-set argument");
        }
    }
}
