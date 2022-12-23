package io.github.netmikey.testprocesses.functional.testfixtures;

import java.nio.file.Paths;

import org.springframework.stereotype.Component;

/**
 * A variant of the {@link EchoTestProcess} that adds a greeting parameter at
 * launch.
 */
@Component
public class GreetingEchoTestProcess extends EchoTestProcess {
    @Override
    protected void buildProcess(ProcessBuilder builder) {
        builder.command("java", "-cp", Paths.get("./build/classes/java/test/").toAbsolutePath().toString(),
            Echo.class.getName(), "Hello Dave");
    }
}
