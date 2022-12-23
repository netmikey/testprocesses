package io.github.netmikey.testprocesses.utils;

/**
 * Allows to specify where stdOut / stdErr streams should start.
 */
public enum StreamStart {
    /**
     * The stream should start at the very beginning (entire stream).
     */
    ABSOLUTE,

    /**
     * The stream should start at the beginning of the current test method. Only
     * the part of the stream written during the current test method will be
     * considered.
     */
    CURRENT_TEST;
}
