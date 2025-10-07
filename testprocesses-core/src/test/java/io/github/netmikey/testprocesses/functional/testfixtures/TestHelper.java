package io.github.netmikey.testprocesses.functional.testfixtures;

import static io.github.netmikey.testprocesses.TestProcessDefinitionBy.*;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import io.github.netmikey.testprocesses.RunningTestProcess;
import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.TestProcessDefinitionBy;
import io.github.netmikey.testprocesses.TestProcessState;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.eventdetector.LogPatternEventDetector;

/**
 * Utility method for asserting and interacting with those test processes
 * specifically created for these framework-tests.
 */
public class TestHelper {

    private static final TestProcessDefinitionBy<EchoTestProcess> ECHO_TEST_PROCESS = TestProcessDefinitionBy
        .clazz(EchoTestProcess.class);

    /**
     * Sends a line of test to the {@link EchoTestProcess} currently running in
     * the specified registry.
     * 
     * @param registry
     *            The {@link TestProcessesRegistry} to be used.
     * @param line
     *            The lins to be sent to the Echo process' stdIn.
     */
    public static void sendToEchoProcess(TestProcessesRegistry registry, String line) {
        Optional<RunningTestProcess<EchoTestProcess>> runningProcess = registry
            .retrieveRunningProcess(ECHO_TEST_PROCESS);

        if (runningProcess.isPresent()) {
            try {
                /*
                 * Note: closing this writer would close the Echo Process' stdIn
                 * stream and make the Echo process exit. Therefor, we
                 * intentionally don't close this writer. When TestProcesses
                 * will be moved to a Java17+ baseline, we can use
                 * Process#outputWriter(Charset) instead.
                 */
                BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(runningProcess.get().getDefinition().getManagedProcess().getOutputStream(),
                        StandardCharsets.UTF_8));

                writer.append(line);
                writer.newLine();
                writer.flush();
            } catch (IOException e) {
                throw new UncheckedIOException(e.getMessage(), e);
            }

            // Wait for the line to appear on the echo process' stdOut
            try {
                registry.waitForEventOn(ECHO_TEST_PROCESS, LogPatternEventDetector.onStdOut().withMarker(line));
            } catch (TimeoutException e) {
                throw new RuntimeException("Timeout while waiting for the line to appear on the EchoProcess' stdOut",
                    e);
            }
        } else {
            throw new IllegalStateException("Trying to write to " + EchoTestProcess.class.getName()
                + "'s stdIn, but no process is running.");
        }
    }

    /**
     * Assert the {@link EchoTestProcess} is currently running using its class
     * for the lookup.
     * 
     * @param registry
     *            The registry on which the lookup should be performed.
     */
    public static void assertEchoRunningByClass(TestProcessesRegistry registry) {
        assertRunningByClass(registry, EchoTestProcess.class);
    }

    /**
     * Assert the {@link EchoTestProcess} is currently running using its process
     * identifier for the lookup.
     * 
     * @param registry
     *            The registry on which the lookup should be performed.
     */
    public static void assertEchoRunningByProcessIdentifier(TestProcessesRegistry registry) {
        assertRunningByProcessIdentifier(registry, EchoTestProcess.PROCESS_IDENTIFIER);
    }

    /**
     * Assert the {@link TestProcessDefinition} is currently running using its
     * class for the lookup.
     * 
     * @param registry
     *            The registry on which the lookup should be performed.
     * @param clazz
     *            The {@link TestProcessDefinition} class to be looked up.
     */
    public static void assertRunningByClass(TestProcessesRegistry registry,
        Class<? extends TestProcessDefinition> clazz) {

        RunningTestProcess<?> runningEchoProcess = registry.retrieveRunningProcess(clazz(clazz))
            .orElseThrow(() -> new AssertionFailedError(clazz.getSimpleName() + " should have been started"));
        Assertions.assertEquals(TestProcessState.STARTED, runningEchoProcess.getDefinition().getActualState());
    }

    /**
     * Assert the {@link TestProcessDefinition} is currently running using its
     * process identifier for the lookup.
     * 
     * @param registry
     *            The registry on which the lookup should be performed.
     * @param processIdentifier
     *            The process identifier to be looked up.
     */
    public static void assertRunningByProcessIdentifier(TestProcessesRegistry registry, String processIdentifier) {

        RunningTestProcess<?> runningEchoProcess = registry.retrieveRunningProcess(processIdentifier(processIdentifier))
            .orElseThrow(() -> new AssertionFailedError(
                "process with identifier `" + processIdentifier + "` should have been started"));
        Assertions.assertEquals(TestProcessState.STARTED, runningEchoProcess.getDefinition().getActualState());
    }

    /**
     * Assert the {@link EchoTestProcess} is currently running using its
     * {@link EchoTestProcess} instance for the lookup.
     * 
     * @param registry
     *            The registry on which the lookup should be performed.
     * @param instance
     *            The {@link EchoTestProcess} instance to look for.
     */
    public static void assertEchoRunningByInstance(TestProcessesRegistry registry, EchoTestProcess instance) {
        RunningTestProcess<?> runningEchoProcess = registry.retrieveRunningProcess(instance(instance))
            .orElseThrow(
                () -> new AssertionFailedError(instance + " should have been started"));
        Assertions.assertEquals(TestProcessState.STARTED, runningEchoProcess.getDefinition().getActualState());
    }

    /**
     * Assert the {@link EchoTestProcess} is NOT currently running using its
     * class for the lookup.
     * 
     * @param registry
     *            The registry on which the lookup should be performed.
     */
    public static void assertEchoNotRunningByClass(TestProcessesRegistry registry) {
        Assertions.assertFalse(registry.retrieveRunningProcess(clazz(EchoTestProcess.class)).isPresent(),
            EchoTestProcess.class.getSimpleName() + " should have been stopped, should not be running");
    }

    /**
     * Assert the {@link TestProcessDefinition} is NOT currently running using
     * its class for the lookup.
     * 
     * @param registry
     *            The registry on which the lookup should be performed.
     * @param clazz
     *            The {@link TestProcessDefinition} class to be looked up.
     */
    public static void assertNotRunningByClass(TestProcessesRegistry registry,
        Class<? extends TestProcessDefinition> clazz) {

        Assertions.assertFalse(registry.retrieveRunningProcess(clazz(clazz)).isPresent(),
            clazz.getSimpleName() + " should have been stopped, should not be running");
    }

    /**
     * Assert the {@link EchoTestProcess} is NOT currently running using its
     * {@link EchoTestProcess} instance for the lookup.
     * 
     * @param registry
     *            The registry on which the lookup should be performed.
     * @param instance
     *            The {@link EchoTestProcess} instance to look for.
     */
    public static void assertEchoNotRunningByInstance(TestProcessesRegistry registry, EchoTestProcess instance) {
        Assertions.assertFalse(registry.retrieveRunningProcess(instance(instance)).isPresent(),
            instance + " should have been stopped, should not be running");
    }
}
