/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import cs653.security.DiffieHellmanExchange;
import java.net.Socket;

/**
 *
 * @author Ryan Anderson
 */
public class ActiveClient extends CommandInterpreter implements Runnable
{
    private final Logger logger = Logger.getLogger(ActiveClient.class);
    private Thread runner = null;

    public ActiveClient( String monitorHost, int monitorPort, int hostPort,
            String identity, String password ) {
        super( monitorHost, monitorPort, hostPort, identity, password );

        //TODO: Remove this from hard code
        HOSTNAME = "localhost";

    }

    // <editor-fold defaultstate="collapsed" desc="launch">
    public void launch() {
        if (null == runner) {
            runner = new Thread(this);
        }
        runner.start();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="run">
    public void run() {

        logger.debug("Entering Active Client run function");

        while( Thread.currentThread() == runner ) {

            try {
                logger.debug("Opening Client connection to Monitor");
                socConnection = new Socket(MONITORHOST, MONITORPORT);
                initConnectionIO();

                logger.debug("Calling client login function");
                boolean result = login();

                logger.debug("Login Result: " + result);

                logger.debug("Closing client connection");
                socConnection.close();

                runner = null;
            } catch (Exception ex ) {
                logger.error("FATAL ERROR: " + ex);
            }
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="login">
    public boolean login() {
        MessageGroup msgs;
        Directive dir;
        boolean result;
        try {

            // Expect a request for IDENT
            msgs = receiveMessageGroup();
            dir = msgs.getNext(DirectiveType.REQUIRE);

            if (!dir.getArg().equals("IDENT")) {
                logger.error("Login failed: Expected IDENT, got: "
                        + dir.getArg());
                return false;
            }

            // Setup Encryption if it's turned on and execute the IDENT command
            if( ENCRYPTION_ON ) {
                dhe = DiffieHellmanExchange.getInstance();
                String myPublicKey = dhe.getPublicKey().toString(32);
                result = executeCommand(Command.IDENT, identity, myPublicKey);
            } else {
                result = executeCommand(Command.IDENT);
            }

            if (!result) {
                logger.error("Failed to execute IDENT command.");
                return false;
            }

            // Encryption should automatically be initialized by the message
            // receiving subsystem
            msgs = receiveMessageGroup();
            dir = msgs.getNext(DirectiveType.REQUIRE);

            if (dir.getArg().equals("PASSWORD")) {
                result = executeCommand(Command.PASSWORD);
                if (!result) {
                    logger.error("Failed to execute PASSWORD command");
                    return false;
                }
                // Expect cookie
                msgs = receiveMessageGroup();
                dir = msgs.getNext(DirectiveType.RESULT);
                Command cmd = Command.valueOf(dir.getArg());
                if (cmd != Command.PASSWORD) {
                    logger.error("Login failed: expected password result, got: "
                            + dir);
                }
                COOKIE = dir.getArg(1);
                this.saveConfig(PASSWORD, COOKIE);
            } else if (dir.getArg().equals("ALIVE")) {
                result = executeCommand(Command.ALIVE);
                if (!result) {
                    logger.error("Failed to execute ALIVE command");
                    return false;
                }
                msgs = receiveMessageGroup();
            } else {
                logger.error("Login failed: expected ALIVE or PASSWORD, got: "
                        + dir);
                return false;
            }

            // Handle Host port
            msgs.reset();
            dir = msgs.getNext(DirectiveType.REQUIRE);
            if (null != dir && dir.getArg().equals("HOST_PORT")) {
                result = executeCommand(Command.HOST_PORT);
                if (!result) {
                    logger.error("Failed to execute HOST_PORT command");
                    return false;
                }
            }

        } catch (NoSuchElementException e) {
            logger.error("Login failed. An expected directive was not "
                    + "found in the message group. Caused:" + e);
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkDirective(1)">
    private boolean checkDirective(Directive dir, DirectiveType expType,
            String expArg0) {
        if (null == dir) {
            logger.error("Expected a " + expType + " directive but none found "
                    + "in message group.");
            return false;
        }
        if (null != expArg0 && !dir.getArg().equals(expArg0)) {
            logger.error("Active Client handshake failed: Expected " + expArg0
                    + " got " + dir.getArg());
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkDirective(2)">
    private boolean checkDirective(Directive dir, DirectiveType expType) {
        return checkDirective(dir, expType, null);
    }
    // </editor-fold>
}
