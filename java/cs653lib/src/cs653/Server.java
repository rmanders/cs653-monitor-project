/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import org.apache.log4j.Logger;

/**
 *
 * @author Ryan Anderson
 */
public class Server implements Runnable {

    protected final int serverPort;
    protected Thread runner;
    protected ServerSocket socServer = null;
    protected ConfigData config = null;

    private final Logger logger = Logger.getLogger(Server.class);

    public Server( ConfigData config ) {
        this.config = config;
        this.serverPort = Integer.
                parseInt(this.config.getProperty("serverPort"));
        logger.debug("Instancing local Server on port: " + this.serverPort);
        try {
            socServer = new ServerSocket( serverPort );
        } catch ( IOException ex ) {
            logger.error("Server failed to start: " + ex);
            System.exit(1);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="startServer">
    public void startServer() {
        logger.debug("Local Server is starting up...");
        if (null == runner) {
            runner = new Thread(this);
            runner.start();
        }
        logger.debug("Local Server successfully started and listening");
    }
    // </editor-fold>

    public void run() {
        try {
            int threadId = 0;
            while( true ) {

                // Refresh config file
                config.load();

                // CHANGE PORTS
                String newPortStr = config.getProperty("serverPort");
                String ident = config.getProperty("identity");

                int newPort = -1;
                try {
                    newPort = Integer.parseInt(newPortStr);
                } catch (NumberFormatException ex) {
                    newPort = -1;
                }
                if( newPort == -1 || newPort < 2048 || newPort > 65000) {
                    logger.error("Tried to change port but failed: port " + newPort);
                } else {
                    socServer.close();
                    socServer = new ServerSocket( newPort );
                    logger.info("[" + ident + "] REALIGNED PORT: " + newPort);
                }

                // Proceed as Normal
                Socket connection = socServer.accept();

                logger.info("Got [HOST_CONNECTION] from "
                        + connection.getRemoteSocketAddress() );

                new ServerThread(connection, threadId, config)
                        .startServertThread();
            }
        } catch (Exception ex ) {
            runner = null;
            logger.error("Server FATAL Error: " + ex);
        }
        logger.debug("Terminating Server");
    }

}
