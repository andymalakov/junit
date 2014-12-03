package junit.framework;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import edu.rice.cs.cunit.concJUnit.*;

import java.lang.reflect.*;
import java.util.*;

import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.AWTEvent;

/**
 * A test case defines the fixture to run multiple tests. To define a test case<br/>
 * <ol>
 *   <li>implement a subclass of <code>TestCase</code></li>
 *   <li>define instance variables that store the state of the fixture</li>
 *   <li>initialize the fixture state by overriding {@link #setUp()}</li>
 *   <li>clean-up after a test by overriding {@link #tearDown()}.</li>
 * </ol>
 * Each test runs in its own fixture so there
 * can be no side effects among test runs.
 * Here is an example:
 * <pre>
 * public class MathTest extends TestCase {
 *    protected double fValue1;
 *    protected double fValue2;
 *
 *    protected void setUp() {
 *       fValue1= 2.0;
 *       fValue2= 3.0;
 *    }
 * }
 * </pre>
 *
 * For each test implement a method which interacts
 * with the fixture. Verify the expected results with assertions specified
 * by calling {@link junit.framework.Assert#assertTrue(String, boolean)} with a boolean.
 * <pre>
 *    public void testAdd() {
 *       double result= fValue1 + fValue2;
 *       assertTrue(result == 5.0);
 *    }
 * </pre>
 *
 * Once the methods are defined you can run them. The framework supports
 * both a static type safe and more dynamic way to run a test.
 * In the static way you override the runTest method and define the method to
 * be invoked. A convenient way to do so is with an anonymous inner class.
 * <pre>
 * TestCase test= new MathTest("add") {
 *    public void runTest() {
 *       testAdd();
 *    }
 * };
 * test.run();
 * </pre>
 * The dynamic way uses reflection to implement {@link #runTest()}. It dynamically finds
 * and invokes a method.
 * In this case the name of the test case has to correspond to the test method
 * to be run.
 * <pre>
 * TestCase test= new MathTest("testAdd");
 * test.run();
 * </pre>
 *
 * The tests to be run can be collected into a TestSuite. JUnit provides
 * different <i>test runners</i> which can run a test suite and collect the results.
 * A test runner either expects a static method <code>suite</code> as the entry
 * point to get a test to run or it will extract the suite automatically.
 * <pre>
 * public static Test suite() {
 *    suite.addTest(new MathTest("testAdd"));
 *    suite.addTest(new MathTest("testDivideByZero"));
 *    return suite;
 * }
 * </pre>
 *
 * @see TestResult
 * @see TestSuite
 */
public abstract class TestCase extends Assert implements Test {
    /**
     * the name of the test case
     */
    private String fName;

    /**
     * No-arg constructor to enable serialization. This method
     * is not intended to be used by mere mortals without calling setName().
     */
    public TestCase() {
        fName = null;
    }

    /**
     * Constructs a test case with the given name.
     */
    public TestCase(String name) {
        fName = name;
    }

    /**
     * Counts the number of test cases executed by run(TestResult result).
     */
    public int countTestCases() {
        return 1;
    }

    /**
     * Creates a default TestResult object
     *
     * @see TestResult
     */
    protected TestResult createResult() {
        return new TestResult();
    }

    /**
     * A convenience method to run this test, collecting the results with a
     * default TestResult object.
     *
     * @see TestResult
     */
    public TestResult run() {
        TestResult result = createResult();
        run(result);
        return result;
    }

    /**
     * Runs the test case and collects the results in TestResult.
     */
    public void run(TestResult result) {
        result.run(this);
    }


