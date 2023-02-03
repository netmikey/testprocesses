package io.github.netmikey.testprocesses.functional;

import static io.github.netmikey.testprocesses.TestProcessDefinitionBy.*;
import static io.github.netmikey.testprocesses.functional.testfixtures.TestHelper.*;

import org.junit.jupiter.api.TestMethodOrder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.netmikey.testprocesses.StartStrategy;
import io.github.netmikey.testprocesses.TestProcessDefinitionBy;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.functional.testfixtures.EchoTestProcess;
import io.github.netmikey.testprocesses.utils.StreamStart;

/**
 * Test the bahvior of the {@link StreamStart} setting when retrieving streams
 * from a test process that has been started imperatively using the API (instead
 * of the test-method-bound annotation).
 * <p>
 * To do this, the test method execution is ordered. Beware of the order when
 * editing this test. Also, since the order and the state between the running
 * test methods is important, running single test methods will fail of course.
 */
@TestProcessesSpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class StreamStartImperativeTest {

    private static final String MARKER_1 = "+++ MARKER_1";

    private static final String MARKER_2 = "+++ MARKER_2";

    @Autowired
    private TestProcessesRegistry registry;

    private TestProcessDefinitionBy<EchoTestProcess> echoTestProcess = clazz(EchoTestProcess.class);

    /**
     * Test starting a test process imperatively / "manually" / using the API
     * and then retrieving its stdOut stream.
     */
    @Test
    @Order(1)
    public void testWithProcessStartedViaAPI() {
        // Start EchoTestProcess using API
        registry.start(echoTestProcess, StartStrategy.REQUIRE_RESTART);

        sendToEchoProcess(registry, MARKER_1);

        // JUnit Test was running before EchoTestProcess, should return the
        // whole stream
        String absoluteLog = registry.stdOutAsStringOf(echoTestProcess, StreamStart.ABSOLUTE);
        String currentTestLog = registry.stdOutAsStringOf(echoTestProcess, StreamStart.CURRENT_TEST);

        // Both should be equal
        Assertions.assertThat(absoluteLog).isEqualTo(currentTestLog);
    }

    /**
     * Test that a test process started imperatively / "manually" / using the
     * API in a previous test obtains the "current test" markers on its streams
     * when the present test starts and behaves as any regular already-running
     * process.
     */
    @Test
    @Order(2)
    public void testWithProcessFromPreviousTestStartedViaAPI() {
        // EchoTestProcess should still be running

        sendToEchoProcess(registry, MARKER_2);

        Assertions.assertThat(registry.stdOutAsStringOf(echoTestProcess, StreamStart.ABSOLUTE))
            .contains(MARKER_1, MARKER_2);

        Assertions.assertThat(registry.stdOutAsStringOf(echoTestProcess, StreamStart.CURRENT_TEST))
            .contains(MARKER_2)
            .doesNotContain(MARKER_1);
    }
}
