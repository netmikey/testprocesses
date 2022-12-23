package io.github.netmikey.testprocesses;

import io.github.netmikey.testprocesses.eventdetector.EventDetector;
import io.github.netmikey.testprocesses.processdestroyer.ProcessDestroyer;

/**
 * A {@link TestProcessDefinition} implementation defines how to handle a test
 * process. Among some other required control methods, it defines methods for
 * starting and stopping the process.
 */
public interface TestProcessDefinition {

    /**
     * Start the managed process. This method should only start the process and
     * return. It must not wait for it to be fully operational as this will be
     * done by the {@link TestProcessesRegistry} using
     * {@link #getStartupDetector()}.
     */
    public void start();

    /**
     * This will be invoked by the {@link TestProcessesRegistry} before the
     * process is actively being destroyed. It might not be necessary to provide
     * an implementation this method as the actual process destruction and
     * shutdown detection will also be initiated by the
     * {@link TestProcessesRegistry} using {@link #getProcessDestroyer()} and
     * {@link #getShutdownDetector()}.
     */
    default public void stop() {
        // Noop.
    };

    /**
     * Return the unique identifier of this test process. The
     * {@link TestProcessesRegistry} makes sure there's at most one process with
     * this identifier running at any time.
     * 
     * @return The process identifier.
     */
    public String getProcessIdentifier();

    /**
     * Return the currently requested state of the managed process.
     * 
     * @return The requested state.
     */
    public TestProcessState getRequestedState();

    /**
     * Return the current actual state of the managed process.
     * 
     * @return The actual state.
     */
    public TestProcessState getActualState();

    /**
     * Get the {@link EventDetector} to be used to detect when this process has
     * finished starting.
     * 
     * @return The {@link EventDetector}.
     */
    public EventDetector getStartupDetector();

    /**
     * Get the {@link EventDetector} to be used to detect when this process has
     * finished shutting down.
     * 
     * @return The {@link EventDetector}.
     */
    public EventDetector getShutdownDetector();

    /**
     * Get the {@link ProcessDestroyer} to be used to destroy this process.
     * 
     * @return The {@link ProcessDestroyer}.
     */
    public ProcessDestroyer getProcessDestroyer();
}
