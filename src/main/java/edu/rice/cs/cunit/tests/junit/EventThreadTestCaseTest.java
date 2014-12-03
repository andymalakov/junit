package edu.rice.cs.cunit.tests.junit;

import junit.framework.*;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;

/**
 * Test of test cases that happen in the event thread.
 * @author Mathias Ricken
 */
public class EventThreadTestCaseTest extends TestCase {
    public static void main (String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    public static Test suite ( ) {
        TestSuite suite= new TestSuite("Concutest-JUnit Tests");
        suite.addTestSuite(EventThreadTestCaseTest.class);
        return suite;
    }

    protected void setUp() {
        // clear the _eventThreadStillProcessingErrorOccurred flag to get reports for every test
        edu.rice.cs.cunit.concJUnit.TestThreadGroup.setEventThreadStillProcessingErrorOccurred(false);
    }
    protected void tearDown() {
        // clear the _eventThreadStillProcessingErrorOccurred flag to get reports for every test
        edu.rice.cs.cunit.concJUnit.TestThreadGroup.setEventThreadStillProcessingErrorOccurred(false);
    }
    
    // ===============================================
    // Test of a test case that should pass.
    // ===============================================
    
    public static class TestCase0 extends TestCase {
        public TestCase0(String name) {
            super(name);
        }
        public void testShouldPass() { }
    }
    
    /**
     * Test of a test case which should pass.
     */
    public void testMainThread() {
        // System.out.println("testMainThread");
        TestCase tc = new TestCase0("testShouldPass");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_failureT != null) {
                    if (_errorT != null) {
                        fail("Test case reported an error and a failure.");
                    }
                    else {
                        fail("Test case reported a failure.");
                    }
                }
                else {
                    if (_errorT != null) {
                        fail("Test case reported an error.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
        edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
    }
    
    // ===============================================
    // Test of a test case that should pass.
    // ===============================================
    
    public static class TestCase1 extends TestCase {
        public TestCase1(String name) {
            super(name);
        }
        public void testShouldPass() throws InterruptedException {
            // we run something in the event thread, but wait longer
            final CountDownLatch signal = new CountDownLatch(1);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    signal.countDown();
                }
            });
            signal.await();
        }
    }
    
