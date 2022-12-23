package io.github.netmikey.testprocesses.functional.testfixtures;

import java.io.IOException;

/**
 * A Java process used for testing that sleeps forever and logs every couple of
 * seconds.
 */
public class Sleeper {
    /**
     * The main method.
     * 
     * @param args
     *            CLI Arguments.
     * @throws IOException
     *             If transfering stdIn to stdOut fails.
     */
    public static void main(String[] args) throws IOException {
        System.out.println("+++ " + Sleeper.class.getSimpleName() + " process running...");
        int i = 0;
        while (true) {
            try {
                Thread.sleep(10000);
                System.out.println("+++ Woke up for the " + i++ + " time! *yawn* let's go to sleep again...");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}