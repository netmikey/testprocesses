package io.github.netmikey.testprocesses.functional;

import static io.github.netmikey.testprocesses.TestProcessDefinitionBy.*;
import static io.github.netmikey.testprocesses.functional.testfixtures.TestHelper.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.netmikey.testprocesses.StartStrategy;
import io.github.netmikey.testprocesses.StopStrategy;
import io.github.netmikey.testprocesses.TestProcess;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.functional.testfixtures.EchoTestProcess;
import io.github.netmikey.testprocesses.utils.StreamStart;

/**
 * Tests that TestProcesses honor the configured {@link StartStrategy} and
 * {@link StopStrategy}.
 * <p>
 * To do this, the test method execution is ordered. Beware of the order when
 * editing this test. Also, since the order and the state between the running
 * test methods is important, running single test methods will fail of course.
 */
@TestProcessesSpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class StartStopStrategiesTest {

    private static final String MARKER_LINE_1 = "line from testStartAndLeaveRunning";

    private static final String MARKER_LINE_2 = "line from testRequireRestartHasRestartedTheProcess";

    @Autowired
    private TestProcessesRegistry registry;

    /**
     * Stop the {@link EchoTestProcess} if it is still running from previous
     * tests.
     */
    @Test
    @Order(0)
    public void testNotRunningToBeginWith() {
        registry.stop(clazz(EchoTestProcess.class));
        assertEchoNotRunningByClass(registry);
    }

    /**
     * Start the {@link EchoTestProcess} and use
     * {@link StopStrategy#LEAVE_RUNNING} to leave it running after this test
     * method.
     */
    @Test
    @Order(10)
    @TestProcess(beanClass = EchoTestProcess.class, stopStrategy = StopStrategy.LEAVE_RUNNING)
    public void testStartAndLeaveRunning() {
        sendToEchoProcess(registry, MARKER_LINE_1);
        assertEchoRunningByClass(registry);
    }

    /**
     * This test method checks that the {@link EchoTestProcess} started by
     * {@link #testStartAndLeaveRunning()} is still running. It verifies that it
     * is in fact that very instance by checking that its stdOut still contains
     * output from the other test.
     */
    @Test
    @Order(20)
    @TestProcess(
        beanClass = EchoTestProcess.class,
        startStrategy = StartStrategy.USE_EXISTING,
        stopStrategy = StopStrategy.LEAVE_RUNNING)
    public void testSameInstanceStillRunning() {
        assertEchoRunningByClass(registry);
        Assertions.assertThat(registry.stdOutAsStringOf(clazz(EchoTestProcess.class), StreamStart.ABSOLUTE))
            .contains(MARKER_LINE_1);
    }

    /**
     * This test method checks that the {@link EchoTestProcess} started by
     * {@link #testStartAndLeaveRunning()} has been restarted for this test. It
     * verifies that it is in fact a fresh instance by checking that its stdOut
     * doesn't contain output from prevkous tests.
     */
    @Test
    @Order(30)
    @TestProcess(
        beanClass = EchoTestProcess.class,
        startStrategy = StartStrategy.REQUIRE_RESTART,
        stopStrategy = StopStrategy.LEAVE_RUNNING)
    public void testRequireRestartHasRestartedTheProcess() {
        assertEchoRunningByClass(registry);
        Assertions.assertThat(registry.stdOutAsStringOf(clazz(EchoTestProcess.class), StreamStart.ABSOLUTE))
            .doesNotContain(MARKER_LINE_1);
        sendToEchoProcess(registry, MARKER_LINE_2);
    }

    /**
     * This test method checks that the {@link EchoTestProcess} started by
     * {@link #testRequireRestartHasRestartedTheProcess()} is still running. It
     * verifies that it is in fact that very instance by checking that its
     * stdOut still contains output from the other test.
     */
    @Test
    @Order(40)
    @TestProcess(
        beanClass = EchoTestProcess.class,
        startStrategy = StartStrategy.USE_EXISTING,
        stopStrategy = StopStrategy.STOP_AFTER_TEST)
    public void testSameInstanceStillRunning2() {
        assertEchoRunningByClass(registry);
        Assertions.assertThat(registry.stdOutAsStringOf(clazz(EchoTestProcess.class), StreamStart.ABSOLUTE))
            .contains(MARKER_LINE_2);
    }

    /**
     * Test that the {@link EchoTestProcess} has been stopped after
     * {@link #testSameInstanceStillRunning2()} because it has been marked with
     * {@link StopStrategy#STOP_AFTER_TEST}.
     */
    @Test
    @Order(50)
    public void testHasBeenStoppedByStopAfterTest() {
        assertEchoNotRunningByClass(registry);
    }
}