    /**
     * Runs the bare test sequence.
     * @throws Throwable if any exception is thrown
     */
    public void runBare() throws Throwable {
        final boolean useThreadGroup =
            TestThreadGroup.isCheckThreadsEnabled() &&
            ((fName == null) || 
             !(fName.endsWith("_NOTHREAD"))); // use thread group unless method name ends _NOTHREAD
        final boolean checkJoin = useThreadGroup &&
            TestThreadGroup.isCheckJoinEnabled() &&
            ((fName == null) ||
             !(fName.endsWith("_NOJOIN"))); // check join unless method name ends with _NOJOIN
        final boolean checkLucky = checkJoin &&
            TestThreadGroup.isCheckLuckyEnabled() &&
            ((fName == null) ||
             !(fName.endsWith("_NOLUCKY"))); // check lucky unless method name ends with _NOLUCKY
        if (useThreadGroup) {
            final String prevSunHandler = System.getProperty("sun.awt.exception.handler");
            // TestThreadGroup.msg("       TestCase.runBare: prevSunHandler="+prevSunHandler);
            try {
                final TestThreadGroup tg = new TestThreadGroup();
                TestThreadGroup.setCurrentThreadGroup(tg);
                System.setProperty("sun.awt.exception.handler", TestThreadGroup.AwtHandler.class.getName());
                TestThreadGroup.updateAWTHandlerThreadGroup();
                // TestThreadGroup.msg("-----> TestCase.runBare: created and set tg="+TestThreadGroup.getCurrentThreadGroup());
                setUp();
                final Thread t = new Thread(tg, new Runnable() {
                    public void run() {
                        Throwable exception= null;
                        try {
                            runTest();
                        }
                        catch(Throwable running) {
                            exception = running;
                        }
                        finally {
                            try {
                                tearDown();
                            }
                            catch (Throwable tearingDown) {
                                if (exception == null) {
                                    exception = tearingDown;
                                }
                            }
                        }
                        if (exception!=null) {
//                            try {
//                                extendStackTrace(exception, Thread.currentThread());
//                            }
//                            catch(LuckyWarningsDisabledException e) { /* nothing to do */ }
                            throw new WrappedException(exception);
                        }
                        tg.notifyEvent(); 
                    }
                }, "Concutest-JUnit-"+((fName!=null)?fName:"test"));
                t.setDaemon(false); // set the test thread to always be a non-daemon
                t.start();
                try {
                    tg.waitForEvent();
                    try {
                        // and then join with the actual test thread
                        t.join();
                    }
                    catch(InterruptedException e) {
                        // there was an interrupted exception that we didn't expect
                        throw new MultithreadedTestError("After the test had finished, the join "+
                                                         "operation with the test thread was interrupted "+
                                                         "(probably a bug in Concutest-JUnit)", e);
                    }
                }
                catch(InterruptedException e) {
                    // this thread was interrupted, pass it on to the actual test thread
                    t.interrupt();
                    try {
                        // and then join with the actual test thread
                        t.join();
                    }
                    catch(InterruptedException e1) {
                        // there was another interrupted exception that we didn't expect
                        throw new MultithreadedTestError("After the test had been interrupted, the join "+
                                                         "operation with the test thread was interrupted "+
                                                         "(probably a bug in Concutest-JUnit)", e);
                    }
                }
                // if there was an exception, wait until the throwing thread
                // is dead (but not longer than 1000 ms)
                if (tg.getUncaughtThread()!=null) {
                    try {
                        tg.getUncaughtThread().join(1000);
                    }
                    catch(InterruptedException e) {
                        // there was another interrupted exception that we didn't expect
                        throw new MultithreadedTestError("After the test threw an exception and died, the join "+
                                                         "operation with the test thread was interrupted "+
                                                         "(probably a bug in Concutest-JUnit)", e);
                    }
                }
                
                // check if there was an uncaught exception and throw it if there was one
                TestThreadGroup.throwUncaughtException(tg);
                
                if (checkJoin) {
                    // check which threads are still running
                    final Thread[] alive = TestThreadGroup.checkThreadsAlive(tg, t);
                    if (alive.length>0) {
                        for (Thread at: alive) { System.err.println("Thread "+at.getName()+" ("+at.getClass().getName()+") is still alive"); }
                        StringBuilder sb = new StringBuilder();
                        sb.append(fName);
                        sb.append(": The test did not perform a join on all spawned threads.");
                        try {
                            String cc = TestThreadGroup.getThreadStartStackTrace(alive[0]);
                            throw new NoJoinError(sb.toString(),cc);
                        }
                        catch(TestThreadGroup.LuckyWarningsDisabledException e) {
                            throw new NoJoinError(sb.toString());
                        }
                        // System.err.println("WARNING: "+sb.toString());
                    }
                    else if (checkLucky) {
                        try {
                            // TestThreadGroup.dumpThreads("threadsStarted");
                            // TestThreadGroup.dumpThreads("threadsJoined");
                            HashSet<Thread> threadsStarted = TestThreadGroup.collectThreads(t,"threadsStarted");
                            HashSet<Thread> threadsJoined = TestThreadGroup.collectThreads(t,"threadsJoined");
                            // if a thread is ignored, we will treat it as another "root" of the join graph
                            // a thread has been properly joined if and only if it is reachable from the main thread or
                            // it is reachable from a thread that is ignored
                            for(Thread ct: threadsStarted) {
                                if (TestThreadGroup.shouldIgnoreThread(ct)) {
                                    HashSet<Thread> threadsJoinedByIgnored = TestThreadGroup.collectThreads(ct,"threadsJoined");
                                    threadsJoined.addAll(threadsJoinedByIgnored);
                                }
                            }
                            HashSet<Thread> threadsNotJoined = new HashSet<Thread>(threadsStarted);
                            threadsNotJoined.removeAll(threadsJoined);
                            HashSet<Thread> threadsInViolation = new HashSet<Thread>(threadsNotJoined);
                            for(Thread ct: threadsNotJoined) {
                                if (TestThreadGroup.shouldIgnoreThread(ct)) {
                                    threadsInViolation.remove(ct);
                                }
                            }
                            if (threadsInViolation.size()>0) {
                                StringBuilder sb = new StringBuilder(fName+": Some spawned threads ended before the test was over, but the test did not join with them:");
                                for(Thread ct: threadsInViolation) {
                                    sb.append('\n');
                                    sb.append(ct);
                                    sb.append(":\n");
                                    sb.append(TestThreadGroup.getThreadStartStackTrace(ct));
                                }
                                throw new LuckyError(sb.toString(), TestThreadGroup.getThreadStartStackTrace(threadsInViolation.iterator().next()));
                                /*
                                 System.err.println("WARNING: "+fName+": Some spawned threads ended before the test was over, but the test did not join with them:");
                                 for(Thread ct: threadsInViolation) {
                                 System.err.println(ct+":\n"+getThreadStartStackTrace(ct));
                                 }
                                 */
                            }
                        }
                        catch(TestThreadGroup.LuckyWarningsDisabledException e) { System.err.println("Disabled \"lucky\" warnings: "+e); }                        
                    }

                    // only check if this error hasn't been reported already
                    if (!TestThreadGroup.getEventThreadStillProcessingErrorOccurred()) {
                        // check if the event thread was alive
                        boolean eventThreadAlive = false;
                        Thread eventThreadOrNull = TestThreadGroup.getEventThreadOrNull();
                        // System.out.println("eventThreadOrNull: "+eventThreadOrNull);
                        if (eventThreadOrNull!=null) {
                            eventThreadAlive = eventThreadOrNull.isAlive();
                        }
                        // System.out.println("eventThreadAlive: "+eventThreadAlive);
                        if (eventThreadAlive) {
                            // The event thread is alive, make sure that there are no more Runnables in it
                            // that could fail.
                            // We know that all other threads have terminated:
                            // - test's main thread finished and returned
                            // - no child threads are running (would have caused a NoJoinError otherwise)
                            // There could be a Runnable in the event thread running right now.
                            // If we add our own "token" Runnable to it and wait for it to finish,
                            // then we know the currently running Runnable has finished.
                            // However, it could still have added more Runnables to the event queue.
                            // So we check in our own Runnable if the event queue is empty.
                            final Thread eventThread = eventThreadOrNull;
                            final StackTraceElement[] eventThreadStackTrace = eventThread.getStackTrace(); 
                            final EventQueue eq = Toolkit.getDefaultToolkit().getSystemEventQueue();
                            if (eq!=null) {
                                AWTEvent ev = eq.peekEvent();
                                if (ev!=null) {
                                    TestThreadGroup.setEventThreadStillProcessingErrorOccurred(true);
                                    throw new EventThreadStillProcessingError("More than one Runnable was still running in the event "+
                                                                              "thread after the test had already ended. (Note: This error "+
                                                                              "will not be reported anymore in the following tests.)",
                                                                              eventThreadStackTrace);
                                }
                                else {
                                    // event queue is empty
                                    // add our own Runnable
                                    Thread interrupterThread = null;
                                    try {
                                        // interrupt the invokeAndWait below after 100 ms
                                        final Object signal = new Object();
                                        final boolean[] tokenRunning = new boolean[] { false };
                                        final boolean[] timedOut = new boolean[] { false };
                                        
                                        final Thread mainThread = Thread.currentThread();
                                        interrupterThread = new Thread("Concutest-JUnit-EventThreadWaitInterrupter") {
                                            public void run() {
                                                // wait for at most 100 ms, then interrupt mainThread
                                                synchronized(signal) {
                                                    try { signal.wait(100); }
                                                    catch(InterruptedException e) {
                                                        MultithreadedTestError mte =
                                                            new MultithreadedTestError("After the test's main thread finished and the "+
                                                                                       "test framework waited for the event thread to "+
                                                                                       "finish processing, a wait operation was "+
                                                                                       "interrupted unexpectedly "+
                                                                                       "(probably a bug in Concutest-JUnit)", e);
                                                        tg.uncaughtException(Thread.currentThread(), mte);
                                                    }
                                                }
                                                synchronized(signal) {
                                                    if (!tokenRunning[0]) {
                                                        // if our token isn't running at this point,
                                                        // interrupt the main thread
                                                        timedOut[0] = true;
                                                        mainThread.interrupt();
                                                    }
                                                }
                                            }
                                        };
                                        interrupterThread.start();
                                        
                                        eq.invokeAndWait(new Runnable() {
                                            public void run() {
                                                // signal the interrupterThread it doesn't have to wait any longer 
                                                synchronized(signal) {
                                                    tokenRunning[0] = true;
                                                    signal.notify();
                                                }
                                                
                                                // check if event queue is empty
                                                AWTEvent ev = eq.peekEvent();
                                                if (ev!=null) {
                                                    // not empty! that means a Runnable executing after the thread
                                                    // finished added another Runnable to the event queue
                                                    synchronized(signal) {
                                                        if (!timedOut[0]) {
                                                            TestThreadGroup.setEventThreadStillProcessingErrorOccurred(true);
                                                            EventThreadStillProcessingError ete =
                                                                new EventThreadStillProcessingError("A Runnable was still running in the event "+
                                                                                                    "thread after the test had already ended, "+
                                                                                                    "and a new Runnable was even added to the "+
                                                                                                    "event queue. (Note: This error will not "+
                                                                                                    "be reported anymore in the following tests.)",
                                                                                                    eventThreadStackTrace);
                                                            tg.uncaughtException(Thread.currentThread(), ete);
                                                        }
                                                    }
                                                }
                                            }
                                        });
                                    }
                                    catch(InterruptedException ie) {
                                        TestThreadGroup.setEventThreadStillProcessingErrorOccurred(true);
                                        throw new EventThreadStillProcessingError("A Runnable was still running in the event "+
                                                                                  "thread after the test had already ended. "+
                                                                                  "(Note: This error will not be reported anymore "+
                                                                                  "in the following tests.)",
                                                                                  eventThreadStackTrace);
                                    }
                                    catch(InvocationTargetException ite) {
                                        throw new MultithreadedTestError("After the test's main thread finished and the "+
                                                                         "test framework waited for the event thread to "+
                                                                         "finish processing, the framework's own token "+
                                                                         "Runnable unexpectedly threw an exception "+
                                                                         "(probably a bug in Concutest-JUnit)", ite);
                                    }
                                    finally {
                                        try { interrupterThread.join(); }
                                        catch(InterruptedException ie) { /* ignore */ }
                                    }
                                    
                                    // check if there was an uncaught exception and throw it if there was one
                                    TestThreadGroup.throwUncaughtException(tg);
                                }
                            }
                        }
                    }
                }
            }
            finally {
                if (prevSunHandler!=null) {
                    System.setProperty("sun.awt.exception.handler", prevSunHandler);
                    // TestThreadGroup.msg("<----- RunInThreadGroup.runBare: reset prevSunHandler="+prevSunHandler);
                }
                else {
                    // can't be set back to null, so we install a dummy handler
                    System.setProperty("sun.awt.exception.handler", TestThreadGroup.DummyHandler.class.getName());
                    // TestThreadGroup.msg("<~~~~~ TestCase.runBare: reset prevSunHandler="+TestThreadGroup.DummyHandler.class.getName());
                }
            }
        }
        else {
            // do not force threads to join
            setUp();
            Throwable exception = null;
            try {
                runTest();
            }
            catch(Throwable running) {
                exception = running;
            }
            finally {
                try {
                    tearDown();
                } catch (Throwable tearingDown) {
                    if (exception==null) exception = tearingDown;
                }
            }
            if (exception!=null) {
                throw exception;
            }
        }
    }
    
