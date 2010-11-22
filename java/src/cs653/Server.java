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
 * @author rmanders
 */
public class Server implements Runnable {

    protected int serverPort;
    protected Thread runner;
    protected ServerSocket socServer = null;
    protected String ident;
    protected String password;

    private final Logger logger = Logger.getLogger(Server.class);

    public Server( int serverPort, String ident, String password ) {
        logger.debug("Instancing local Server on port: " + serverPort);
        this.serverPort = serverPort;
        this.ident = ident;
        this.password = password;
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
                Socket connection = socServer.accept();
                new ServerThread(connection, threadId, ident)
                        .startServertThread();
            }
        } catch (Exception ex ) {

        }
        logger.debug("Terminating Server");
    }

}
