package io.github.netmikey.testprocesses;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.test.context.TestContext;

import io.github.netmikey.testprocesses.eventdetector.EventDetector;
import io.github.netmikey.testprocesses.utils.StreamStart;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * A registry that holds all defined processes.
 */
@Component
public class TestProcessesRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(TestProcessesRegistry.class);

    @Autowired
    private ApplicationContext applicationContext;

    private Map<String, TestProcessDefinition> testProcessDefinitionBeans;

    private Map<String, RunningTestProcess<?>> runningProcesses = new ConcurrentHashMap<>();

    /**
     * Look up the {@link TestProcessDefinition} and start it using the
     * specified {@link StartStrategy}.
     * 
     * @param processDefinitionBy
     *            The reference to the {@link TestProcessDefinition} to be
     *            started.
     * @param startStrategy
     *            The {@link StartStrategy} to be used.
     * @param <T>
     *            The concrete type of the {@link TestProcessDefinition}.
     */
    public <T extends TestProcessDefinition> void start(TestProcessDefinitionBy<T> processDefinitionBy,
        StartStrategy startStrategy) {
        T newDefinition = retrieve(processDefinitionBy);
        String processIdentifier = newDefinition.getProcessIdentifier();
        RunningTestProcess<T> newRunningProcess = new RunningTestProcess<>(newDefinition);

        boolean needsStart = true;
        // Check if a process with the same identifier is already running
        if (runningProcesses.containsKey(processIdentifier)) {
            RunningTestProcess<?> runningProcess = runningProcesses.get(processIdentifier);
            if (newRunningProcess.getDefinition().equals(runningProcess.getDefinition()) && isRunning(runningProcess)
                && StartStrategy.USE_EXISTING.equals(startStrategy)) {

                LOG.debug("Not starting test process of definition class {} with identifier '{}' because it is "
                    + "already running and StartStrategy {} is used.",
                    newRunningProcess.getDefinition().getClass().getName(), processIdentifier, startStrategy);
                needsStart = false;
            } else {
                if (isRunning(runningProcess)) {
                    LOG.info("First stopping running test process with definition {} because test process definition "
                        + "{} needs to be started with the same process identifier '{}'.",
                        runningProcess.getDefinition(), newDefinition, processIdentifier);
                } else {
                    LOG.debug("Test process {} is not running anymore. Will formally stop and restart it because it "
                        + "is requested.", runningProcess.getDefinition().getProcessIdentifier());
                }
                doStop(processIdentifier);
            }
        }

        if (needsStart) {
            // Now start it
            LOG.info("Starting test process with identifier {} of definition type {}",
                newRunningProcess.getDefinition().getProcessIdentifier(),
                newRunningProcess.getDefinition().getClass().getName());
            runningProcesses.put(processIdentifier, newRunningProcess);
            newRunningProcess.getDefinition().start();

            try {
                newRunningProcess.getDefinition().getStartupDetector().waitForEvent(newRunningProcess);
            } catch (TimeoutException e) {
                LOG.warn("Timeout while waiting for process " + newRunningProcess.getDefinition().getProcessIdentifier()
                    + " to finish starting up. The process may not have started correctly. " + e.getMessage());
            }
        }
    }

    /**
     * Look up the {@link TestProcessDefinition} bean and stop it.
     * 
     * @param processDefinitionBy
     *            The reference to the {@link TestProcessDefinition} bean to be
     *            stopped.
     */
    public void stop(TestProcessDefinitionBy<?> processDefinitionBy) {
        doStop(retrieve(processDefinitionBy).getProcessIdentifier());
    }

    /**
     * Uses an {@link EventDetector} to wait for an event on the referenced test
     * process.
     * 
     * @param testProcessDefinitionBy
     *            The reference to the {@link TestProcessDefinition}.
     * @param eventDetector
     *            The {@link EventDetector} to be used.
     * @throws TimeoutException
     *             Thrown by the {@link EventDetector} if its configured timeout
     *             occurs before the event has occurred.
     */
    public void waitForEventOn(TestProcessDefinitionBy<?> testProcessDefinitionBy, EventDetector eventDetector)
        throws TimeoutException {

        RunningTestProcess<?> runningTestProcess = retrieveRunningProcessOrElseThrow(testProcessDefinitionBy);
        eventDetector.waitForEvent(runningTestProcess);
    }

    /**
     * Reads the current part the stdOut stream of the referenced test process
     * into a string and returns it. Only the part of the stream since the
     * current test method started will be returned.
     * 
     * @param testProcessDefinitionBy
     *            The reference to the test process definition.
     * @return The current part of the stdOut stream as string.
     */
    public String stdOutAsStringOf(TestProcessDefinitionBy<?> testProcessDefinitionBy) {
        return stdOutAsStringOf(testProcessDefinitionBy, StreamStart.CURRENT_TEST);
    }

    /**
     * Reads the specified part the stdOut stream of the referenced test process
     * into a string and returns it.
     * 
     * @param testProcessDefinitionBy
     *            The reference to the test process definition.
     * @param streamStart
     *            Where the returned part of the stream should start.
     * @return The specified part of the stdOut stream as string.
     */
    public String stdOutAsStringOf(TestProcessDefinitionBy<?> testProcessDefinitionBy, StreamStart streamStart) {
        return streamAsStringOf(testProcessDefinitionBy, streamStart, "stdOut stream", this::stdOutOf);
    }

    /**
     * Reads the current part the stdErr stream of the referenced test process
     * into a string and returns it. Only the part of the stream since the
     * current test method started will be returned.
     * 
     * @param testProcessDefinitionBy
     *            The reference to the test process definition.
     * @return The current part of the stdErr stream as string.
     */
    public String stdErrAsStringOf(TestProcessDefinitionBy<?> testProcessDefinitionBy) {
        return stdErrAsStringOf(testProcessDefinitionBy, StreamStart.CURRENT_TEST);
    }

    /**
     * Reads the specified part the stdErr stream of the referenced test process
     * into a string and returns it.
     * 
     * @param testProcessDefinitionBy
     *            The reference to the test process definition.
     * @param streamStart
     *            Where the returned part of the stream should start.
     * @return The current part of the stdErr stream as string.
     */
    public String stdErrAsStringOf(TestProcessDefinitionBy<?> testProcessDefinitionBy, StreamStart streamStart) {
        return streamAsStringOf(testProcessDefinitionBy, streamStart, "stdErr stream", this::stdErrOf);
    }

    private String streamAsStringOf(TestProcessDefinitionBy<?> testProcessDefinitionBy, StreamStart streamStart,
        String streamDescription, BiFunction<TestProcessDefinitionBy<?>, StreamStart, InputStream> streamRetriever) {

        try (InputStream stdErr = streamRetriever.apply(testProcessDefinitionBy, streamStart)) {
            return new String(stdErr.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Error opening new InputStream to process' "
                + streamDescription + ": " + e.getMessage(), e);
        }
    }

    /**
     * Returns an InputStream pointing at the specified position of the
     * referenced test process' stdOut stream.
     * 
     * @param testProcessDefinitionBy
     *            The reference to the test process definition.
     * @param streamStart
     *            Where the returned part of the stream should start.
     * @return The current part of the stdOut stream.
     */
    public InputStream stdOutOf(TestProcessDefinitionBy<?> testProcessDefinitionBy, StreamStart streamStart) {
        return streamOf(testProcessDefinitionBy, streamStart, "stdOut stream",
            FileBackedOutErrStreams::getOutFile,
            RunningTestProcess::getCurrentTestStdOutStart);
    }

    /**
     * Returns an InputStream pointing at the specified part of the referenced
     * test process' stdErr stream.
     * 
     * @param testProcessDefinitionBy
     *            The reference to the test process definition.
     * @param streamStart
     *            Where the returned part of the stream should start.
     * @return The current part of the stdErr stream.
     */
    public InputStream stdErrOf(TestProcessDefinitionBy<?> testProcessDefinitionBy, StreamStart streamStart) {
        return streamOf(testProcessDefinitionBy, streamStart, "stdErr stream",
            FileBackedOutErrStreams::getErrFile,
            RunningTestProcess::getCurrentTestStdErrStart);
    }

    private InputStream streamOf(TestProcessDefinitionBy<?> testProcessDefinitionBy, StreamStart streamStart,
        String streamDescription, Function<FileBackedOutErrStreams, Optional<Path>> streamFileRetriever,
        Function<RunningTestProcess<?>, Optional<Long>> streamPositionRetriever) {

        RunningTestProcess<?> runningTestProcess = retrieveRunningProcessOrElseThrow(testProcessDefinitionBy);
        if (!(runningTestProcess.getDefinition() instanceof FileBackedOutErrStreams)) {
            throw new IllegalStateException("TestProcessDefinition class must implement "
                + FileBackedOutErrStreams.class + " in order to access its streams");
        }
        Path streamFile = streamFileRetriever.apply((FileBackedOutErrStreams) runningTestProcess.getDefinition())
            .orElseThrow(() -> new IllegalStateException(
                "Test process " + runningTestProcess.getDefinition().getProcessIdentifier()
                    + " defined in bean " + runningTestProcess.getDefinition()
                    + " seems to be running but does not have an " + streamDescription
                    + " assigned to it yet. This shouldn't happen."));

        Optional<Long> streamFileStartPositionOfCurrentTest = streamPositionRetriever.apply(runningTestProcess);

        try {
            /*
             * Open new channel, seek to the position in it where the current
             * test started, and return a new InputStream from this position to
             * the end of the file.
             */
            SeekableByteChannel streamFileChannel = Files.newByteChannel(streamFile, StandardOpenOption.READ);
            if (StreamStart.CURRENT_TEST.equals(streamStart)
                && streamFileStartPositionOfCurrentTest.isPresent()) {

                streamFileChannel.position(streamFileStartPositionOfCurrentTest.get());
            }
            return Channels.newInputStream(streamFileChannel);
        } catch (IOException e) {
            throw new UncheckedIOException("Error opening new InputStream to process' "
                + streamDescription + ": " + e.getMessage(), e);
        }
    }

    /**
     * Intentionally package-visible test-lifecycle method, invoked by the
     * {@link TestProcessesListener}.
     * 
     * @param testContext
     *            The {@link TestContext}.
     */
    void beforeTestMethod(TestContext testContext) {
        runningProcesses.forEach((processIdentifier, runningProcess) -> runningProcess.onTestStart());
    }

    /**
     * Intentionally package-visible test-lifecycle method, invoked by the
     * {@link TestProcessesListener}.
     * 
     * @param testContext
     *            The {@link TestContext}.
     */
    void afterTestMethod(TestContext testContext) {
        runningProcesses.forEach((processIdentifier, runningProcess) -> runningProcess.onTestEnd());
    }

    @SuppressWarnings("unchecked")
    private <T extends TestProcessDefinition> T retrieve(TestProcessDefinitionBy<T> testProcessDefinitionBy)
        throws UnknownTestProcessDefinitionException, TooManyTestProcessDefinitionsException {

        T result;
        if (testProcessDefinitionBy.getBeanName().isPresent()) {
            String beanName = testProcessDefinitionBy.getBeanName().get();
            if (!testProcessDefinitionBeans.containsKey(beanName)) {
                throw new UnknownTestProcessDefinitionException(
                    "No " + TestProcessDefinition.class.getSimpleName() + " with bean name '"
                        + beanName + "' could be found. Make sure you have a bean named '" + beanName
                        + "' implementing " + TestProcessDefinition.class.getSimpleName()
                        + " in your Spring Test-Context.");
            }
            result = (T) testProcessDefinitionBeans.get(beanName);
        } else if (testProcessDefinitionBy.getClazz().isPresent()) {
            Class<? extends TestProcessDefinition> processDefinition = testProcessDefinitionBy.getClazz().get();

            // First, try to find a match within the currently running processes
            Map<String, T> matchingRunning = runningProcesses.entrySet().stream()
                .filter(e -> processDefinition.equals(e.getValue().getDefinition().getClass()))
                .collect(Collectors.toMap(
                    e -> "instance " + e.getValue().getDefinition() + " with identifier "
                        + e.getValue().getDefinition().getProcessIdentifier(),
                    e -> (T) e.getValue().getDefinition()));

            if (matchingRunning.size() > 1) {
                throw new TooManyTestProcessDefinitionsException("More than one TestProcessDefinition bean of type "
                    + processDefinition.getName() + " were found to be running. Either subclass your "
                    + TestProcessDefinition.class.getSimpleName() + " for each individual definition or reference "
                    + "it using the instance or bean name rather than the class. Found beans were: "
                    + matchingRunning.keySet().stream().collect(Collectors.joining(", ")));
            }

            if (matchingRunning.size() == 1) {
                result = matchingRunning.values()
                    .stream()
                    .findFirst()
                    .get();
            } else {
                // If no matching definition is running, try to find a matching
                // Spring bean

                Map<String, T> matchingBeans = testProcessDefinitionBeans.entrySet().stream()
                    .filter(e -> processDefinition.equals(e.getValue().getClass()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> (T) e.getValue()));

                if (matchingBeans.size() == 0) {
                    throw new UnknownTestProcessDefinitionException(
                        "No " + TestProcessDefinition.class.getSimpleName() + " of class "
                            + processDefinition.getName() + " could be found. Make sure you have a bean of this type "
                            + "in your Spring Test-Context.");
                }

                if (matchingBeans.size() > 1) {
                    throw new TooManyTestProcessDefinitionsException("More than one TestProcessDefinition bean of type "
                        + processDefinition.getName() + " were found. Either subclass your "
                        + TestProcessDefinition.class.getSimpleName() + " for each individual definition or reference "
                        + "it using the instance or bean name rather than the class. Found beans were: "
                        + matchingBeans.keySet().stream().collect(Collectors.joining(", ")));
                }

                result = matchingBeans.values()
                    .stream()
                    .findFirst()
                    .get();
            }
        } else if (testProcessDefinitionBy.getProcessIdentifier().isPresent()) {
            String processIdentifier = testProcessDefinitionBy.getProcessIdentifier().get();

            // First, try to find a match within the currently running processes
            Map<String, T> matchingRunning = runningProcesses.entrySet().stream()
                .filter(p -> processIdentifier.equals(p.getValue().getDefinition().getProcessIdentifier()))
                .collect(Collectors.toMap(
                    e -> "instance " + e.getValue().getDefinition() + " with identifier "
                        + e.getValue().getDefinition().getProcessIdentifier(),
                    e -> (T) e.getValue().getDefinition()));

            // Note that this shouldn't happen as only one process with a given
            // processIdentifier should run at any time, but check for good
            // measure...
            if (matchingRunning.size() > 1) {
                throw new TooManyTestProcessDefinitionsException("More than one TestProcessDefinition bean with "
                    + "identifier " + processIdentifier + " were found to be running. Consider referencing your "
                    + "test process using the class, instance or bean name rather than the process identifier. "
                    + "Found beans were: " + matchingRunning.keySet().stream().collect(Collectors.joining(", ")));
            }

            if (matchingRunning.size() == 1) {
                result = matchingRunning.values()
                    .stream()
                    .findFirst()
                    .get();
            } else {
                // If no matching definition is running, try to find a matching
                // Spring bean

                Map<String, T> matchingBeans = testProcessDefinitionBeans.entrySet().stream()
                    .filter(b -> processIdentifier.equals(b.getValue().getProcessIdentifier()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> (T) e.getValue()));

                if (matchingBeans.size() == 0) {
                    throw new UnknownTestProcessDefinitionException(
                        "No " + TestProcessDefinition.class.getSimpleName() + " with identifier "
                            + processIdentifier + " could be found. Make sure you have a bean of type "
                            + TestProcessDefinition.class.getSimpleName()
                            + "that uses this identifier in your Spring Test-Context.");
                }

                if (matchingBeans.size() > 1) {
                    throw new TooManyTestProcessDefinitionsException("More than one TestProcessDefinition "
                        + "bean with identifier " + processIdentifier + " were found. Consider referencing it "
                        + "using its class, the instance or bean name rather than the process identifier. "
                        + "Found beans were: " + matchingBeans.keySet().stream().collect(Collectors.joining(", ")));
                }

                result = matchingBeans.values()
                    .stream()
                    .findFirst()
                    .get();
            }
        } else {
            result = testProcessDefinitionBy.getInstance().get();
        }
        return result;
    }

    /**
     * Retrieve a currently {@link RunningTestProcess}.
     * 
     * @param testProcessDefinitionBy
     *            The reference to the running process'
     *            {@link TestProcessDefinition}.
     * @return The {@link RunningTestProcess} or {@link Optional#empty()} if no
     *         process has been started that matches the specified
     *         {@link TestProcessDefinitionBy} reference.
     * @param <T>
     *            The concrete type of the {@link TestProcessDefinition}.
     */
    public <T extends TestProcessDefinition> Optional<RunningTestProcess<T>> retrieveRunningProcess(
        TestProcessDefinitionBy<T> testProcessDefinitionBy) {

        try {
            T testProcessDefinition = retrieve(testProcessDefinitionBy);
            boolean requiresIdentityCheck = testProcessDefinitionBy.getClazz().isEmpty();
            return retrieveRunningProcessByProcessDefinition(testProcessDefinition, requiresIdentityCheck);
        } catch (UnknownTestProcessDefinitionException e) {
            return Optional.empty();
        }
    }

    private <T extends TestProcessDefinition> RunningTestProcess<T> retrieveRunningProcessOrElseThrow(
        TestProcessDefinitionBy<T> testProcessDefinitionBy) {

        T testProcessDefinition = retrieve(testProcessDefinitionBy);
        boolean requiresIdentityCheck = testProcessDefinitionBy.getClazz().isEmpty();
        return retrieveRunningProcessByProcessDefinition(testProcessDefinition, requiresIdentityCheck)
            .orElseThrow(() -> {
                String beanNameHint = testProcessDefinitionBy.getBeanName()
                    .map(bean -> " (bean " + bean + ")")
                    .orElse("");

                return new IllegalStateException("No test process of definition class "
                    + testProcessDefinition.getClass().getName() + " with identifier "
                    + testProcessDefinition.getProcessIdentifier() + beanNameHint + " is running");
            });
    }

    private <T extends TestProcessDefinition> Optional<RunningTestProcess<T>> retrieveRunningProcessByProcessDefinition(
        T definition, boolean identityCheck) {

        String processIdentifier = definition.getProcessIdentifier();
        if (runningProcesses.containsKey(processIdentifier)) {
            @SuppressWarnings("unchecked")
            RunningTestProcess<T> runningProcess = (RunningTestProcess<T>) runningProcesses.get(processIdentifier);
            if ((!identityCheck && definition.getClass().equals(runningProcess.getDefinition().getClass()))
                || definition.equals(runningProcess.getDefinition())) {
                return Optional.of(runningProcess);
            }
        }
        return Optional.empty();
    }

    private void doStop(String processIdentifier) {
        RunningTestProcess<?> runningProcess = runningProcesses.get(processIdentifier);
        if (runningProcess != null) {
            runningProcess.getDefinition().stop();
            runningProcess.getDefinition().getProcessDestroyer().destroy(runningProcess);
            try {
                runningProcess.getDefinition().getShutdownDetector().waitForEvent(runningProcess);
            } catch (TimeoutException e) {
                LOG.warn("Timeout while waiting for process " + runningProcess.getDefinition().getProcessIdentifier()
                    + " to finish shutting down. The process may not have stopped correctly. " + e.getMessage());
            }
            debugOutErrFiles(runningProcess.getDefinition());
        }
        runningProcesses.remove(processIdentifier);
    }

    private void debugOutErrFiles(TestProcessDefinition testProcessDefinition) {
        if (LOG.isTraceEnabled() && testProcessDefinition instanceof FileBackedOutErrStreams) {
            FileBackedOutErrStreams testProcess = (FileBackedOutErrStreams) testProcessDefinition;
            testProcess.getOutFile().ifPresent(outFile -> {
                try (InputStream fis = new FileInputStream(outFile.toFile())) {
                    System.out.println("-------------------- stdout --------------------");
                    fis.transferTo(System.out);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            testProcess.getErrFile().ifPresent(errFile -> {
                try (InputStream fis = new FileInputStream(errFile.toFile())) {
                    System.out.println("-------------------- stderr --------------------");
                    fis.transferTo(System.out);
                    System.out.println("------------------------------------------------");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private boolean isRunning(RunningTestProcess<?> runningProcess) {
        return TestProcessState.STARTED.equals(runningProcess.getDefinition().getActualState());
    }

    @PostConstruct
    private void init() {
        testProcessDefinitionBeans = applicationContext.getBeansOfType(TestProcessDefinition.class);
    }

    @PreDestroy
    private void shutdown() {
        if (runningProcesses.size() > 0) {
            LOG.info("Test context shutting down: Destroying {} running test process{}", runningProcesses.size(),
                runningProcesses.size() > 1 ? "es" : "");
        }

        runningProcesses.forEach((processId, runningProcess) -> doStop(processId));

        if (runningProcesses.size() != 0) {
            String unstoppedProcesses = runningProcesses.entrySet().stream()
                .map(e -> e.getValue().getDefinition().getClass() + " ("
                    + e.getValue().getDefinition().getProcessIdentifier() + ")")
                .collect(Collectors.joining(", "));

            LOG.warn("It seems we were unable to stop all test processes before shutting down the test context. "
                + "Test processes that could not be stopped: " + unstoppedProcesses);
        }
    }
}
