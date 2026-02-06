package io.github.netmikey.testprocesses.extensions;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import io.github.netmikey.testprocesses.TestProcessDefinitionBy;
import io.github.netmikey.testprocesses.TestProcessesRegistry;
import io.github.netmikey.testprocesses.utils.StreamPrintingUtils;
import io.github.netmikey.testprocesses.utils.StreamStart;

/**
 * A JUnit {@link Extension} that logs <code>stdOut</code> and
 * <code>stdErr</code> streams of each currently running test process after each
 * test. Only the output that has been produced on the stdOut/stdErr streams
 * during the current test is logged. If no output has been produced on a
 * stream, it is not logged.
 */
public class LogTestProcessesOutputExtension implements AfterEachCallback {

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        TestProcessesRegistry registry = SpringExtension.getApplicationContext(context)
            .getBean(TestProcessesRegistry.class);
        registry.runningProcessIdentifiers().forEach(processIdentifier -> {
            TestProcessDefinitionBy<?> selector = TestProcessDefinitionBy.processIdentifier(processIdentifier);
            registry.retrieveRunningProcess(selector).ifPresent(
                runningProcess -> StreamPrintingUtils.printOutAndErrStreams(runningProcess, StreamStart.CURRENT_TEST));
        });
    }

}
