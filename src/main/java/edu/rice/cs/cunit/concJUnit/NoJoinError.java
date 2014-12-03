package edu.rice.cs.cunit.concJUnit;

/**
 * Class representing a conceptual error of a multithreaded test: the
 * thread did not end before the test was over.
 */
public class NoJoinError extends CreationContextError {
    /**
     * Constructs a new "no join" test error with the specified detail message.
     * @param message the detail message
     */
    public NoJoinError(String message) { super(message); }
    
    /**
     * Constructs a new "no join" test error with the specified detail message and
     * creation context.
     * @param message the detail message
     * @param stes array of stack trace elements, describing where the thread was started
     */
    public NoJoinError(String message, StackTraceElement[] stes) { super(message, stes); }
    
    /**
     * Constructs a new "no join" test error with the specified detail message and
     * creation context.
     * @param message the detail message
     * @param cc string with creation context, describing where the thread was started
     */
    public NoJoinError(String message, String cc) { super(message, cc); }
}
