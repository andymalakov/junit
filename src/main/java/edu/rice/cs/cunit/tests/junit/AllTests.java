package edu.rice.cs.cunit.tests.junit;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * TestSuite that runs all the Concutest-JUnit tests.
 * @author Mathias Ricken
 */
public class AllTests {
    public static void main(String[] args) {
        junit.textui.TestRunner.run (suite());
    }
    public static Test suite() {
        TestSuite suite= new TestSuite("All Concutest-JUnit Tests");
        suite.addTest(MultithreadedTestCaseTest.suite());
        suite.addTest(NoJoinTestCaseTest.suite());
        suite.addTest(LuckyTestCaseTest.suite());
        suite.addTest(EventThreadTestCaseTest.suite());
        suite.addTest(EventThreadTestCaseAnnotTest.suite());
        suite.addTest(TestUtilsWaitTest.suite());
        suite.addTest(TestUtilsWaitAnnotTest.suite());
        return suite;
    }
}
