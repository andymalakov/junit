package edu.rice.cs.cunit.concJUnit;

import java.io.*;
import java.util.*;

/**
 * Class representing a conceptual error of a multithreaded test that has
 * information about the creation context of a thread.
 */
public abstract class CreationContextError extends MultithreadedTestError {
    /** Stack trace elements describing where the thread was started. */
    protected StackTraceElement[] _stes;
    
    /**
     * Constructs a new test error with the specified detail message and no
     * creation context.
     * @param message the detail message
     */
    public CreationContextError(String message) {
        this(message, new StackTraceElement[0]);
    }
    
    /**
     * Constructs a new multithreaded test error with the specified detail message and
     * creation context.
     * @param message the detail message
     * @param stes array of stack trace elements, describing where the thread was started
     */
    public CreationContextError(String message, StackTraceElement[] stes) {
        super(message);
        _stes = stes;
        setStackTrace(stes);
    }
    
    /**
     * Constructs a new multithreaded test error with the specified detail message and
     * creation context.
     * @param message the detail message
     * @param cc string with creation context, describing where the thread was started
     */
    public CreationContextError(String message, String cc) {
        this(message, parseCreationContextString(cc));
    }
    
    /**
     * Returns the creation context.
     * @return array of stack trace elements, describing where the thread was started
     */
    public StackTraceElement[] getCreationContext() { return _stes; }
    
    /**
     * Parse a creation context string.
     * @param cc creation context string (looks like a stack trace printed out).
     * @return array of stack trace elements, describing where the thread was started
     */
    public static StackTraceElement[] parseCreationContextString(String cc) {
        ArrayList<StackTraceElement> stes = new ArrayList<StackTraceElement>();
        if (cc!=null) try {
            BufferedReader br = new BufferedReader(new StringReader(cc));
            // first line is description, skip it
            String line;// = br.readLine();
//            if (line!=null) {
                while((line = br.readLine())!=null) {
                    // following lines: "\tat "+className+"."+methodName+"("+fileName+":"+lineNumber+")"
                    // with fileName and lineNumber optional
                    String className;
                    String methodName = "???";
                    String fileName = null;
                    int lineNumber = -1;
                    
                    if (line.startsWith("\tat ")) line = line.substring("\tat ".length());
                    int parenPos = line.indexOf('(');
                    String beforeParen = (parenPos>=0)?line.substring(0,parenPos):line;
                    int lastDot = beforeParen.lastIndexOf('.');
                    if (lastDot>=0) {
                        className = beforeParen.substring(0,lastDot);
                        methodName = beforeParen.substring(lastDot+1);
                    }
                    else {
                        className = beforeParen;
                    }
                    
                    if (parenPos>=0) {
                        String inParen = line.substring(parenPos+1, line.length()-1);
                        int colonPos = inParen.lastIndexOf(':');
                        if (colonPos>=0) {
                            fileName = inParen.substring(0,colonPos);
                            String ln = inParen.substring(colonPos+1);
                            try {
                                lineNumber = new Integer(ln);
                            }
                            catch(NumberFormatException nfe) { /* nothing to do */ }
                        }
                        else {
                            fileName = inParen;
                        }
                    }
                    
                    stes.add(new StackTraceElement(className, methodName, fileName, lineNumber));
                }
//            }
        }
        catch(IOException ie) { return new StackTraceElement[0]; }
        return stes.toArray(new StackTraceElement[stes.size()]);
    }
}
