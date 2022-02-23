package com.batch.android.lisp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class LispTest {

    private final EvaluationContext context = new TestsEvaluationContext();

    // region Tests

    // region AST Tests

    @Test
    public void testBasicAST() {
        assertExpression(
            "(= 1 2)",
            new LiteralSExpression(new MockOperator("="), new PrimitiveValue(1), new PrimitiveValue(2))
        );

        assertExpression(
            "(= true (> 1 2))",
            new LiteralSExpression(
                new MockOperator("="),
                new PrimitiveValue(true),
                new LiteralSExpression(new MockOperator(">"), new PrimitiveValue(1), new PrimitiveValue(2))
            )
        );

        assertExpression(
            "(= `foo` \"bar\")",
            new LiteralSExpression(new MockOperator("="), new VariableValue("foo"), new PrimitiveValue("bar"))
        );

        assertExpression(
            "(= true \"bar\\n\\t\\r\\\"\\\'\\\\ \")",
            new LiteralSExpression(
                new MockOperator("="),
                new PrimitiveValue(true),
                new PrimitiveValue("bar\n\t\r\"\'\\ ")
            )
        );

        assertExpression(
            "(= true [\"a\" \"b\"])",
            new LiteralSExpression(
                new MockOperator("="),
                new PrimitiveValue(true),
                new PrimitiveValue(new HashSet<>(Arrays.asList("a", "b")))
            )
        );

        // Test that a set is a set and not an array
        assertExpression(
            "(= true [\"b\" \"a\" \"b\"])",
            new LiteralSExpression(
                new MockOperator("="),
                new PrimitiveValue(true),
                new PrimitiveValue(new HashSet<>(Arrays.asList("a", "b")))
            )
        );
    }

    @Test
    public void testMalformedExpressions() {
        assertParsingError("(= 2 2");
        assertParsingError("= 2 2");
        assertEvaluationError("(2 2)");
        assertEvaluationError("(2)");
    }

    @Test
    public void testVariables() {
        assertTrue("(= `foo` \"bar\")");
        assertTrue("(= `unknown` nil)");
    }

    @Test
    public void testSubexpressions() {
        assertTrue("(= true (= true true))");
        assertFalse("(= true (= true false))");
    }

    // endregion

    // region Operator tests

    @Test
    public void testEqualityOperator() {
        assertTrue("(=)");
        assertTrue("(= 2 2)");
        assertTrue("(= true true)");
        assertTrue("(= \"ok\" \"ok\")");

        assertFalse("(= 2 3)");
        assertFalse("(= true false)");
        assertFalse("(= \"ok\" \"not ok\")");

        // Can't compare values of different type
        assertFalse("(= 2 true)");
    }

    @Test
    public void testIfOperator() {
        assertTrue("(if true true false)");
        assertFalse("(if false true false)");
        assertTrue("(if (= 2 2) true false)");
        assertTrue("(= (if true 2 1) 2)");
        assertTrue("(= (if true \"ok\") \"ok\")");
        assertTrue("(= (if true [\"ok\"]) [\"ok\"])");
        assertTrue("(= nil (if false true))");
        assertTrue("(= nil (if true nil false))");

        assertEvaluationError("(if nil true false true)");
        assertEvaluationError("(if nil)");
        assertEvaluationError("(if)");
        assertEvaluationError("(if 2 true)");
        assertEvaluationError("(if \"ok\" true)");
        assertEvaluationError("(if [\"ok\"] true)");
    }

    @Test
    public void testNotOperator() {
        // Not only works with one boolean
        assertEvaluationError("(not)");
        assertEvaluationError("(not true true)");
        assertEvaluationError("(not 1)");
        assertEvaluationError("(not \"ok\")");

        assertFalse("(not true)");
        assertTrue("(not false)");
    }

    @Test
    public void testAndOperator() {
        assertFalse("(and true false)");
        assertFalse("(and false true)");
        assertTrue("(and true true)");
        assertFalse("(and false false)");

        assertFalse("(and true nil)");
        assertFalse("(and nil true)");

        assertEvaluationError("(and \"bar\" true)");
        assertFalse("(and false \"bar\")");
        assertEvaluationError("(and 2 true)");
        assertFalse("(and false 2)");
    }

    @Test
    public void testOrOperator() {
        assertTrue("(or true false)");
        assertTrue("(or false true)");
        assertTrue("(or true true)");
        assertFalse("(or false false)");

        assertTrue("(or true nil)");
        assertTrue("(or nil true)");
        assertFalse("(or false nil)");
        assertFalse("(or nil false)");

        assertEvaluationError("(or \"bar\" true)");
        assertTrue("(or true \"bar\")");
        assertEvaluationError("(or 2 true)");
        assertTrue("(or true 2)");
    }

    @Test
    public void testNumberComparisonOperators() {
        assertTrue("(> 3 2)");
        assertTrue("(> 3 2 1 0 -1)");
        assertFalse("(> 3 2 1 0 4)");
        assertFalse("(> 2 2)");
        assertFalse("(> 2 2 1)");
        assertFalse("(>= 3 2 1 0 4)");
        assertTrue("(>= 2 2)");
        assertTrue("(>= 2 2 1)");

        assertTrue("(< 2 3)");
        assertTrue("(< 2 3 4 5 6)");
        assertFalse("(< 2 3 4 5 6 1)");
        assertFalse("(< 2 2)");
        assertFalse("(< 2 2 3)");
        assertFalse("(<= 2 3 4 5 6 1)");
        assertTrue("(<= 2 2)");
        assertTrue("(<= 2 2 3)");
    }

    @Test
    public void testSetContains() {
        assertTrue("(contains \"foo\" [\"foo\" \"bar\"])");
        assertTrue("(contains [\"foo\"] [\"foo\" \"bar\"])");
        assertTrue("(contains [\"foo\" \"lorem\"] [\"foo\" \"bar\"])");
        assertFalse("(contains [\"lorem\"] [\"foo\" \"bar\"])");
        assertFalse("(contains [\"lorem\"] nil)");

        assertTrue("(containsAll \"foo\" [\"foo\" \"bar\"])");
        assertTrue("(containsAll [\"foo\"] [\"foo\" \"bar\"])");
        assertTrue("(containsAll [\"foo\" \"bar\"] [\"foo\" \"bar\" \"baz\"])");
        assertFalse("(containsAll [\"foo\" \"lorem\"] [\"foo\" \"bar\"])");
        assertFalse("(containsAll [\"lorem\"] [\"foo\" \"bar\"])");
        assertFalse("(containsAll [\"lorem\"] nil)");
    }

    @Test
    public void testStringCast() {
        assertTrue("(= (write-to-string 2) \"2\")");
        assertTrue("(= (write-to-string 2345678.123) \"2345678.123\")");
        assertTrue("(= (write-to-string true) \"true\")");
        assertTrue("(= (write-to-string false) \"false\")");
        assertTrue("(= (write-to-string \"ok\") \"ok\")");
        assertTrue("(= (write-to-string nil) nil)");

        assertEvaluationError("(write-to-string [\"test\"])");
    }

    @Test
    public void testStringToInteger() {
        assertTrue("(= (parse-string \"2345678.123\") 2345678.123)");
        assertTrue("(= (parse-string \"2\") 2)");
        assertTrue("(= (parse-string nil) nil)");

        assertEvaluationError("(parse-string [\"test\"])");
        assertEvaluationError("(parse-string 2)");
        assertEvaluationError("(parse-string true)");
    }

    @Test
    public void testCaseOperators() {
        assertTrue("(= (upper \"ilower\") \"ILOWER\")");
        assertTrue("(= (upper [\"first\" \"second\"]) [\"FIRST\" \"SECOND\"])");
        assertFalse("(= (upper [\"first\" \"second\"]) [\"first\" \"second\"])");
        assertTrue("(= (upper nil) nil)");

        assertEvaluationError("(upper true)");
        assertEvaluationError("(upper 2)");

        assertTrue("(= (lower \"ILOWER\") \"ilower\")");
        assertTrue("(= (lower [\"FIRST\" \"SECOND\"]) [\"first\" \"second\"])");
        assertFalse("(= (lower [\"FIRST\" \"SECOND\"]) [\"FIRST\" \"SECOND\"])");
        assertTrue("(= (lower nil) nil)");

        assertEvaluationError("(lower true)");
        assertEvaluationError("(lower 2)");
    }

    // endregion

    // endregion

    // region Convenience methods

    private void assertTrue(String expression) {
        assertBoolean(expression, true);
    }

    private void assertFalse(String expression) {
        assertBoolean(expression, false);
    }

    private void assertParsingError(String expression) {
        Value program = new Parser(expression).parse();

        if (program instanceof ErrorValue) {
            ErrorValue error = (ErrorValue) program;
            if (error.type == ErrorValue.Type.Parser) {
                return;
            }
        }

        fail("Expression should result in a parsing error: " + expression);
    }

    private void assertEvaluationError(String expression) {
        Value result = evaluate(expression);

        if (result instanceof ErrorValue) {
            ErrorValue error = (ErrorValue) result;
            if (error.type == ErrorValue.Type.Error) {
                return;
            }
        }

        fail("Expression should result in a runtime error: " + expression);
    }

    private void assertBoolean(String expression, boolean expectedResult) {
        Value result = evaluate(expression);

        if (result instanceof ErrorValue) {
            ErrorValue error = (ErrorValue) result;

            if (error.type == ErrorValue.Type.Parser) {
                fail(
                    "Expression should be " +
                    expectedResult +
                    ", but could not be parsed: " +
                    expression +
                    "Error: " +
                    error
                );
                return;
            }
            fail("Expression should be " + expectedResult + ", but errored: " + expression + "Error: " + error);
        } else if (result instanceof PrimitiveValue) {
            PrimitiveValue value = (PrimitiveValue) result;

            if (value.type != PrimitiveValue.Type.Bool) {
                fail("Expression result should be boolean");
                return;
            }

            if (value.value instanceof Boolean) {
                Boolean boolValue = (Boolean) value.value;

                assertEquals(
                    "Expression result should be " + expectedResult + ": " + expression,
                    expectedResult,
                    boolValue
                );

                return;
            }

            fail(
                "Expression result is of type bool, but the actual value could not be extracted. Looks like an internal error"
            );
        } else {
            fail(
                "Expression should be " +
                expectedResult +
                ", but executing it resulted in an unexpected value: " +
                expression
            );
        }
    }

    private void assertExpression(String expression, Value expectedAST) {
        Value parsedExpression = parse(expression);
        assertEquals(
            "Expression " + expression + " does not parse to expected AST: " + expectedAST,
            expectedAST,
            parsedExpression
        );
    }

    private Value parse(String expression) {
        return new Parser(expression).parse();
    }

    private Value evaluate(String expression) {
        Value program = new Parser(expression).parse();
        if (program instanceof Reduceable) {
            return ((Reduceable) program).reduce(context);
        }
        return program;
    }
    // endregion
}

class TestsEvaluationContext implements EvaluationContext {

    @Override
    public Value resolveVariableNamed(String name) {
        if (name.toLowerCase(Locale.US).equals("foo")) {
            return new PrimitiveValue("bar");
        }
        return PrimitiveValue.nilValue();
    }
}

class LiteralSExpression extends SExpression {

    LiteralSExpression(Value... elements) {
        super(new ArrayList<>(Arrays.asList(elements)));
    }
}

class MockOperator extends OperatorValue {

    MockOperator(String symbol) {
        super(new Operator(symbol, (context, values) -> PrimitiveValue.nilValue()));
    }
}
