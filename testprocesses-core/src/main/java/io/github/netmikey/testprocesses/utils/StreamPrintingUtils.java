package io.github.netmikey.testprocesses.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.channels.Channels;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.ClassUtils;

import io.github.netmikey.testprocesses.FileBackedOutErrStreams;
import io.github.netmikey.testprocesses.RunningTestProcess;
import io.github.netmikey.testprocesses.TestProcessDefinition;

/**
 * Utilities for reading and printing stdErr / stdOut streams.
 */
public class StreamPrintingUtils {

    /**
     * Prints the <code>stdOut</code> and <code>stdErr</code> streams of the
     * process with the specified {@link TestProcessDefinition} to the unit
     * test's <code>stdOut</code> stream.
     * <p>
     * Note that the targeted {@link TestProcessDefinition} must implement
     * {@link FileBackedOutErrStreams} and the streams must not be empty for
     * this method to print something.
     * 
     * @param runningTestProcess
     *            The test process who'se streams should be logged.
     * @param streamStart
     *            Where the returned part of the stream should start.
     */
    public static void printOutAndErrStreams(RunningTestProcess<?> runningTestProcess, StreamStart streamStart) {
        printOutAndErrStreams(runningTestProcess, streamStart, System.out);
    }

    /**
     * Prints the <code>stdOut</code> and <code>stdErr</code> streams of the
     * {@link RunningTestProcess} to the specified {@link PrintStream}.
     * <p>
     * Note that the targeted {@link TestProcessDefinition} must implement
     * {@link FileBackedOutErrStreams} and the streams must not be empty for
     * this method to print something.
     * 
     * @param runningTestProcess
     *            The test process who'se streams should be logged.
     * @param streamStart
     *            Where the returned part of the stream should start.
     * @param outStream
     *            The PrintStream to output the resulting stream to.
     */
    public static void printOutAndErrStreams(RunningTestProcess<?> runningTestProcess, StreamStart streamStart,
        PrintStream outStream) {

        TestProcessDefinition testProcessDefinition = runningTestProcess.getDefinition();
        if (testProcessDefinition instanceof FileBackedOutErrStreams) {
            String processIdentifier = testProcessDefinition.getProcessIdentifier();
            printStream(processIdentifier, "stdOut", stdOutStreamOf(runningTestProcess, streamStart), outStream);
            printStream(processIdentifier, "stdErr", stdErrStreamOf(runningTestProcess, streamStart), outStream);
        }
    }

    private static void printStream(String processIdentifier, String streamName, InputStream processStream,
        PrintStream outStream) {

        try (var reader = new BufferedReader(new InputStreamReader(processStream))) {
            if (processStream.available() > 0) {
                String title = " %s %s ".formatted(ClassUtils.getAbbreviatedName(processIdentifier, 59), streamName);
                String headerRow = "/" + "-".repeat(5) +
                    title + "-".repeat(80 - 6 - title.length());

                outStream.println("\n" + headerRow);

                reader
                    .lines()
                    .map(line -> "  " + line)
                    .forEach(System.out::println);

                outStream.println("\\" + "-".repeat(headerRow.length() - 1));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the specified part of the referenced test process'
     * <code>stdOut</code> stream.
     * 
     * @param runningTestProcess
     *            The test process who'se stream should be returned.
     * @param streamStart
     *            Where the returned part of the stream should start.
     * @return The specified part of the <code>stdOut</code> stream.
     */
    public static InputStream stdOutStreamOf(RunningTestProcess<?> runningTestProcess, StreamStart streamStart) {
        return streamOf(runningTestProcess, streamStart, "stdOut stream",
            FileBackedOutErrStreams::getOutFile,
            RunningTestProcess::getCurrentTestStdOutStart);
    }

    /**
     * Returns the specified part of the referenced test process'
     * <code>stdErr</code> stream.
     * 
     * @param runningTestProcess
     *            The test process who'se stream should be returned.
     * @param streamStart
     *            Where the returned part of the stream should start.
     * @return The specified part of the <code>stdErr</code> stream.
     */
    public static InputStream stdErrStreamOf(RunningTestProcess<?> runningTestProcess, StreamStart streamStart) {
        return streamOf(runningTestProcess, streamStart, "stdErr stream",
            FileBackedOutErrStreams::getErrFile,
            RunningTestProcess::getCurrentTestStdErrStart);
    }

    private static InputStream streamOf(RunningTestProcess<?> runningTestProcess, StreamStart streamStart,
        String streamDescription, Function<FileBackedOutErrStreams, Optional<Path>> streamFileRetriever,
        Function<RunningTestProcess<?>, Optional<Long>> streamPositionRetriever) {

        if (!(runningTestProcess.getDefinition() instanceof FileBackedOutErrStreams)) {
            throw new IllegalStateException("TestProcessDefinition class must implement "
                + FileBackedOutErrStreams.class + " in order to access its streams");
        }
        Path streamFile = streamFileRetriever.apply((FileBackedOutErrStreams) runningTestProcess.getDefinition())
            .orElseThrow(() -> new IllegalStateException(
                "Test process " + runningTestProcess.getDefinition().getProcessIdentifier()
                    + " defined in bean " + runningTestProcess.getDefinition()
                    + " seems to be running but does not have an " + streamDescription
                    + " assigned to it yet. This shouldn't happen."));

        Optional<Long> streamFileStartPositionOfCurrentTest = streamPositionRetriever.apply(runningTestProcess);

        try {
            /*
             * Open new channel, seek to the position in it where the current
             * test started, and return a new InputStream from this position to
             * the end of the file.
             */
            SeekableByteChannel streamFileChannel = Files.newByteChannel(streamFile, StandardOpenOption.READ);
            if (StreamStart.CURRENT_TEST.equals(streamStart)
                && streamFileStartPositionOfCurrentTest.isPresent()) {

                streamFileChannel.position(streamFileStartPositionOfCurrentTest.get());
            }
            return Channels.newInputStream(streamFileChannel);
        } catch (IOException e) {
            throw new UncheckedIOException("Error opening new InputStream to process' "
                + streamDescription + ": " + e.getMessage(), e);
        }
    }
}
