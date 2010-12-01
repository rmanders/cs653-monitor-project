/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import cs653.security.DiffieHellmanExchange;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Random;

/**
 *
 * @author Ryan Anderson
 */
public class ActiveClient extends CommandInterpreter implements Runnable
{
    private Thread runner = null;

    public ActiveClient( String monitorHost, int monitorPort, 
            String serverHostname, int serverPort,
            String identity, String password ) {
        super( monitorHost, monitorPort, serverHostname, serverPort,
                identity, password, Logger.getLogger(ActiveClient.class));
    }

    public ActiveClient( String configfile ) {
        super(configfile, Logger.getLogger(ActiveClient.class));
    }

    public ActiveClient( ConfigData config ) {
        super(config, Logger.getLogger(ActiveClient.class));
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
                String monitorHostname = CONFIG.getProperty("monitorHostname");
                int monitorPort = Integer
                        .parseInt(CONFIG.getProperty("monitorPort"));
                socConnection = new Socket(monitorHostname,monitorPort);
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

                // Store and save the cookie
                if( CONFIG.hasProperty("cookie")) {
                    CONFIG.setProperty("cookie", dir.getArg(1));
                } else {
                    CONFIG.addProperty("cookie", dir.getArg(1));
                }
                CONFIG.save();
                
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

    public boolean doTransfer(String to, String from, int amount ) {
        logger.info("Initiating transfer protocol[ from "
                + from + " to " + to + " amount: " + amount + "]");
        Random rand = new Random();

        boolean result = executeCommand(Command.TRANSFER_REQUEST,
                to, Integer.toString(amount), "FROM", from );
        if(!result) {
            logger.error("Failed to execute command: "
                    + Command.TRANSFER_REQUEST);
            return false;
        }

        MessageGroup msgs = receiveMessageGroup();
        System.out.println(msgs);

        // Execute Public Key
        BigInteger p = BigInteger.probablePrime(64, rand);
        BigInteger g = BigInteger.probablePrime(64, rand);
        result = executeCommand(Command.PUBLIC_KEY,
                p.toString(), g.toString() );
        if(!result) {
            logger.error("Failed to execute command: "
                    + Command.PUBLIC_KEY);
            return false;
        }

        // Expect REQUIRE Authorize set
        msgs = receiveMessageGroup();
        Directive dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE,"AUTHORIZE_SET");
        if(!checkDirective(dir,DirectiveType.REQUIRE,"AUTHORIZE_SET")) {
            return false;
        }

        // Calcuate athorize set
        String fodder = "7863687 6129879 182987 2131908 289746 12097 03947 17364 8763 846";

        // Execute Authorize Set
        result = executeCommand(Command.AUTHORIZE_SET, fodder);
        if(!result) {
            logger.error("Failed to execute command: " + Command.AUTHORIZE_SET);
            return false;
        }

        // Expect SUBSET_A
        msgs = receiveMessageGroup();
        dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "SUBSET_A");
        if(!checkDirective(dir,DirectiveType.RESULT,"SUBSET_A")) {
            return false;
        }

        // Compute Subset_K
        Integer subA[] = dir.getArgIntArray(1);
        String subStr = dir.getArg(1);
        if(null == subA) {
            logger.error("Error getting SUBSET_A arguments from: " + dir);
            return false;
        }

        //Expect REQUIRE SUBSET_K
        dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE, "SUBSET_K");
        if(!checkDirective(dir,DirectiveType.REQUIRE,"SUBSET_K")) {
            return false;
        }


        // Execute SubsetK
        result = executeCommand(Command.SUBSET_K, subStr);
        if(!result) {
            logger.error("Failed to execute command: " + Command.SUBSET_K);
            return false;
        }

        // Expect Require SUBSET_J
        msgs = receiveMessageGroup();
        dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE, "SUBSET_J");
        if(!checkDirective(dir,DirectiveType.REQUIRE,"SUBSET_J")) {
            return false;
        }

        // Calculate Subset J
        int i = (10 - subA.length);
        fodder = "";
        for( int j=0; j<i; j++) {
            fodder += " " + rand.nextInt(1024);
        }

        // Unlock the lockfile
        logger.info("LOCKFILE unlock success?: " + setLockYes());
        
        // Execute Subset J
        result = executeCommand(Command.SUBSET_J, fodder);
        if(!result) {
            logger.error("Failed to execute command: " + Command.SUBSET_J);
            return false;
        }

        // Expect Result TRANSFER_RESPONSE
        msgs = receiveMessageGroup();
        dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "TRANSFER_RESPONSE");
        if(!checkDirective(dir,DirectiveType.RESULT,"TRANSFER_RESPONSE")) {
            return false;
        }

        logger.info(dir);
        return true;
    }

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
