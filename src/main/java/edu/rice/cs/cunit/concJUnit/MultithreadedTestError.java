package edu.rice.cs.cunit.concJUnit;

/**
 * Class representing a conceptual error of a multithreaded test.
 */
public class MultithreadedTestError extends java.lang.Error {
    /**
     * Constructs a new multithreaded test error with the specified detail message.
     * @param message the detail message
     */
    public MultithreadedTestError(String message) {
        super(message);
    }
    
    /**
     * Constructs a new multithreaded test error with the specified detail message and cause.
     * @param message the detail message
     * @param cause exception that caused this exception
     */
    public MultithreadedTestError(String message, Throwable cause) {
        super(message, cause);
    }
}