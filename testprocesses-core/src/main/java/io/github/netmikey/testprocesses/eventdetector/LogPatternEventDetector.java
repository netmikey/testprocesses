package io.github.netmikey.testprocesses.eventdetector;

import java.nio.file.Path;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

import io.github.netmikey.testprocesses.AbstractTestProcessDefinition;
import io.github.netmikey.testprocesses.FileBackedOutErrStreams;
import io.github.netmikey.testprocesses.RunningTestProcess;
import io.github.netmikey.testprocesses.TestProcessDefinition;
import io.github.netmikey.testprocesses.utils.MutableObject;
import io.github.netmikey.testprocesses.utils.StreamStart;
import io.github.netmikey.testprocesses.utils.Tailer;
import io.github.netmikey.testprocesses.utils.TailerListener;
import io.github.netmikey.testprocesses.utils.TailerListenerAdapter;

/**
 * Watches a log file line by line for a specific pattern (plain string marker
 * or RegEx pattern) to detect an event. Patterns must be contained within a
 * single line to be detected.
 */
public class LogPatternEventDetector extends AbstractEventDetector<LogPatternEventDetector> implements EventDetector {

    private Target target;

    private Path targetFile;

    private String marker;

    private Pattern pattern;

    private StreamStart lookFrom = StreamStart.ABSOLUTE;

    /**
     * Create a new {@link LogPatternEventDetector} on the specified file.
     * 
     * @param file
     *            The file to look into.
     * @return The new {@link LogPatternEventDetector}.
     */
    public static LogPatternEventDetector onFile(Path file) {
        LogPatternEventDetector newDetector = new LogPatternEventDetector();
        newDetector.target = Target.arbitraryFile;
        newDetector.targetFile = file;
        return newDetector;
    }

    /**
     * Create a new {@link LogPatternEventDetector} on the process' output
     * stream.
     * 
     * @return The new {@link LogPatternEventDetector}.
     */
    public static LogPatternEventDetector onStdOut() {
        return onStdOut(StreamStart.CURRENT_TEST);
    }

    /**
     * Create a new {@link LogPatternEventDetector} on the process' output
     * stream.
     * 
     * @param lookFrom
     *            Specify where in the stdOut stream we should start looking for
     *            the pattern.
     * @return The new {@link LogPatternEventDetector}.
     */
    public static LogPatternEventDetector onStdOut(StreamStart lookFrom) {
        LogPatternEventDetector newDetector = new LogPatternEventDetector();
        newDetector.target = Target.stdOut;
        newDetector.lookFrom = lookFrom;
        return newDetector;
    }

    /**
     * Create a new {@link LogPatternEventDetector} on the process' error
     * stream.
     *
     * @return The new {@link LogPatternEventDetector}.
     */
    public static LogPatternEventDetector onStdErr() {
        return onStdErr(StreamStart.CURRENT_TEST);
    }

    /**
     * Create a new {@link LogPatternEventDetector} on the process' error
     * stream.
     *
     * @param lookFrom
     *            Specify where in the stdOut stream we should start looking for
     *            the pattern.
     * @return The new {@link LogPatternEventDetector}.
     */
    public static LogPatternEventDetector onStdErr(StreamStart lookFrom) {
        LogPatternEventDetector newDetector = new LogPatternEventDetector();
        newDetector.target = Target.stdErr;
        newDetector.lookFrom = lookFrom;
        return newDetector;
    }

    /**
     * Look for the specified regex pattern.
     * 
     * @param regexPattern
     *            The pattern to look for.
     * @return This {@link LogPatternEventDetector}.
     */
    public LogPatternEventDetector withPattern(String regexPattern) {
        return withPattern(Pattern.compile(regexPattern));
    }

    /**
     * Look for the specified regex pattern.
     * 
     * @param regexPattern
     *            The pattern to look for.
     * @return This {@link LogPatternEventDetector}.
     */
    public LogPatternEventDetector withPattern(Pattern regexPattern) {
        this.pattern = regexPattern;
        this.marker = null;
        return this;
    }

