package io.github.netmikey.testprocesses.eventdetector;

import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import io.github.netmikey.testprocesses.RunningTestProcess;
import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.TestProcessState;

/**
 * {@link EventDetector} base class that adds timeout semantics.
 * 
 * @param <T>
 *            The concrete type of the subclass.
 */
public abstract class AbstractEventDetector<T extends AbstractEventDetector<T>> implements EventDetector {

    private long timeoutMillis = 30000;

    private long intervalMillis = 500;

    /**
     * Specify the maximum time in milliseconds to wait for the event to occur.
     * 
     * @param timeout
     *            The specified timeout in milliseconds.
     * @return This {@link EventDetector}.
     */
    @SuppressWarnings("unchecked")
    public T withTimeoutMillis(long timeout) {
        this.timeoutMillis = timeout;
        return (T) this;
    }

    /**
     * Specify the time in milliseconds to wait between checks when polling.
     * 
     * @param interval
     *            The specified interval in milliseconds.
     * @return This {@link EventDetector}.
     */
    @SuppressWarnings("unchecked")
    public T withIntervalMillis(long interval) {
        this.intervalMillis = interval;
        return (T) this;
    }

    /**
     * First checks whether the test process is still running and throws an
     * Exception if it isn't. Then, given the provided start timestamp, checks
     * if the configured timeout has been reached. If so, throws a
     * TimeoutException with the message provided by the specified supplier. If
     * not, sleeps for the configured {@link #intervalMillis} before the next
     * execution.
     * 
     * @param runningTestProcess
     *            The reference to the current {@link RunningTestProcess}.
     * @param startMillis
     *            The timestamp milliseconds when the operation started (will be
     *            compared to {@link System#currentTimeMillis()}.
     * @param operationDescription
     *            A provider function that returns a description of what the
     *            {@link EventDetector} is actually doing. It will be used for
     *            timeout and interrupted exception messages.
     * @throws TimeoutException
     *             If the timeout has been reached.
     */
    protected void checkRunningTimeoutAndSleep(RunningTestProcess<?> runningTestProcess, long startMillis,
        Supplier<String> operationDescription) throws TimeoutException {

        TestProcessDefinition definition = runningTestProcess.getDefinition();
        if (!TestProcessState.STARTED.equals(definition.getActualState())) {
            throw new IllegalStateException("Process " + definition.getProcessIdentifier()
                + " has unexpectedly stopped running while waiting for an event.");
        }

        if ((System.currentTimeMillis() - startMillis) > timeoutMillis) {
            throw new TimeoutException("Timeout after " + timeoutMillis + " ms while " + operationDescription.get());
        }
        try {
            Thread.sleep(intervalMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while " + operationDescription.get());
        }
    }

    /**
     * Get the timeoutMillis.
     * 
     * @return Returns the timeoutMillis.
     */
    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Set the timeoutMillis.
     * 
     * @param timeoutMillis
     *            The timeoutMillis to set.
     */
    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

}
