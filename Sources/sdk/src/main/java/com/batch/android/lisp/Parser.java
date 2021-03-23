package com.batch.android.lisp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Parser
{
    private final static char TOKEN_DELIMITER_VARIABLE = '`';
    private final static char TOKEN_DELIMITER_STRING = '"';
    private final static char TOKEN_DELIMITER_LIST_START = '(';
    private final static char TOKEN_DELIMITER_STRING_ARRAY_START = '[';

    private boolean isConsumed;
    private int _pos;
    private final int _maxPos;
    private final String input;
    private boolean endReached;
    private final OperatorProvider operators;
    private final NumberFormat numberFormatter;

    public Parser(String expression)
    {
        isConsumed = false;
        input = stringByTrimmingWhiteSpaceAndNewline(expression);
        _maxPos = Math.max(0, input.length() - 1);
        endReached = _maxPos == 0;
        _pos = 0;
        operators = new OperatorProvider();
        numberFormatter = NumberFormat.getInstance(Locale.US);
        numberFormatter.setParseIntegerOnly(false);
    }

    public Value parse()
    {
        if (isConsumed) {
            return errorWithMessage(
                    "This parser has already been consumed. Please instantiate a new one.");
        }

        isConsumed = true;

        if (getNextChar() == TOKEN_DELIMITER_LIST_START) {
            return parseList();
        }

        return errorWithPositionAndMessage("Expected " + TOKEN_DELIMITER_LIST_START);
    }

    private Character getNextChar()
    {
        if (endReached) {
            return '\0';
        }

        Character c = input.charAt(_pos);
        if (_pos == _maxPos) {
            endReached = true;
        } else {
            _pos++;
        }
        return c;
    }

    private Value parseList()
    {
        // ( has already been consumed
        ArrayList<Value> values = new ArrayList<>();
        StringBuilder tokenAccumulator = null;

        while (!endReached) {
            Character c = getNextChar();

            if (c == ')' || c == ' ') {
                // Both can mark the end of a symbol
                if (tokenAccumulator != null) {
                    // Unprefixed values can either be values or operators
                    Value tmpVal = parseSpecial(tokenAccumulator.toString());

                    if (tmpVal == null) {
                        tmpVal = parseOperator(tokenAccumulator.toString());
                    }

                    if (tmpVal == null) {
                        tmpVal = parseNumber(tokenAccumulator.toString());
                    }

                    if (tmpVal == null) {
                        tmpVal = errorWithMessage("Unknown symbol '" + tokenAccumulator.toString() + "': It is not an operator, and it could not be converted to a number");
                    }

                    values.add(tmpVal);
                    tokenAccumulator = null;
                }

                if (c == ')') {
                    for (Value val : values) {
                        if (val instanceof ErrorValue) {
                            return val;
                        }
                    }
                    return new SExpression(values);
                }
            } else if (c == TOKEN_DELIMITER_STRING) {
                values.add(parseString());
            } else if (c == TOKEN_DELIMITER_VARIABLE) {
                values.add(parseVariable());
            } else if (c == TOKEN_DELIMITER_LIST_START) {
                values.add(parseList());
            } else if (c == TOKEN_DELIMITER_STRING_ARRAY_START) {
                values.add(parseStringArray());
            } else {
                if (tokenAccumulator == null) {
                    tokenAccumulator = new StringBuilder();
                }
                tokenAccumulator.append(String.valueOf(c));
            }
        }

        return errorUnexpectedEOF(")");
    }

    private Value parseStringArray()
    {
        Set<String> values = new HashSet<>();

        while (!endReached) {
            Character c = getNextChar();

            if (c == ']') {
                return new PrimitiveValue(values);
            } else if (c == TOKEN_DELIMITER_STRING) {
                Value stringValue = parseString();

                if (stringValue instanceof ErrorValue) {
                    return stringValue;
                } else if (stringValue instanceof PrimitiveValue &&
                        ( (PrimitiveValue) stringValue ).type == PrimitiveValue.Type.String &&
                        ( (PrimitiveValue) stringValue ).value instanceof String)
                {
                    values.add((String) ( (PrimitiveValue) stringValue ).value);
                } else {
                    return errorWithPositionAndMessage(
                            "Internal parser error: value is not a string nor an error");
                }
            } else if (c != ' ') {
                return errorWithPositionAndMessage("Unexpected character '" + c + "' in string array, expected \" or ]");
            }
        }

        return errorUnexpectedEOF("]");
    }

    private Value parseString()
    {
        StringBuilder accumulator = new StringBuilder();
        boolean isEscaping = false;

        while (!endReached) {
            Character c = getNextChar();

            if (isEscaping) {
                switch (c) {
                    case '\\': // Actually \
                        c = '\\';
                        break;
                    case 'n':
                        c = '\n';
                        break;
                    case 't':
                        c = '\t';
                        break;
                    case 'r':
                        c = '\r';
                        break;
                    case '"':
                        c = '"';
                        break;
                    case '\'': // Actually '
                        c = '\'';
                        break;
                    default:
                        return errorWithPositionAndMessage("Invalid escaped character: \\" + c);
                }

                isEscaping = false;
            } else if (c == '\\') {
                isEscaping = true;
                continue;
            } else if (c == TOKEN_DELIMITER_STRING) {
                return new PrimitiveValue(accumulator.toString());
            }

            accumulator.append(String.valueOf(c));
        }

        return errorUnexpectedEOF(String.valueOf(TOKEN_DELIMITER_STRING));
    }

    @Nullable
    private PrimitiveValue parseSpecial(String token)
    {
        token = token.toLowerCase(Locale.US);

        switch (token) {
            case "true":
                return new PrimitiveValue(true);
            case "false":
                return new PrimitiveValue(false);
            case "nil":
            case "null":
                return PrimitiveValue.nilValue();
        }

        return null;
    }

    @Nullable
    private PrimitiveValue parseNumber(String numberString)
    {
        try {
            Number parsedNumber = numberFormatter.parse(numberString);
            return new PrimitiveValue(parsedNumber.doubleValue());
        } catch (ParseException e) {
            return null;
        }
    }

    @Nullable
    private OperatorValue parseOperator(String symbol)
    {
        Operator op = operators.operatorForSymbol(symbol.toLowerCase(Locale.US));

        if (op == null) {
            return null;
        }

        return new OperatorValue(op);
    }

    private Value parseVariable()
    {
        StringBuilder accumulator = new StringBuilder();

        while (!endReached) {
            Character c = getNextChar();

            if (c == TOKEN_DELIMITER_VARIABLE) {
                String trimmed = stringByTrimmingWhiteSpaceAndNewline(accumulator.toString());
                if (trimmed.length() == 0) {
                    return errorWithMessage("Variables cannot have an empty name");
                }
                return new VariableValue(accumulator.toString());
            }

            accumulator.append(c);
        }
        return errorUnexpectedEOF(String.valueOf(TOKEN_DELIMITER_VARIABLE));
    }

    @NonNull
    private ErrorValue errorWithMessage(String message)
    {
        return new ErrorValue(ErrorValue.Type.Parser, message);
    }

    @NonNull
    private ErrorValue errorWithPositionAndMessage(String message)
    {
        return new ErrorValue(ErrorValue.Type.Parser, "At position" + _pos + message);
    }

    @NonNull
    private ErrorValue errorUnexpectedEOF(String expected)
    {
        return new ErrorValue(ErrorValue.Type.Parser, "Unexpected EOF. Expected: " + expected);
    }

    @NonNull
    private String stringByTrimmingWhiteSpaceAndNewline(String source)
    {
        return source.replaceAll("[\n\r]", "");
    }
}