    /**
     * Look for the specified string marker.
     * 
     * @param markerString
     *            The marker to look for.
     * @return This {@link LogPatternEventDetector}.
     */
    public LogPatternEventDetector withMarker(String markerString) {
        this.pattern = null;
        this.marker = markerString;
        return this;
    }

    /**
     * When using stdOut or stdErr streams, specify where this
     * {@link EventDetector} should start looking within those streams.
     * 
     * @param startLooking
     *            The strategy to be used.
     * @return This {@link LogPatternEventDetector}.
     */
    public LogPatternEventDetector lookFrom(StreamStart startLooking) {
        this.lookFrom = startLooking;
        return this;
    }

    @Override
    public void waitForEvent(RunningTestProcess<?> runningProcess) throws TimeoutException {

        Path file = findEffectiveFile(runningProcess.getDefinition());

        long startMillis = System.currentTimeMillis();

        MutableObject<Boolean> found = new MutableObject<>(false);

        TailerListener tailerListener = new TailerListenerAdapter() {
            @Override
            public void handle(String line) {
                if (containsMarkerOrPattern(line)) {
                    found.setValue(true);
                }
            }
        };

        long startPosition = findEffectiveStartPosition(runningProcess);

        Tailer tailer = Tailer.create(file.toFile(), tailerListener, 500, startPosition);

        try {
            while (!found.getValue()) {
                checkRunningTimeoutAndSleep(runningProcess, startMillis, () -> timeoutMessage(runningProcess));
            }
        } finally {
            tailer.stop();
        }
    }

    private String timeoutMessage(RunningTestProcess<?> runningProcess) {
        StringBuffer result = new StringBuffer("looking for ");

        if (marker != null) {
            result.append("marker string '" + marker + "' ");
        }
        if (pattern != null) {
            result.append("regex pattern '" + pattern + "' ");
        }

        switch (target) {
            case arbitraryFile:
                result.append("in file " + targetFile + " ");
                break;
            case stdOut:
                result.append("in '" + runningProcess.getDefinition().getProcessIdentifier()
                    + "' process' stdOut stream ");
                break;
            case stdErr:
                result.append("in '" + runningProcess.getDefinition().getProcessIdentifier()
                    + "' process' stdErr stream ");
                break;
            default:
                throw new IllegalStateException("Should never happen");
        }
        return result.toString();
    }

    private long findEffectiveStartPosition(RunningTestProcess<?> runningTestProcess) {
        long result;
        if (StreamStart.CURRENT_TEST.equals(lookFrom)) {
            switch (target) {
                case stdOut:
                    result = runningTestProcess.getCurrentTestStdOutStart().orElse(0L);
                    break;
                case stdErr:
                    result = runningTestProcess.getCurrentTestStdErrStart().orElse(0L);
                    break;
                case arbitraryFile:
                default:
                    result = 0;
            }
        } else {
            result = 0;
        }
        return result;
    }

    private Path findEffectiveFile(TestProcessDefinition process) {
        Path file = null;

        switch (target) {
            case arbitraryFile:
                file = targetFile;
                break;
            case stdOut:
            case stdErr:
                file = getStdFile(process);
                break;
            default:
                throw new IllegalStateException("Should never happen");
        }
        if (file == null) {
            throw new IllegalStateException("No target file specified or obtainable.");
        }
        return file;
    }

    private Path getStdFile(TestProcessDefinition process) {
        Path result = null;
        if (process instanceof FileBackedOutErrStreams) {
            if (Target.stdOut.equals(target)) {
                result = ((AbstractTestProcessDefinition) process).getOutFile().orElse(null);
            }
            if (Target.stdErr.equals(target)) {
                result = ((AbstractTestProcessDefinition) process).getErrFile().orElse(null);
            }
        } else {
            throw new IllegalStateException("Can only retrieve stdOut / stdErr from classes implementing "
                + FileBackedOutErrStreams.class);
        }
        return result;
    }

    private boolean containsMarkerOrPattern(String line) {
        return (marker != null && line.contains(marker))
            || (pattern != null && pattern.matcher(line).find());
    }

    private static enum Target {
        arbitraryFile, stdOut, stdErr;
    }
}