    /**
     * Override to run the test and assert its state.
     *
     * @throws Throwable if any exception is thrown
     */
    protected void runTest() throws Throwable {
        assertNotNull("TestCase.fName cannot be null", fName); // Some VMs crash when calling getMethod(null,null);
        Method runMethod = null;
        try {
            // use getMethod to get all public inherited
            // methods. getDeclaredMethods returns all
            // methods of this class but excludes the
            // inherited ones.
            runMethod = getClass().getMethod(fName, (Class[]) null);
        } catch (NoSuchMethodException e) {
            fail("Method \"" + fName + "\" not found");
        }
        if (!Modifier.isPublic(runMethod.getModifiers())) {
            fail("Method \"" + fName + "\" should be public");
        }

        try {
            runMethod.invoke(this);
        } catch (InvocationTargetException e) {
            e.fillInStackTrace();
            throw e.getTargetException();
        } catch (IllegalAccessException e) {
            e.fillInStackTrace();
            throw e;
        }
    }

    /**
     * Asserts that a condition is true. If it isn't it throws
     * an AssertionFailedError with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertTrue(String message, boolean condition) {
        Assert.assertTrue(message, condition);
    }

    /**
     * Asserts that a condition is true. If it isn't it throws
     * an AssertionFailedError.
     */
    @SuppressWarnings("deprecation")
    public static void assertTrue(boolean condition) {
        Assert.assertTrue(condition);
    }