    /**
     * Test of a test case which should pass.
     */
    public void testEventThread() {
        // System.out.println("testEventThread");
        TestCase tc = new TestCase1("testShouldPass");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_failureT != null) {
                    if (_errorT != null) {
                        fail("Test case reported an error and a failure.");
                    }
                    else {
                        fail("Test case reported a failure.");
                    }
                }
                else {
                    if (_errorT != null) {
                        fail("Test case reported an error.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
        edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
    }
    
    // ===============================================
    // Test of a test case with a late failure in the
    // event thread.
    // ===============================================
    
    public static class TestCase2 extends TestCase {
        public TestCase2(String name) {
            super(name);
        }
        public void testShouldFail() {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                    throw new RuntimeException("booh!");
                }
            });
            // do not join or wait or sleep...
        }
    }
    
    /**
     * Test of a test case with a late failure in the event thread, which should fail.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase2 fails and leaves threads alive. This test asserts that TestCase2 fails,
     *       so it should succeed if TestCase2 fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testEventThreadLate_NOJOIN() {
        // System.out.println("testEventThreadLate_NOJOIN");
        TestCase tc = new TestCase2("testShouldFail");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_errorT == null) {
                    if (_failureT != null) {
                        fail("Test case reported a failure instead of an error.");
                    }
                    else {
                        fail("Test case did not report an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case also reported a failure.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
        edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
    }
    
    // ===============================================
    // Test of a test case in which the event thread
    // is still processing late, but does not fail.
    // ===============================================
    
    public static class TestCase3 extends TestCase {
        public TestCase3(String name) {
            super(name);
        }
        public void testShouldFail() {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(5000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                }
            });
            // do not join or wait or sleep...
        }
    }
    
    /**
     * Test of a test case with a late failure in the event thread, which should fail.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase3 fails and leaves threads alive. This test asserts that TestCase3 fails,
     *       so it should succeed if TestCase3 fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testEventThreadJustLateNoFail_NOJOIN() {
        // System.out.println("testEventThreadJustLateNoFail_NOJOIN");
        TestCase tc = new TestCase3("testShouldFail");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_errorT == null) {
                    if (_failureT != null) {
                        fail("Test case reported a failure instead of an error.");
                    }
                    else {
                        fail("Test case did not report an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case also reported a failure.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
        edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
    }
    
    /**
     * Test of a test case with a late failure in the event thread, which should fail,
     * but only once.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase3 fails and leaves threads alive. This test asserts that TestCase3 fails,
     *       so it should succeed if TestCase3 fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testEventThreadJustLateNoFailOnlyOnce_NOJOIN() {
        // System.out.println("testEventThreadJustLateNoFail_NOJOIN");
        TestCase tc = new TestCase3("testShouldFail");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_errorT == null) {
                    if (_failureT != null) {
                        fail("Test case reported a failure instead of an error.");
                    }
                    else {
                        fail("Test case did not report an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case also reported a failure.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
        TestResult tr2 = new TestResult();
        tr2.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_errorT != null) {
                    if (_failureT != null) {
                        fail("Test case reported a failure and an error.");
                    }
                    else {
                        fail("Test case reported an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case reported a failure.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr2);
        if (tr2.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
        edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
    }
    
    // ===============================================
    // Test of a test case in which the event thread
    // is processing late, and where new Runnables
    // get added after the token Runnable.
    // ===============================================
    
    public static class TestCase4 extends TestCase {
        public TestCase4(String name) {
            super(name);
        }
        public void testShouldFail() throws InterruptedException {
            final CountDownLatch signal = new CountDownLatch(1);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    signal.countDown();
                    SwingUtilities.invokeLater(new RunnableStarterRunnable(10));
                }
            });
            signal.await();
        }
        
        public static class RunnableStarterRunnable implements Runnable {
            private final int _num;
            public RunnableStarterRunnable(int num) {
                _num = num;
            }
            public void run() {
                try {
                    Thread.sleep(50);
                }
                catch(InterruptedException e) {
                    /* ignore */
                }
                if (_num>0) {
                    SwingUtilities.invokeLater(new RunnableStarterRunnable(_num-1));
                }
            }
        }
    }
    
    /**
     * Test of a test case with a late failure in the event thread, which should fail.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase4 fails and leaves threads alive. This test asserts that TestCase4 fails,
     *       so it should succeed if TestCase4 fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testEventThreadLateNoFailRunnablesAfterToken_NOJOIN() {
        // System.out.println("testEventThreadLateNoFailRunnablesAfterToken_NOJOIN");
        TestCase tc = new TestCase4("testShouldFail");
        TestResult tr = new TestResult();
        tr.addListener(new TestListener() {
            private Throwable _errorT = null;
            private Throwable _failureT = null;
            public void addError(Test test, java.lang.Throwable t) {
                _errorT = t;
            }
            public void addFailure(Test test, AssertionFailedError t) {
                _failureT = t;
            }
            public void endTest(Test test) {
                if (_errorT == null) {
                    if (_failureT != null) {
                        fail("Test case reported a failure instead of an error.");
                    }
                    else {
                        fail("Test case did not report an error.");
                    }
                }
                else {
                    if (_failureT != null) {
                        fail("Test case also reported a failure.");
                    }
                }
            }
            public void startTest(Test test) {
            }
        });
        tc.run(tr);
        if (tr.runCount()!=1) {
            fail("Test case was not executed correctly.");
        }
        edu.rice.cs.cunit.concJUnit.TestUtils.waitForAll();
    }
}
