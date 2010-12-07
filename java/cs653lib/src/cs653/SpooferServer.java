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
public class SpooferServer implements Runnable {

    protected final int serverPort;
    protected Thread runner;
    protected ServerSocket socServer = null;
    protected ConfigData config = null;
    protected boolean run = true;

    private final Logger logger = Logger.getLogger(SpooferServer.class);

    public SpooferServer( ConfigData config ) {
        this.config = config;
        this.serverPort = Integer.
                parseInt(this.config.getProperty("serverPort"));
        logger.debug("Instancing local Server on port: " + this.serverPort);
        try {
            socServer = new ServerSocket( serverPort );
        } catch ( IOException ex ) {
            logger.error("FATAL: Failed to create new Server Socket: " + ex);
            System.exit(1);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="startServer">
    public void startServer() {
        logger.debug("Spoofer Server is starting up...");
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
            socServer.setSoTimeout(60000);

            while( true ) {

                // Listen for a connection
                Socket connection = socServer.accept();
                
                if(!run) {
                    break;
                }

                // Refresh config file
                config.load();

                // Check for emergency shutdown
                if(!panicCheck()) {
                    logger.error("EMERGENCY SERVER SHUTDOWN!!!!");
                    break;
                }

                logger.info("Got [HOST_CONNECTION] from "
                        + connection.getRemoteSocketAddress() );

                // Run the spooerthread but NOT as a separate thread
                SpooferThread spoofer = new SpooferThread(connection, threadId++, config);

                // Some Blocking may ocurr
                try
                {
                    spoofer.run();

                    if( spoofer.hasTransferred()) {
                        break;
                    }
                } catch (Exception ex ) {
                    logger.error("SPOOFING SERVER FAILED!!! QUITTING..." + ex);
                    break;
                }
            }
        } catch (Exception ex ) {
            runner = null;
            logger.error("Server FATAL Error: " + ex);
        }
        logger.debug("Terminating Server");
    }

    public void kill() {
        this.run = false;
    }

    protected boolean panicCheck() {
        if( null != config ) {
            if( config.hasProperty("die")
                    && config.getProperty("die").equals("True")) {
                return false;
            }
        }
        return true;
    }
}
