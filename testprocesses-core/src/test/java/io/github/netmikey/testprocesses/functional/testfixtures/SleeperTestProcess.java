package io.github.netmikey.testprocesses.functional.testfixtures;

import java.nio.file.Paths;

import org.springframework.stereotype.Component;

import io.github.netmikey.testprocesses.AbstractTestProcessDefinition;
import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.eventdetector.LogPatternEventDetector;

/**
 * A {@link TestProcessDefinition} for the {@link Sleeper} test process.
 */
@Component
public class SleeperTestProcess extends AbstractTestProcessDefinition {

    /**
     * Default constructor.
     */
    public SleeperTestProcess() {
        setStartupDetector(LogPatternEventDetector
            .onStdOut()
            .withMarker(Sleeper.class.getSimpleName() + " process running"));
    }

    @Override
    protected void buildProcess(ProcessBuilder builder) {
        builder.command("java", "-cp", Paths.get("./build/classes/java/test/").toAbsolutePath().toString(),
            Sleeper.class.getName());
    }

}
