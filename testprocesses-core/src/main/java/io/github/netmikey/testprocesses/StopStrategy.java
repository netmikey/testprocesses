package io.github.netmikey.testprocesses;

/**
 * Defines when TestProcesses should stop a process.
 */
public enum StopStrategy {
    /**
     * Leaves a test process running after a test. It will only be stopped when
     * another test requests a restart or until the test Spring Context is being
     * shut down (at the end of all tests).
     */
    LEAVE_RUNNING,

    /**
     * Stops a test process immediately after a test. If other tests need the
     * process, TestProcesses will have to re-start it.
     */
    STOP_AFTER_TEST;
}
