package io.github.netmikey.testprocesses;

import io.github.netmikey.testprocesses.eventdetector.EventDetector;
import io.github.netmikey.testprocesses.processdestroyer.ProcessDestroyer;

/**
 * A {@link TestProcessDefinition} type used as a marker for not-set
 * {@link TestProcess#beanClass()} annotation field.
 */
public class VoidTestProcessDefinition implements TestProcessDefinition {

    @Override
    public void start() {
        throw new IllegalStateException("This class should never be used.");
    }

    @Override
    public void stop() {
        throw new IllegalStateException("This class should never be used.");
    }

    @Override
    public String getProcessIdentifier() {
        throw new IllegalStateException("This class should never be used.");
    }

    @Override
    public TestProcessState getRequestedState() {
        throw new IllegalStateException("This class should never be used.");
    }

    @Override
    public TestProcessState getActualState() {
        throw new IllegalStateException("This class should never be used.");
    }

    @Override
    public EventDetector getStartupDetector() {
        throw new IllegalStateException("This class should never be used.");
    }

    @Override
    public EventDetector getShutdownDetector() {
        throw new IllegalStateException("This class should never be used.");
    }

    @Override
    public ProcessDestroyer getProcessDestroyer() {
        throw new IllegalStateException("This class should never be used.");
    }
}
