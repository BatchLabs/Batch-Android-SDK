package com.batch.android.lisp;

public final class Operator
{
    final String symbol;

    final OperatorHandler handler;

    public Operator(String symbol, OperatorHandler handler)
    {
        this.symbol = symbol;
        this.handler = handler;
    }
}
