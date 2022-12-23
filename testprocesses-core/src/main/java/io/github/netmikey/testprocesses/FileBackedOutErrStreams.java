package io.github.netmikey.testprocesses;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Interface to be implemented by {@link TestProcessDefinition} implementations
 * who redirect the test process' output- and error-streams to files. Some
 * TestProcesses functionality requires access to those files.
 */
public interface FileBackedOutErrStreams {
    /**
     * Get the outFile.
     * 
     * @return Returns the outFile. May be {@link Optional#empty()} if the
     *         process hasn't run yet.
     */
    public Optional<Path> getOutFile();

    /**
     * Get the errFile.
     * 
     * @return Returns the errFile. May be {@link Optional#empty()} if the
     *         process hasn't run yet.
     */
    public Optional<Path> getErrFile();
}
