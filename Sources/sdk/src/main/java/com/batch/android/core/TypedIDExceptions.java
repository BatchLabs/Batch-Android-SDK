package com.batch.android.core;

public class TypedIDExceptions {

    public static class InvalidIDException extends Exception {

        public InvalidIDException(String s) {
            super(s);
        }
    }

    public static class InvalidSeparatorException extends Exception {}

    public static class InvalidSizeException extends Exception {

        public InvalidSizeException(String s) {
            super(s);
        }
    }

    public static class InvalidTypeException extends Exception {

        public InvalidTypeException(String s) {
            super(s);
        }
    }

    public static class InvalidChecksumException extends Exception {

        public InvalidChecksumException(String s) {
            super(s);
        }
    }
}
