/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.net.Socket;
import org.apache.log4j.Logger;

/**
 *
 * @author Ryan Anderson
 *
 * A server thread that handles an individual incoming connection
 *
 *
 */
public class ServerThread extends CommandInterpreter implements Runnable {

    private int threadId;
    private final Logger logger;
    private Thread runner;

    // <editor-fold defaultstate="collapsed" desc="Constructor">
    public ServerThread(Socket socConnection, int threadId, String identity) {
        super(identity);
        logger = Logger.getLogger("ServerThread ID[" + threadId + "]");
        this.threadId = threadId;
        this.socConnection = socConnection;
        if (!initConnectionIO()) {
            logger.error("FATAL ERROR: Failed to open IO buffers for connection");
            System.exit(1);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="startServerThread">
    /**
     * Starts the ServerThread execution
     */
    public void startServertThread() {
        if (null == runner) {
            runner = new Thread(this);
            runner.start();
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="doLoginHandshake">
    public boolean doLoginHandshake() {

        // Expect Ident directive
        MessageGroup msgs = receiveMessageGroup();
        Directive dir = msgs.getNext(Directive.REQUIRE);
        if (!dir.getArg().equals("IDENT")) {
            logger.error("Local Server handshake failed: Expected IDENT, got "
                    + dir.getArg());
            return false;
        }

        // Execute ident command
        boolean result = executeCommand(Command.IDENT);
        if (!result) {
            logger.error("In doLoginHandshake: Failed to execute IDENT command");
            return false;
        }

        // Expect Alive directive
        msgs = receiveMessageGroup();
        dir = msgs.getNext(Directive.REQUIRE);
        if (!dir.getArg().equals("ALIVE")) {
            logger.error("Local Server handshake failed: Expected ALIVE, got "
                    + dir.getArg());
            return false;
        }

        // Execute ALIVE command
        result = executeCommand(Command.ALIVE);
        if (!result) {
            logger.error("In doLoginHandshake: Failed to execute ALIVE command");
            return false;
        }

        // Expect QUIT
        msgs = receiveMessageGroup();
        dir = msgs.getNext(Directive.REQUIRE);
        if (!dir.getArg().equals("QUIT")) {
            logger.error("Local Server handshake failed: Expected QUIT, got "
                    + dir.getArg());
            return false;
        }

        // Execute QUIT command
        result = executeCommand(Command.QUIT);
        if (!result) {
            logger.error("In doLoginHandshake: Failed to execute QUIT command");
            return false;
        }

        return true;
    }
    // </editor-fold>

    public void run() {

        // TODO: Execute login protocol
        if( !doLoginHandshake() ) {
            logger.error("ServerThread Login handshake FAILED");
        }
        logger.debug("Server Thread Login handshake SUCCEEDED");
        
        // TODO: Find out exactly how to terminate threads properly
        //while( Thread.currentThread() == this.runner ) {
            
        //}
        logger.debug("Exiting Local Server thread");
    }

}
