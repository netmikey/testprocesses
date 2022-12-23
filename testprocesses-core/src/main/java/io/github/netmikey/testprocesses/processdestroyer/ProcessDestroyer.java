package io.github.netmikey.testprocesses.processdestroyer;

import io.github.netmikey.testprocesses.RunningTestProcess;
import io.github.netmikey.testprocesses.TestProcessDefinition;

/**
 * Interface for a class that destroys / stops a given {@link Process}.
 */
public interface ProcessDestroyer {
    /**
     * Destroy / stop the {@link Process} controlled by the specified
     * {@link TestProcessDefinition}.
     * 
     * @param runningTestProcess
     *            The {@link RunningTestProcess} holding the process to be
     *            stopped / destroyed.
     */
    public void destroy(RunningTestProcess<?> runningTestProcess);
}