    /**
     * Asserts that a condition is false. If it isn't it throws
     * an AssertionFailedError with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertFalse(String message, boolean condition) {
        Assert.assertFalse(message, condition);
    }

    /**
     * Asserts that a condition is false. If it isn't it throws
     * an AssertionFailedError.
     */
    @SuppressWarnings("deprecation")
    public static void assertFalse(boolean condition) {
        Assert.assertFalse(condition);
    }

    /**
     * Fails a test with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void fail(String message) {
        Assert.fail(message);
    }

    /**
     * Fails a test with no message.
     */
    @SuppressWarnings("deprecation")
    public static void fail() {
        Assert.fail();
    }

    /**
     * Asserts that two objects are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, Object expected, Object actual) {
        Assert.assertEquals(message, expected, actual);
    }

    /**
     * Asserts that two objects are equal. If they are not
     * an AssertionFailedError is thrown.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(Object expected, Object actual) {
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that two Strings are equal.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, String expected, String actual) {
        Assert.assertEquals(message, expected, actual);
    }

    /**
     * Asserts that two Strings are equal.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String expected, String actual) {
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that two doubles are equal concerning a delta.  If they are not
     * an AssertionFailedError is thrown with the given message.  If the expected
     * value is infinity then the delta value is ignored.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, double expected, double actual, double delta) {
        Assert.assertEquals(message, expected, actual, delta);
    }

    /**
     * Asserts that two doubles are equal concerning a delta. If the expected
     * value is infinity then the delta value is ignored.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(double expected, double actual, double delta) {
        Assert.assertEquals(expected, actual, delta);
    }

    /**
     * Asserts that two floats are equal concerning a positive delta. If they
     * are not an AssertionFailedError is thrown with the given message. If the
     * expected value is infinity then the delta value is ignored.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, float expected, float actual, float delta) {
        Assert.assertEquals(message, expected, actual, delta);
    }

    /**
     * Asserts that two floats are equal concerning a delta. If the expected
     * value is infinity then the delta value is ignored.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(float expected, float actual, float delta) {
        Assert.assertEquals(expected, actual, delta);
    }

    /**
     * Asserts that two longs are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, long expected, long actual) {
        Assert.assertEquals(message, expected, actual);
    }

    /**
     * Asserts that two longs are equal.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(long expected, long actual) {
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that two booleans are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, boolean expected, boolean actual) {
        Assert.assertEquals(message, expected, actual);
    }

    /**
     * Asserts that two booleans are equal.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(boolean expected, boolean actual) {
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that two bytes are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, byte expected, byte actual) {
        Assert.assertEquals(message, expected, actual);
    }

    /**
     * Asserts that two bytes are equal.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(byte expected, byte actual) {
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that two chars are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, char expected, char actual) {
        Assert.assertEquals(message, expected, actual);
    }

    /**
     * Asserts that two chars are equal.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(char expected, char actual) {
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that two shorts are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, short expected, short actual) {
        Assert.assertEquals(message, expected, actual);
    }

    /**
     * Asserts that two shorts are equal.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(short expected, short actual) {
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that two ints are equal. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(String message, int expected, int actual) {
        Assert.assertEquals(message, expected, actual);
    }

    /**
     * Asserts that two ints are equal.
     */
    @SuppressWarnings("deprecation")
    public static void assertEquals(int expected, int actual) {
        Assert.assertEquals(expected, actual);
    }

