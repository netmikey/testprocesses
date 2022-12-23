package io.github.netmikey.testprocesses;

/**
 * Defines how TestProcesses should handle already-running processes.
 */
public enum StartStrategy {
    /**
     * If a process for the specified {@link TestProcessDefinition} is already
     * running, don't do anything and re-use it. If not, start it.
     */
    USE_EXISTING,

    /**
     * If a process for the specified {@link TestProcessDefinition} is already
     * running, stop it first. Always start a fresh process.
     */
    REQUIRE_RESTART;
}
