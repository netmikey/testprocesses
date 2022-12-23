package io.github.netmikey.testprocesses.eventdetector;

import java.util.concurrent.TimeoutException;

import io.github.netmikey.testprocesses.RunningTestProcess;

/**
 * Waits until a certain event occurs. Usually to detect when a process has
 * finished starting up or shutting down.
 */
public interface EventDetector {
    /**
     * Blocks until the process has finished starting up.
     * 
     * @param process
     *            The {@link RunningTestProcess} this {@link EventDetector} runs
     *            on.
     * @throws TimeoutException
     *             If waiting times out.
     */
    public void waitForEvent(RunningTestProcess<?> process) throws TimeoutException;
}