    /**
     * Asserts that an object isn't null.
     */
    @SuppressWarnings("deprecation")
    public static void assertNotNull(Object object) {
        Assert.assertNotNull(object);
    }

    /**
     * Asserts that an object isn't null. If it is
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertNotNull(String message, Object object) {
        Assert.assertNotNull(message, object);
    }

    /**
     * Asserts that an object is null. If it isn't an {@link AssertionError} is
     * thrown.
     * Message contains: Expected: <null> but was: object
     *
     * @param object Object to check or <code>null</code>
     */
    @SuppressWarnings("deprecation")
    public static void assertNull(Object object) {
        Assert.assertNull(object);
    }

    /**
     * Asserts that an object is null.  If it is not
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertNull(String message, Object object) {
        Assert.assertNull(message, object);
    }

    /**
     * Asserts that two objects refer to the same object. If they are not
     * an AssertionFailedError is thrown with the given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertSame(String message, Object expected, Object actual) {
        Assert.assertSame(message, expected, actual);
    }

    /**
     * Asserts that two objects refer to the same object. If they are not
     * the same an AssertionFailedError is thrown.
     */
    @SuppressWarnings("deprecation")
    public static void assertSame(Object expected, Object actual) {
        Assert.assertSame(expected, actual);
    }

