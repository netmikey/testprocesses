package io.github.netmikey.testprocesses.eventdetector;

import java.util.concurrent.TimeoutException;

import io.github.netmikey.testprocesses.RunningTestProcess;

/**
 * An EventDetector that doesn't actually wait for anything but returns
 * immediately.
 */
public class ImmediateEventDetector implements EventDetector {

    /**
     * A singleton instance of this (stateless) event detector.
     */
    public static final ImmediateEventDetector INSTANCE = new ImmediateEventDetector();

    @Override
    public void waitForEvent(RunningTestProcess<?> process) throws TimeoutException {
        return;
    }
}
