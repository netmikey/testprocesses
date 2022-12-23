package io.github.netmikey.testprocesses.functional;

import static io.github.netmikey.testprocesses.TestProcessDefinitionBy.*;
import static io.github.netmikey.testprocesses.functional.testfixtures.TestHelper.*;

import java.nio.file.Paths;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.netmikey.testprocesses.StartStrategy;
import io.github.netmikey.testprocesses.TestProcess;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.functional.testfixtures.Echo;
import io.github.netmikey.testprocesses.functional.testfixtures.EchoTestProcess;
import io.github.netmikey.testprocesses.functional.testfixtures.GreetingEchoTestProcess;

/**
 * Test behavior whenthe same process identifier is being (re-)used.
 * <p>
 * To do this, the test method execution is ordered. Beware of the order when
 * editing this test. Also, since the order and the state between the running
 * test methods is important, running single test methods will fail of course.
 */
@TestProcessesSpringBootTest
@TestMethodOrder(OrderAnnotation.class)
public class SameIdentifierTest {

    @Autowired
    private TestProcessesRegistry registry;

    private EchoTestProcess customEchoTestProcessSubclass = new EchoTestProcess() {
        @Override
        protected void buildProcess(ProcessBuilder builder) {
            builder.command("java", "-cp", Paths.get("./build/classes/java/test/").toAbsolutePath().toString(),
                Echo.class.getName(), "Hi Steve");
        }
    };

    /**
     * This test makes sure that the {@link EchoTestProcess} is running.
     */
    @Test
    @TestProcess(EchoTestProcess.class)
    @Order(0)
    public void testStartEchoIfNotYetRunning() {
        assertRunningByClass(registry, EchoTestProcess.class);
    }

    /**
     * This test asks the {@link GreetingEchoTestProcess} to be started. That
     * one is a variant (a subclass in fact) of {@link EchoTestProcess} and
     * notably uses the same process Identifier. TestProcesses is expected to
     * stop the running process with the same identifier and start the newly
     * requested one instead.
     */
    @Test
    @TestProcess(GreetingEchoTestProcess.class)
    @Order(10)
    public void testEchoIsReplacedWhenUsingTheSameProcessIdentifier() {
        assertNotRunningByClass(registry, EchoTestProcess.class);
        assertRunningByClass(registry, GreetingEchoTestProcess.class);
    }

    /**
     * This test uses the API to start a custom {@link EchoTestProcess} subclass
     * that uses the same process identifier. TestProcesses is expected to stop
     * the running process with the same identifier and start the newly
     * requested one instead.
     */
    @Test
    @Order(20)
    public void testGreetingEchoIsReplacedWhenUsingTheSameProcessIdentifier() {
        try {
            registry.start(instance(customEchoTestProcessSubclass), StartStrategy.USE_EXISTING);
            assertNotRunningByClass(registry, EchoTestProcess.class);
            assertNotRunningByClass(registry, GreetingEchoTestProcess.class);
        } finally {
            registry.stop(instance(customEchoTestProcessSubclass));
        }
    }

}
