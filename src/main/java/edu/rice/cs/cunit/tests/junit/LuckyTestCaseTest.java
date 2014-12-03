package edu.rice.cs.cunit.tests.junit;

import junit.framework.*;

/**
 * Test of test cases that do not join with all threads even though they end ("lucky").
 * @author Mathias Ricken
 */
public class LuckyTestCaseTest extends TestCase {
    public static void main (String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    public static Test suite ( ) {
        TestSuite suite= new TestSuite("Concutest-JUnit Tests");
        suite.addTestSuite(LuckyTestCaseTest.class);
        return suite;
    }

    // ============================================
    // Test of a test case with a "_NOLUCKY" method.
    // ============================================

    public static class TestCase1 extends TestCase {
        public Thread auxThread = null;
        public TestCase1(String name) {
            super(name);
        }
        public void testShouldPass_NOLUCKY() {
            auxThread = new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(1000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                }
            }, "Testcase1");
            auxThread.start();
            try {
                Thread.sleep(5000);
            }
            catch(InterruptedException e) {
                /* ignore */
            }
            // do not join with auxThread
            // but auxThread will finish first ("lucky")
        }
    }

    /**
     * Test of a test case with a "_NOLUCKY" method.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase1 passes but leaves threads alive. This test asserts that TestCase1 passes,
     *       so it should succeed if TestCase1 passes, even though it leaves threads alive.
     *       But that means we need to ignore the threads that are still alive.
     */
    public void testNoJoin_NOJOIN() {
        TestCase1 tc = new TestCase1("testShouldPass_NOLUCKY");
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
        // join with spawned auxiliary thread
        try { tc.auxThread.join(); }
        catch(InterruptedException e) { /* ignore */ }
    }


    // ===============================================
    // Test of a test case without a "_NOLUCKY" method.
    // ===============================================

    public static class TestCase2 extends TestCase {
        public TestCase2(String name) {
            super(name);
        }
        public void testShouldFail() {
            Thread auxThread= new Thread(new Runnable() {
                public void run() {
                    try {
                        Thread.sleep(1000);
                    }
                    catch(InterruptedException e) {
                        /* ignore */
                    }
                }
            }, "Testcase1");
            auxThread.start();
            try {
                Thread.sleep(5000);
            }
            catch(InterruptedException e) {
                /* ignore */
            }
            // do not join with auxThread
            // but auxThread will finish first ("lucky")
        }
    }

    /**
     * Test of a test case without a "_NOLUCKY", which should fail.
     * Note: We don't want the join tests enabled here, because this us a unit test for testing.
     *       TestCase2 fails and leaves threads alive. This test asserts that TestCase2 fails,
     *       so it should succeed if TestCase2 fails. But that means we need to ignore the threads
     *       that are still alive.
     */
    public void testWithoutNoJoin_NOJOIN() {
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
    }
}
