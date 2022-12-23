package io.github.netmikey.testprocesses.eventdetector;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import io.github.netmikey.testprocesses.RunningTestProcess;

/**
 * An {@link EventDetector} that detects when a given TCP network port opens /
 * is able to connect.
 * <p>
 * In order to detect its availability, it will have to connect to the port and
 * immediately disconnect once the connection succeeds. This might introduce
 * side effects on the application that opens the port.
 */
public class TcpPortEventDetector extends AbstractEventDetector<TcpPortEventDetector> implements EventDetector {

    private int port;

    private String host;

    private boolean targetIsOpen = true;

    /**
     * Create a new {@link TcpPortEventDetector} for the specified local port.
     * 
     * @param port
     *            The target port.
     * @return A new {@link TcpPortEventDetector}.
     */
    public static TcpPortEventDetector forLocalPort(int port) {
        return forPort("localhost", port);
    }

    /**
     * Create a new {@link TcpPortEventDetector} for the specified port on the
     * specified host.
     * 
     * @param host
     *            The target hostname or IP.
     * @param port
     *            The target port.
     * @return A new {@link TcpPortEventDetector}.
     */
    public static TcpPortEventDetector forPort(String host, int port) {
        TcpPortEventDetector newInstance = new TcpPortEventDetector();
        newInstance.host = host;
        newInstance.port = port;
        return newInstance;
    }

    /**
     * Set the {@link EventDetector} to trigger when the port becomes available.
     * 
     * @return This {@link TcpPortEventDetector}.
     */
    public TcpPortEventDetector detectOpenPort() {
        this.targetIsOpen = true;
        return this;
    }

    /**
     * Invert the functioning of this {@link EventDetector}: trigger when the
     * port becomes unavailable rather then when it becomes available.
     * 
     * @return This {@link TcpPortEventDetector}.
     */
    public TcpPortEventDetector detectClosedPort() {
        this.targetIsOpen = false;
        return this;
    }

    @Override
    public void waitForEvent(RunningTestProcess<?> runningTestProcess) throws TimeoutException {
        long start = System.currentTimeMillis();
        while (targetIsOpen != isPortOpen()) {
            checkRunningTimeoutAndSleep(runningTestProcess, start,
                () -> "waiting for port " + host + ":" + port + " to become available");
        }
    }

    private boolean isPortOpen() throws IllegalStateException {
        try (Socket ignored = new Socket(host, port)) {
            return true;
        } catch (ConnectException e) {
            return false;
        } catch (IOException e) {
            throw new UncheckedIOException("Error while trying to check open port: " + e.getMessage(), e);
        }
    }
}
