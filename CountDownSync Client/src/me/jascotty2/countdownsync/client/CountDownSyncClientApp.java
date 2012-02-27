/*
 * CountDownSyncClientApp.java
 */

package me.jascotty2.countdownsync.client;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class CountDownSyncClientApp extends SingleFrameApplication {

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {
        show(new CountDownSyncClientView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of CountDownSyncClientApp
     */
    public static CountDownSyncClientApp getApplication() {
        return Application.getInstance(CountDownSyncClientApp.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
//		Settings.extractResource("lib/appframework-1.0.3.jar", new File("lib/appframework-1.0.3.jar"));
//		Settings.extractResource("lib/swing-worker-1.1.jar", new File("lib/swing-worker-1.1.jar"));
        launch(CountDownSyncClientApp.class, args);
    }
}
