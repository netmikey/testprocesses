package io.github.netmikey.testprocesses.functional;

import static io.github.netmikey.testprocesses.functional.testfixtures.TestHelper.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.netmikey.testprocesses.TestProcess;
import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.functional.testfixtures.EchoTestProcess;
import io.github.netmikey.testprocesses.functional.testfixtures.SleeperTestProcess;

/**
 * Test that multiple {@link TestProcessDefinition}s can be used within the same
 * test.
 */
@TestProcessesSpringBootTest
public class MultipleProcessesTest {

    @Autowired
    private TestProcessesRegistry registry;

    /**
     * When mulitple {@link TestProcess} annotations are present, all of them
     * should be started.
     */
    @Test
    @TestProcess(EchoTestProcess.class)
    @TestProcess(SleeperTestProcess.class)
    public void testStartingMultipleTestProcesses() {
        assertRunningByClass(registry, EchoTestProcess.class);
        assertRunningByClass(registry, SleeperTestProcess.class);
    }
}
