package io.github.netmikey.testprocesses.eventdetector;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.github.netmikey.testprocesses.RunningTestProcess;
import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.utils.ProcessUtils;

/**
 * An {@link EventDetector} that waits for the {@link TestProcessDefinition}'s
 * <code>managedProcess</code> and all its recursive child processes to finish.
 * <p>
 * This is handy e.g. when you start a Java process through a shell script and
 * thus the shell (and not the JVM) is the process controlled by TestProcesses.
 * In this scenario, using this {@link EventDetector} to detect process shutdown
 * will return only when the JVM (that is a grand-child of the test using
 * TestProcesses) exits.
 */
public class RecursiveProcessTerminationEventDetector
    extends AbstractEventDetector<RecursiveProcessTerminationEventDetector> implements EventDetector {

    /**
     * Create a new instance.
     * 
     * @return A new instance.
     */
    public static RecursiveProcessTerminationEventDetector newInstance() {
        return new RecursiveProcessTerminationEventDetector();
    }

    @Override
    public void waitForEvent(RunningTestProcess<?> process) throws TimeoutException {
        long startMillis = System.currentTimeMillis();
        TestProcessDefinition processDefinition = process.getDefinition();
        Process managedProcess = ProcessUtils.retrieveManagedProcess(processDefinition);

        try {
            if (!managedProcess.waitFor(getTimeoutMillis(), TimeUnit.MILLISECONDS)) {
                throw new TimeoutException("Test process " + processDefinition.getProcessIdentifier()
                    + " did not exit within " + getTimeoutMillis() + " ms.");
            }
        } catch (InterruptedException e) {
            // Nothing to do.
        }

        try {
            waitForChildrenRecursive(processDefinition, startMillis, managedProcess.toHandle());
        } catch (RuntimeException e) {
            if (e.getCause() instanceof TimeoutException) {
                throw (TimeoutException) e.getCause();
            } else {
                throw e;
            }
        }
    }

    private void waitForChildrenRecursive(TestProcessDefinition processDefinition, long startMillis,
        ProcessHandle process) {

        process.children().forEach(child -> waitForChildrenRecursive(processDefinition, startMillis, child));
        try {
            long remainingMillis = getTimeoutMillis() - (System.currentTimeMillis() - startMillis);
            process.onExit().get(remainingMillis, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException("Error occured while waiting for child " + process.toString()
                + " of the process " + processDefinition.getProcessIdentifier() + " to finish: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for child " + process.toString()
                + " of the process " + processDefinition.getProcessIdentifier() + " to finish: " + e.getMessage(), e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
