package io.github.netmikey.testprocesses.extensions;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.util.StringUtils;

import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.TestProcessDefinitionBy;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.utils.StreamStart;

/**
 * A JUnit {@link Extension} that logs <code>stdOut</code> and
 * <code>stdErr</code> streams of each currently running test process after each
 * test. Only the output that has been produced on the stdOut/stdErr streams
 * during the current test is logged. If no output has been produced on a
 * stream, it is not logged.
 */
public class LogTestProcessesOutputExtension implements AfterEachCallback {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory
        .getLogger(LogTestProcessesOutputExtension.class);

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        TestProcessesRegistry registry = SpringExtension.getApplicationContext(context)
            .getBean(TestProcessesRegistry.class);
        registry.runningProcessIdentifiers().forEach(processIdentifier -> {
            TestProcessDefinitionBy<? extends TestProcessDefinition> selector = TestProcessDefinitionBy
                .processIdentifier(processIdentifier);
            logStream(processIdentifier, "stdOut", registry.stdOutAsStringOf(selector, StreamStart.CURRENT_TEST));
            logStream(processIdentifier, "stdErr", registry.stdErrAsStringOf(selector, StreamStart.CURRENT_TEST));
        });
    }

    private void logStream(String processIdentifier, String streamName, String content) {
        if (StringUtils.hasText(content)) {
            String title = processIdentifier;
            String delimiter = "-------------------- %s %s --------------------"
                .formatted(title, streamName);
            LOG.info("\n{}\n{}\n{}",
                delimiter,
                content.replaceAll("\r?\n$", "").replaceAll("(^|\r?\n)", "$1  "),
                "-".repeat(delimiter.length()));
        }
    }
}
