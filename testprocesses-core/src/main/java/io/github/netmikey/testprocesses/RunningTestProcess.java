package io.github.netmikey.testprocesses;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps a {@link TestProcessDefinition} that is currently running, along with
 * (potentially later) some metadata about it.
 * 
 * @param <T>
 *            The concrete {@link TestProcessDefinition} subtype.
 */
public class RunningTestProcess<T extends TestProcessDefinition> {

    private static final Logger LOG = LoggerFactory.getLogger(RunningTestProcess.class);

    private T definition;

    private Optional<Long> currentTestStdOutStart = Optional.empty();

    private Optional<Long> currentTestStdErrStart = Optional.empty();

    /**
     * Initializing constructor.
     * 
     * @param definition
     *            The process definition bean instance.
     */
    public RunningTestProcess(T definition) {
        super();
        this.definition = definition;
    }

    /**
     * Notifies that a test method is about to start.
     */
    public void onTestStart() {
        if (definition instanceof FileBackedOutErrStreams) {
            AbstractTestProcessDefinition processDefinition = (AbstractTestProcessDefinition) definition;
            currentTestStdOutStart = processDefinition.getOutFile()
                .flatMap(outFile -> currentPosition("stdOut stream", outFile));
            currentTestStdErrStart = processDefinition.getErrFile()
                .flatMap(errFile -> currentPosition("stdErr stream", errFile));
        }
    }

    /**
     * Notifies that a test method has ended.
     */
    public void onTestEnd() {
        currentTestStdOutStart = Optional.empty();
        currentTestStdErrStart = Optional.empty();
    }

    private Optional<Long> currentPosition(String description, Path file) {
        try (SeekableByteChannel outChannel = Files.newByteChannel(file, StandardOpenOption.READ)) {
            return Optional.of(outChannel.size());
        } catch (IOException e) {
            LOG.warn("Error while getting current position of " + description + " of test process "
                + definition.getProcessIdentifier() + ": " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Get the definition.
     * 
     * @return Returns the definition.
     */
    public T getDefinition() {
        return definition;
    }

    /**
     * Set the definition.
     * 
     * @param definition
     *            The definition to set.
     */
    public void setDefinition(T definition) {
        this.definition = definition;
    }

    /**
     * Get the currentTestStdOutStart.
     * 
     * @return Returns the currentTestStdOutStart.
     */
    public Optional<Long> getCurrentTestStdOutStart() {
        return currentTestStdOutStart;
    }

    /**
     * Get the currentTestStdErrStart.
     * 
     * @return Returns the currentTestStdErrStart.
     */
    public Optional<Long> getCurrentTestStdErrStart() {
        return currentTestStdErrStart;
    }

}
