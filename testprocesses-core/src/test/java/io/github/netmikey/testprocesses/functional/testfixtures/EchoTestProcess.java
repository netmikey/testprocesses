package io.github.netmikey.testprocesses.functional.testfixtures;

import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import io.github.netmikey.testprocesses.AbstractTestProcessDefinition;
import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.eventdetector.LogPatternEventDetector;

/**
 * A {@link TestProcessDefinition} for the {@link Echo} test process.
 */
@Component
public class EchoTestProcess extends AbstractTestProcessDefinition {

    /**
     * Default constructor.
     */
    public EchoTestProcess() {
        setStartupDetector(LogPatternEventDetector.onStdOut().withMarker("Echo process running"));
    }

    @Override
    protected void buildProcess(ProcessBuilder builder) {
        builder.command("java", "-cp", Paths.get("./build/classes/java/test/").toAbsolutePath().toString(),
            Echo.class.getName());
    }

    @Override
    public String getProcessIdentifier() {
        return "echo-process";
    }
}
