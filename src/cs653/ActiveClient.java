/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;

/**
 *
 * @author Ryan Anderson
 */
public class ActiveClient extends CommandInterpreter implements Runnable
{
    private final Logger logger =
            Logger.getLogger(ActiveClient.class);
    private Thread thread = null;

    public ActiveClient( String monitorHost, int monitorPort,
            String identity, String password ) {
        super( monitorHost, monitorPort, identity, password );
    }

    // <editor-fold defaultstate="collapsed" desc="launch">
    public void launch() {
        if (null == thread) {
            thread = new Thread(this);
        }
        thread.start();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="run">
    public void run() {

        logger.debug("Entering run function");

        while( Thread.currentThread() == thread ) {

            logger.debug("Opening Client connection to Monitor");
            openConnection(MONITORHOST, MONITORPORT);

            logger.debug("Calling client login function");
            boolean result = login();

            logger.debug("Login Result: " + result);

            logger.debug("Closing client connection");
            closeConnection();
            break;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="login">
    private boolean login() {
        MessageGroup msgs;
        Directive dir;
        boolean result;
        try {

            // Send Identity
            msgs = receiveMessageGroup();
            dir = msgs.getNext(Directive.REQUIRE);

            if (!dir.getArg().equals("IDENT")) {
                logger.error("Login failed: Expected IDENT, got: "
                        + dir.getArg());
                return false;
            }
            result = executeCommand(Command.IDENT);
            if (!result) {
                logger.error("Failed to execute IDENT command.");
                return false;
            }

            // Send Password or alive
            msgs = receiveMessageGroup();
            dir = msgs.getNext(Directive.REQUIRE);

            if (dir.getArg().equals("PASSWORD")) {
                result = executeCommand(Command.PASSWORD);
                if (!result) {
                    logger.error("Failed to execute PASSWORD command");
                    return false;
                }
                // Expect cookie
                msgs = receiveMessageGroup();
                dir = msgs.getNext(Directive.RESULT);
                Command cmd = Command.valueOf(dir.getArg());
                if (cmd != Command.PASSWORD) {
                    logger.error("Login failed: expected password result, got: "
                            + dir);
                }
                COOKIE = dir.getPayload();
                // TODO: Save ident and cookie
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
            dir = msgs.getNext(Directive.REQUIRE);
            if (dir.getArg().equals("HOST_PORT")) {
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
}
