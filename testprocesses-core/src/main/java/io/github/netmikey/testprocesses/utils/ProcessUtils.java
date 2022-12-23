package io.github.netmikey.testprocesses.utils;

import io.github.netmikey.testprocesses.AbstractTestProcessDefinition;
import io.github.netmikey.testprocesses.TestProcessDefinition;

/**
 * Internal TestProcesses utility class.
 */
public final class ProcessUtils {

    private ProcessUtils() {
        // Do not instantiate, please.
    }

    /**
     * Retrieve the {@link Process} from the {@link TestProcessDefinition}.
     * 
     * @param processDefinition
     *            The {@link TestProcessDefinition}.
     * @return The {@link Process}.
     */
    public static Process retrieveManagedProcess(TestProcessDefinition processDefinition) {
        if (!(processDefinition instanceof AbstractTestProcessDefinition)) {
            throw new IllegalStateException("This operation only works with "
                + TestProcessDefinition.class.getSimpleName() + "s that are subtypes of "
                + AbstractTestProcessDefinition.class.getSimpleName());
        }

        Process managedProcess = ((AbstractTestProcessDefinition) processDefinition).getManagedProcess();
        return managedProcess;
    }
}
