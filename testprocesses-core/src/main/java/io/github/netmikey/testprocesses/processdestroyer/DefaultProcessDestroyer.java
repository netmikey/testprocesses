package io.github.netmikey.testprocesses.processdestroyer;

import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.netmikey.testprocesses.RunningTestProcess;
import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.eventdetector.EventDetector;
import io.github.netmikey.testprocesses.eventdetector.RecursiveProcessTerminationEventDetector;
import io.github.netmikey.testprocesses.utils.ProcessUtils;

/**
 * This implementation first tries to soft-kill the process. If that doesn't
 * work after a timeout, it tries to destroy the process forcefully.
 * <p>
 * In addition to the managed process, it can also destroy its child processes
 * recursively or not. It applies this recursion by default.
 */
public class DefaultProcessDestroyer implements ProcessDestroyer {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultProcessDestroyer.class);

    private boolean recursive = true;

    private EventDetector softKillEventDetector = RecursiveProcessTerminationEventDetector.newInstance()
        .withTimeoutMillis(10000);

    /**
     * Create a new {@link DefaultProcessDestroyer} instance.
     * 
     * @return The new {@link DefaultProcessDestroyer}.
     */
    public static DefaultProcessDestroyer newInstance() {
        return new DefaultProcessDestroyer();
    }

    @Override
    public void destroy(RunningTestProcess<?> runningTestProcess) {
        TestProcessDefinition processDefinition = runningTestProcess.getDefinition();
        Process managedProcess = ProcessUtils.retrieveManagedProcess(processDefinition);

        // Try soft-destroying
        if (recursive) {
            LOG.debug("Soft-destroying process " + processDefinition.getProcessIdentifier()
                + " and its children recursively");
            destroyRecursive(managedProcess.toHandle(), this::destroySoft);
        } else {
            LOG.debug("Soft-destroying process " + processDefinition.getProcessIdentifier());
            destroySoft(managedProcess.toHandle());
        }

        // Check if soft-destruction worked
        try {
            softKillEventDetector.waitForEvent(runningTestProcess);
        } catch (TimeoutException e) {
            // Timeout. Bring out the big guns
            if (recursive) {
                LOG.debug("Forcefully destroying process " + processDefinition.getProcessIdentifier()
                    + " and its children recursively");
                destroyRecursive(managedProcess.toHandle(), this::destroyForcibly);
            } else {
                LOG.debug("Forcefully destroying process " + processDefinition.getProcessIdentifier());
                destroyForcibly(managedProcess.toHandle());
            }
        }
    }

    private void destroyRecursive(ProcessHandle process, Consumer<ProcessHandle> destroyMethod) {
        Stream<ProcessHandle> children = process.children();
        destroyMethod.accept(process);
        children.forEach(child -> destroyRecursive(child, destroyMethod));
    }

    private void destroySoft(ProcessHandle process) {
        if (process.isAlive()) {
            process.destroy();
        }
    }

    private void destroyForcibly(ProcessHandle process) {
        if (process.isAlive()) {
            process.destroyForcibly();
        }
    }

    /**
     * Sets the {@link EventDetector} used to determine whether soft-killing the
     * process has worked.
     * 
     * @param eventDetector
     *            The eventDetector.
     * @return This {@link ProcessDestroyer}.
     */
    public DefaultProcessDestroyer withSoftKillEventDetector(EventDetector eventDetector) {
        this.softKillEventDetector = eventDetector;
        return this;
    }

    /**
     * Sets whether processes should be destroyed recursively.
     * 
     * @param destroyRecursively
     *            Whether or not processes should be destroyed recursively.
     * @return This {@link ProcessDestroyer}.
     */
    public DefaultProcessDestroyer recursive(boolean destroyRecursively) {
        this.recursive = destroyRecursively;
        return this;
    }
}