    /**
     * Asserts that two objects do not refer to the same object. If they do
     * refer to the same object an AssertionFailedError is thrown with the
     * given message.
     */
    @SuppressWarnings("deprecation")
    public static void assertNotSame(String message, Object expected, Object actual) {
        Assert.assertNotSame(message, expected, actual);
    }

    /**
     * Asserts that two objects do not refer to the same object. If they do
     * refer to the same object an AssertionFailedError is thrown.
     */
    @SuppressWarnings("deprecation")
    public static void assertNotSame(Object expected, Object actual) {
        Assert.assertNotSame(expected, actual);
    }

    @SuppressWarnings("deprecation")
    public static void failSame(String message) {
        Assert.failSame(message);
    }

    @SuppressWarnings("deprecation")
    public static void failNotSame(String message, Object expected, Object actual) {
        Assert.failNotSame(message, expected, actual);
    }

    @SuppressWarnings("deprecation")
    public static void failNotEquals(String message, Object expected, Object actual) {
        Assert.failNotEquals(message, expected, actual);
    }

    @SuppressWarnings("deprecation")
    public static String format(String message, Object expected, Object actual) {
        return Assert.format(message, expected, actual);
    }

    /**
     * Sets up the fixture, for example, open a network connection.
     * This method is called before a test is executed.
     */
    protected void setUp() throws Exception {
    }

    /**
     * Tears down the fixture, for example, close a network connection.
     * This method is called after a test is executed.
     */
    protected void tearDown() throws Exception {
    }

    /**
     * Returns a string representation of the test case
     */
    @Override
    public String toString() {
        return getName() + "(" + getClass().getName() + ")";
    }

    /**
     * Gets the name of a TestCase
     *
     * @return the name of the TestCase
     */
    public String getName() {
        return fName;
    }

    /**
     * Sets the name of a TestCase
     *
     * @param name the name to set
     */
    public void setName(String name) {
        fName = name;
    }

    
    /**
     * Exception class to get a checked exception out of a Runnable.
     */
    public static class WrappedException extends RuntimeException {
        /**
         * Constructs a new runtime exception with the specified cause and a detail message of <tt>(cause==null ? null :
         * cause.toString())</tt> (which typically contains the class and detail message of <tt>cause</tt>).  This constructor
         * is useful for runtime exceptions that are little more than wrappers for other throwables.
         *
         * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A <tt>null</tt>
         *              value is permitted, and indicates that the cause is nonexistent or unknown.)
         *
         * @since 1.4
         */
        public WrappedException(Throwable cause) {
            super(cause);
        }
    }
}
