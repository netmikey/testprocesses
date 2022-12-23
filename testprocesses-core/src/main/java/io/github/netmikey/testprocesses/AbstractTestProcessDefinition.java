package io.github.netmikey.testprocesses;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.netmikey.testprocesses.eventdetector.DelayEventDetector;
import io.github.netmikey.testprocesses.eventdetector.EventDetector;
import io.github.netmikey.testprocesses.eventdetector.RecursiveProcessTerminationEventDetector;
import io.github.netmikey.testprocesses.processdestroyer.DefaultProcessDestroyer;
import io.github.netmikey.testprocesses.processdestroyer.ProcessDestroyer;

/**
 * A simple {@link TestProcessDefinition} implementation that exposes a
 * {@link ProcessBuilder} to its subclasses for customizing the process being
 * built.
 * <p>
 * The process' error and output streams are redirected to temporary files that
 * are marked with {@link File#deleteOnExit()}. These are accessible via getters
 * and should be copied if their content is required after the test JVM has
 * exited.
 * <p>
 * This class uses the implementation class' fully qualified name as process
 * identifier by default.
 */
public abstract class AbstractTestProcessDefinition implements TestProcessDefinition, FileBackedOutErrStreams {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTestProcessDefinition.class);

    /**
     * The process' requested state.
     */
    protected TestProcessState requestedState = TestProcessState.STOPPED;

    private String processIdentifier = this.getClass().getName();

    private Process managedProcess;

    private Path outFile;

    private Path errFile;

    private boolean keepStreamFiles = false;

    private EventDetector startupDetector = DelayEventDetector.withDelayMillis(1000);

    private EventDetector shutdownDetector = RecursiveProcessTerminationEventDetector.newInstance();

    private ProcessDestroyer processDestroyer = DefaultProcessDestroyer.newInstance();

    @Override
    public void start() {
        requestedState = TestProcessState.STARTED;

        try {
            outFile = File.createTempFile(getProcessIdentifier(), "-out.txt").toPath();
            errFile = File.createTempFile(getProcessIdentifier(), "-err.txt").toPath();
            if (!keepStreamFiles) {
                outFile.toFile().deleteOnExit();
                errFile.toFile().deleteOnExit();
            }

            ProcessBuilder processBuilder = new ProcessBuilder()
                .redirectOutput(outFile.toFile())
                .redirectError(errFile.toFile());

            buildProcess(processBuilder);

            if (LOG.isDebugEnabled()) {
                LOG.debug("Starting command: {}", processBuilder.command().stream().collect(Collectors.joining(" ")));
            }
            managedProcess = processBuilder.start();
        } catch (IOException e) {
            LOG.error("Error starting test process: " + e.getMessage(), e);
            throw new UncheckedIOException("Error starting test process: " + e.getMessage(), e);
        }
    }

    /**
     * Allows for the customization of the {@link ProcessBuilder} instance that
     * will be used to build the process.
     * 
     * @param builder
     *            The builder to be used to customize the process to be built.
     */
    protected abstract void buildProcess(ProcessBuilder builder);

    @Override
    public void stop() {
        requestedState = TestProcessState.STOPPED;
    }

    @Override
    public String getProcessIdentifier() {
        return processIdentifier;
    }

    /**
     * Set the processIdentifier.
     * 
     * @param processIdentifier
     *            The processIdentifier to set.
     */
    public void setProcessIdentifier(String processIdentifier) {
        this.processIdentifier = processIdentifier;
    }

    @Override
    public TestProcessState getRequestedState() {
        return requestedState;
    }

    @Override
    public TestProcessState getActualState() {
        return (managedProcess != null && managedProcess.isAlive()) ? TestProcessState.STARTED
            : TestProcessState.STOPPED;
    }

    @Override
    public Optional<Path> getOutFile() {
        return Optional.ofNullable(outFile);
    }

    @Override
    public Optional<Path> getErrFile() {
        return Optional.ofNullable(errFile);
    }

    /**
     * Get the startupDetector.
     * 
     * @return Returns the startupDetector.
     */
    public EventDetector getStartupDetector() {
        return startupDetector;
    }

    /**
     * Set the startupDetector.
     * 
     * @param startupDetector
     *            The startupDetector to set.
     */
    public void setStartupDetector(EventDetector startupDetector) {
        this.startupDetector = startupDetector;
    }

    /**
     * Get the shutdownDetector.
     * 
     * @return Returns the shutdownDetector.
     */
    public EventDetector getShutdownDetector() {
        return shutdownDetector;
    }

    /**
     * Set the shutdownDetector.
     * 
     * @param shutdownDetector
     *            The shutdownDetector to set.
     */
    public void setShutdownDetector(EventDetector shutdownDetector) {
        this.shutdownDetector = shutdownDetector;
    }

    /**
     * Get the managedProcess. Mainly used for {@link EventDetector}s, do not
     * manipulate the Process directly!
     * 
     * @return Returns the managedProcess.
     */
    public Process getManagedProcess() {
        return managedProcess;
    }

    /**
     * Get the processDestroyer.
     * 
     * @return Returns the processDestroyer.
     */
    public ProcessDestroyer getProcessDestroyer() {
        return processDestroyer;
    }

    /**
     * Set the processDestroyer.
     * 
     * @param processDestroyer
     *            The processDestroyer to set.
     */
    public void setProcessDestroyer(ProcessDestroyer processDestroyer) {
        this.processDestroyer = processDestroyer;
    }

}
