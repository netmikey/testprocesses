package io.github.netmikey.testprocesses.utils;

/**
 * Interface has been mirrored from <code>org.apache.commons.io</code> to avoid
 * additional dependency for very limited use.
 * <p>
 * Listener for events from a {@link Tailer}.
 */
public interface TailerListener {

    /**
     * The tailer will call this method during construction, giving the listener
     * a method of stopping the tailer.
     * 
     * @param tailer
     *            the tailer.
     */
    void init(Tailer tailer);

    /**
     * This method is called if the tailed file is not found.
     * <p>
     * <b>Note:</b> this is called from the tailer thread.
     */
    void fileNotFound();

    /**
     * Called if a file rotation is detected.
     *
     * This method is called before the file is reopened, and fileNotFound may
     * be called if the new file has not yet been created.
     * <p>
     * <b>Note:</b> this is called from the tailer thread.
     */
    void fileRotated();

    /**
     * Handles a line from a Tailer.
     * <p>
     * <b>Note:</b> this is called from the tailer thread.
     * 
     * @param line
     *            the line.
     */
    void handle(String line);

    /**
     * Handles an Exception .
     * <p>
     * <b>Note:</b> this is called from the tailer thread.
     * 
     * @param ex
     *            the exception.
     */
    void handle(Exception ex);

}