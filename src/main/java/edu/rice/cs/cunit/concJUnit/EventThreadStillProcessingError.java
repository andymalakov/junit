package edu.rice.cs.cunit.concJUnit;

/**
 * Class representing a conceptual error of a multithreaded test: the
 * event thread was still processing events after the test was over.
 */
public class EventThreadStillProcessingError extends NoJoinError {
    /**
     * Constructs a new "event thread still processing" test error with the
     * specified detail message.
     * @param message the detail message
     */
    public EventThreadStillProcessingError(String message) { super(message); }

    /**
     * Constructs a new "event thread still processing" test error with the
     * specified detail message and creation context.
     * @param message the detail message
     * @param stes array of stack trace elements, describing where the thread was started
     */
    public EventThreadStillProcessingError(String message, StackTraceElement[] stes) { super(message, stes); }
    
    /**
     * Constructs a new "event thread still processing" test error with the
     * specified detail message and creation context.
     * @param message the detail message
     * @param cc string with creation context, describing where the thread was started
     */
    public EventThreadStillProcessingError(String message, String cc) { super(message, cc); }
}
