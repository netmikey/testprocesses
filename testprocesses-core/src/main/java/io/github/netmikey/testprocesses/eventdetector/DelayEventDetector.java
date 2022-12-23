package io.github.netmikey.testprocesses.eventdetector;

import java.util.concurrent.TimeoutException;

import io.github.netmikey.testprocesses.RunningTestProcess;

/**
 * <b>You are highly encouraged to use any of the other, more deterministic
 * {@link EventDetector}s whenever possible.</b>
 * <p>
 * This {@link EventDetector} is only intended for cases where no other event
 * exists that could be detected but execution has to be delayed nevertheless.
 * Timing-based test behavior varies between test execution environments and so
 * it leads to unstable tests. You should avoid relying on timing whenever
 * possible.
 * <p>
 * Waits for the specified amount of time and then triggers event detection.
 */
public class DelayEventDetector implements EventDetector {

    private long delayMillis;

    /**
     * Create a new {@link DelayEventDetector} with the specified delay.
     * 
     * @param delayInMilliseconds
     *            The delay to be used.
     * @return The new {@link DelayEventDetector}.
     */
    public static DelayEventDetector withDelayMillis(long delayInMilliseconds) {
        DelayEventDetector result = new DelayEventDetector();
        result.delayMillis = delayInMilliseconds;
        return result;
    }

    @Override
    public void waitForEvent(RunningTestProcess<?> process) throws TimeoutException {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for the delay of " + delayMillis + " ms to pass");
        }
    }

}
