package io.github.netmikey.testprocesses.functional;

import static io.github.netmikey.testprocesses.TestProcessDefinitionBy.*;
import static io.github.netmikey.testprocesses.functional.testfixtures.TestHelper.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.netmikey.testprocesses.TestProcess;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.eventdetector.LogPatternEventDetector;
import io.github.netmikey.testprocesses.functional.testfixtures.Echo;
import io.github.netmikey.testprocesses.functional.testfixtures.EchoTestProcess;
import io.github.netmikey.testprocesses.utils.StreamStart;

/**
 * Test that the {@link StreamStart} setting is honored when retrieving streams
 * from a running test process.
 * <p>
 * To do this, the test method execution is ordered. Beware of the order when
 * editing this test. Also, since the order and the state between the running
 * test methods is important, running single test methods will fail of course.
 */
@TestProcessesSpringBootTest
@TestMethodOrder(OrderAnnotation.class)
@TestProcess(EchoTestProcess.class)
public class StreamStartTest {

    private static final String MARKER_1 = "+++ This content powered by MARKER_1";

    private static final String MARKER_2 = "+++ This content powered by MARKER_2";

    @Autowired
    private TestProcessesRegistry registry;

    /**
     * Writes {@link #MARKER_1} into the {@link Echo} process' stdIn, waits for
     * it to be echoed to its stdOut.
     * 
     * @throws Exception
     *             Thrown when an unexpected error occurs.
     */
    @Test
    @Order(10)
    public void testWriteMarker1() throws Exception {
        sendToEchoProcess(registry, MARKER_1);

        registry.waitForEventOn(clazz(EchoTestProcess.class),
            LogPatternEventDetector.onStdOut().withMarker(MARKER_1));
    }

    /**
     * Writes {@link #MARKER_2} into the {@link Echo} process' stdOut and waits
     * for it to be echoed to its stdOut. It then tests that {@link #MARKER_1}
     * is visible when using {@link StreamStart#ABSOLUTE} but that it is NOT
     * visible when using {@link StreamStart#CURRENT_TEST}, since it hasn't been
     * written in the current but the previous test.
     * 
     * @throws Exception
     *             Thrown when an unexpected error occurs.
     */
    @Test
    @Order(20)
    public void testWriteMarker2AndCheckStreamStartBehavior() throws Exception {
        sendToEchoProcess(registry, MARKER_2);

        registry.waitForEventOn(clazz(EchoTestProcess.class),
            LogPatternEventDetector.onStdOut().withMarker(MARKER_2));

        Assertions.assertThat(registry.stdOutAsStringOf(clazz(EchoTestProcess.class), StreamStart.ABSOLUTE))
            .contains(MARKER_1, MARKER_2);

        Assertions.assertThat(registry.stdOutAsStringOf(clazz(EchoTestProcess.class), StreamStart.CURRENT_TEST))
            .contains(MARKER_2)
            .doesNotContain(MARKER_1);
    }
}
