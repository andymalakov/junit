package edu.rice.cs.cunit.concJUnit;

/**
 * Class representing a conceptual error of a multithreaded test: the
 * thread end before the test was over, but there was no join.
 */
public class LuckyError extends CreationContextError {
    /**
     * Constructs a new "lucky" test error with the specified detail message.
     * @param message the detail message
     */
    public LuckyError(String message) { super(message); }
    
    /**
     * Constructs a new "lucky" test error with the specified detail message and
     * creation context.
     * @param message the detail message
     * @param stes array of stack trace elements, describing where the thread was started
     */
    public LuckyError(String message, StackTraceElement[] stes) { super(message, stes); }
    
    /**
     * Constructs a new "lucky" test error with the specified detail message and
     * creation context.
     * @param message the detail message
     * @param cc string with creation context, describing where the thread was started
     */
    public LuckyError(String message, String cc) { super(message, cc); }
}
