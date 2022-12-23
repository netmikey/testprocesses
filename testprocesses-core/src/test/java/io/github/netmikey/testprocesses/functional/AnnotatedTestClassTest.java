package io.github.netmikey.testprocesses.functional;

import static io.github.netmikey.testprocesses.functional.testfixtures.TestHelper.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import io.github.netmikey.testprocesses.TestProcess;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.functional.testfixtures.EchoTestProcess;

/**
 * Test that a test class can be annotated with {@link TestProcess} and that all
 * its {@link Test} methods will have the annotated process running.
 */
@TestProcessesSpringBootTest
@TestProcess(EchoTestProcess.class)
public class AnnotatedTestClassTest {

    @Autowired
    private TestProcessesRegistry registry;

    /**
     * This is one of the methods where the {@link EchoTestProcess} should be
     * running. The execution order may vary and is not important.
     */
    @Test
    public void oneTestWereTestProcessShouldBeRunning() {
        assertEchoRunningByClass(registry);
    }

    /**
     * This is another one of the methods where the {@link EchoTestProcess}
     * should be running. The execution order may vary and is not important.
     */
    @Test
    public void anotherTestWereTestProcessShouldBeRunning() {
        assertEchoRunningByClass(registry);
    }
}
