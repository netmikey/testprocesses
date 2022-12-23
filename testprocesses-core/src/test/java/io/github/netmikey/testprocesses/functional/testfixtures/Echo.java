package io.github.netmikey.testprocesses.functional.testfixtures;

import java.io.IOException;

/**
 * A Java process used for testing that simply echoes whatever it gets on its
 * stdIn stream to its stdOut stream.
 */
public class Echo {
    /**
     * The main method.
     * 
     * @param args
     *            CLI Arguments.
     * @throws IOException
     *             If transfering stdIn to stdOut fails.
     */
    public static void main(String[] args) throws IOException {
        String greeting = (args != null && args.length > 0) ? args[0] + ", " : "";
        System.out.println("+++ " + greeting + "Echo process running...");
        System.in.transferTo(System.out);
        System.out.println("+++ Echo process exiting.");
    }
}
