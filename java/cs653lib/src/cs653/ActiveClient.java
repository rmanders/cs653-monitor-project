/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.NoSuchElementException;
import org.apache.log4j.Logger;
import cs653.security.DiffieHellmanExchange;
import cs653.security.RSAKeys;
import java.io.File;
import java.math.BigInteger;
import java.net.Socket;
import java.util.Arrays;
import java.util.Random;

/**
 *
 * @author Ryan Anderson
 *
 * Garden-variety active client. Use to connect to the monitor and issue
 * basic commands. Takes care of the more complicated protocols such as
 * logins and transfers.
 *
 */
public class ActiveClient extends CommandInterpreter implements Runnable
{
    /** Used for static method debugging **/
    private static final Logger lgrDebug = Logger.getLogger(ActiveClient.class);

    /** Used for squaring large integers **/
    private static final BigInteger TWO = new BigInteger("2",10);

    /** Used for running an active client thread ***/
    private Thread runner = null;

    /** Holds the current message group returned from the monitor **/
    protected MessageGroup msgs;

    /** Holds the current Directive being examined in the message group **/
    protected Directive dir;
    
    /** The client's secret key **/
    protected final BigInteger keyS;
    
    /** The client's public key modulus **/
    protected final BigInteger keyN;

    /** The client's public key v-value: keyV = keyS^2 mod keyN **/
    protected final BigInteger keyV;

    private ActiveClient( ConfigData config ) {
        super(config, Logger.getLogger(ActiveClient.class));
        this.keyS = new BigInteger(config.getProperty("keyS"),10);
        this.keyN = new BigInteger(config.getProperty("keyN"),10);
        this.keyV = new BigInteger(config.getProperty("keyV"),10);
    }

    // <editor-fold defaultstate="collapsed" desc="getInstance(1)">
    /**
     * Primary ActiveClient factory
     *
     * @param config {@link ConfigData} object
     *
     * @return
     */
    public static ActiveClient getInstance(ConfigData config) {
        if (!checkAndInitConfigData(config)) {
            return null;
        }
        return new ActiveClient(config);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getInstance(2)">
   /**
    * Factory that makes an active client from a config file path
    *
    * @param configfile path to config file
    *
    * @return
    */
    public static ActiveClient getInstance(String configfile) {
        File file = new File(configfile);
        if (null == file || !file.exists() || !file.canRead()) {
            lgrDebug.error("in getInstance: config file does not exists or "
                    + "is not readable: " + configfile);
            return null;
        }

        ConfigData config = ConfigData.getInstance(configfile);
        if (null == config) {
            lgrDebug.error("in getInstance: Could nto load config file: "
                    + configfile);
            return null;
        }
        return getInstance(config);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getInstance(3)">
    /**
     *
     * @param monitorHostname Hostname or ip-address of the monitor
     * @param monitorPort Monitor port
     * @param serverHostname hostname or ip-address of the passive server
     * @param serverPort port of the passive server
     * @param identity account identity to connect to the monitor with
     * @param password the password to connect to the monitor with
     * @param cookie the monitor cookie
     *
     * @return
     */
    public static ActiveClient getInstance(
            String monitorHostname, int monitorPort,
            String serverHostname, int serverPort,
            String identity, String password, String cookie) {
        ConfigData config = ConfigData.getInstance("ActiveClient.cfg");
        config.addOrSetProperty("identity", identity);
        config.addOrSetProperty("password", password);
        config.addOrSetProperty("cookie", cookie);
        config.addOrSetProperty("monitorHostname", monitorHostname);
        config.addOrSetProperty("monitorPort", String.valueOf(monitorPort));
        config.addOrSetProperty("serverHostname", serverHostname);
        config.addOrSetProperty("serverPort", String.valueOf(serverPort));
        return getInstance(config);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getDir">
    /**
     * Gets the currently processing/or most recently processed directive
     *
     * @return
     */
    public Directive getDir() {
        return dir;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getMsgs">
    /**
     * Gets the current messageGroup returned from the monitor
     * 
     * @return
     */
    public MessageGroup getMsgs() {
        return msgs;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="launch">
    public void launch() {
        if (null == runner) {
            runner = new Thread(this);
        }
        runner.start();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="run">
    /**
     * I don't really use this. But just in case we need some repetition, it
     * can be done here. See the AttackClient branch for more specialized
     * and (hopefully) intelligent usage of an ActiveClient.
     *
     */
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
    /**
     * Implement monitor login and alive handshake protocol
     *
     * @return True if login to the monitor succeeded, false otherwise
     *
     */
    public boolean login() {
        boolean result;
        try {

            // Expect a request for IDENT
            msgs = receiveMessageGroup();
            if(!expect(DirectiveType.REQUIRE,"IDENT")) {
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

            // Incoming/Outgoing encrypted data should automatically
            // be handled and decrypted by the CommandInterpreter subsystem
            
            msgs = receiveMessageGroup();
            if(!expect(DirectiveType.REQUIRE)) {
                return false;
            }

            if (dir.getArg().equals("PASSWORD")) {
                if(!executeCommand(Command.PASSWORD)) {
                    logger.error("Failed to execute PASSWORD command");
                    return false;
                }

                // Expect cookie
                msgs = receiveMessageGroup();
                if(!expect(DirectiveType.RESULT,"PASSWORD")) {
                    return false;
                }

                // Store and save the cookie
                CONFIG.addOrSetProperty("cookie",dir.getArg(1));
                CONFIG.save();
                
            } else if (dir.getArg().equals("ALIVE")) {
                if(!executeCommand(Command.ALIVE)) {
                    logger.error("Failed to execute ALIVE command");
                    return false;
                }
                msgs = receiveMessageGroup();
                if(!expect(DirectiveType.RESULT,"ALIVE")) {
                    return false;
                }
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

    // <editor-fold defaultstate="collapsed" desc="doTransfer">
    public boolean doTransfer(String to, String from, int amount) {
        try {
            logger.info("Initiating transfer protocol[ from "
                    + from + " to " + to + " amount: " + amount + "]");
            Random rand = new Random();

            // Execute TRANSFER_REQUEST
            boolean result = executeCommand(Command.TRANSFER_REQUEST,
                    to, Integer.toString(amount), "FROM", from);
            if (!result) {
                logger.error("Failed to execute command: "
                        + Command.TRANSFER_REQUEST);
                return false;
            }

            // Expect REQUIRE: PUBLIC_KEY
            MessageGroup msgs = receiveMessageGroup();
            System.out.println(msgs);
            Directive dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE, "PUBLIC_KEY");
            if(!this.checkDirective(dir, DirectiveType.REQUIRE, "PUBLIC_KEY")) {
                return false;
            }

            // Get my public key
            RSAKeys myKeys = RSAKeys.getInstance();

            // Execute PUBLIC_KEY
            result = executeCommand(Command.PUBLIC_KEY,
                    myKeys.getV().toString(), myKeys.getN().toString());
            if (!result) {
                logger.error("Failed to execute command: "
                        + Command.PUBLIC_KEY);
                return false;
            }

            // Expect RESULT: ROUNDS
            msgs = receiveMessageGroup();
            dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "ROUNDS");
            if (!checkDirective(dir, DirectiveType.RESULT, "ROUNDS")) {
                return false;
            }

            final int rounds;
            try {
                rounds = Integer.parseInt(dir.getArg(1));
            } catch (NumberFormatException ex ) {
                logger.error("Could not parse the number of rounds into "
                        + "an int: " + dir);
                return false;
            }

            // Expect REQUIRE Authorize set
            msgs = receiveMessageGroup();
            dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE, "AUTHORIZE_SET");
            if (!checkDirective(dir, DirectiveType.REQUIRE, "AUTHORIZE_SET")) {
                return false;
            }

            // Calcuate AUTHORIZE_SET
            StringBuilder fodder = new StringBuilder();
            BigInteger authSet[] = new BigInteger[rounds];
            for(int i=0; i<rounds; i++) {
                authSet[i] = new BigInteger(256,rand);
                fodder.append(" ").append(authSet[i]);
            }

            // Execute AUTHORIZE_SET
            result = executeCommand(Command.AUTHORIZE_SET, fodder.toString());
            if (!result) {
                logger.error("Failed to execute command: " + Command.AUTHORIZE_SET);
                return false;
            }

            // Expect SUBSET_A
            msgs = receiveMessageGroup();
            dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "SUBSET_A");
            if (!checkDirective(dir, DirectiveType.RESULT, "SUBSET_A")) {
                return false;
            }

            //store SUBSET_A
            Integer subsetA[] = dir.getArgIntArray(1);
            if(null == subsetA || subsetA.length > rounds) {
                logger.error("Unable to parse SUBSET_A from result or SUBSET_A "
                        + "has more values than rounds: " + dir);
                return false;
            }

            //Expect REQUIRE SUBSET_K
            dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE, "SUBSET_K");
            if (!checkDirective(dir, DirectiveType.REQUIRE, "SUBSET_K")) {
                return false;
            }


            // Compute SUBSET_K
            BigInteger subsetK[] = new BigInteger[subsetA.length];
            int j=0;
            for(Integer i : subsetA) {
                subsetK[j++] = authSet[i].multiply(myKeys.getS())
                        .modPow(TWO, myKeys.getN());
                fodder.append(" ").append(subsetK[j-1]);
            }


            // Execute SubsetK
            result = executeCommand(Command.SUBSET_K, fodder.toString());
            if (!result) {
                logger.error("Failed to execute command: " + Command.SUBSET_K);
                return false;
            }

            // Expect Require SUBSET_J
            msgs = receiveMessageGroup();
            dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE, "SUBSET_J");
            if (!checkDirective(dir, DirectiveType.REQUIRE, "SUBSET_J")) {
                return false;
            }

            // Compute SUBSET_J (more fugly code.... so sue me)
            fodder = new StringBuilder();
            Arrays.sort(subsetA);
            BigInteger subsetJ[] = new BigInteger[rounds-subsetA.length];
            j=0;
            int k=0;
            for(int i=0; i<rounds; i++) {
                if(subsetA[j] == i) {
                    j++;
                } else {
                    subsetJ[k++] = authSet[i].modPow(TWO, myKeys.getN());
                    fodder.append(" ").append(subsetJ[k-1]);
                }
            }

            // Unlock the lockfile
            logger.info("LOCKFILE unlock success?: " + setLockYes());

            // Execute Subset J
            result = executeCommand(Command.SUBSET_J, fodder.toString());
            if (!result) {
                logger.error("Failed to execute command: " + Command.SUBSET_J);
                return false;
            }

            // Expect Result TRANSFER_RESPONSE
            msgs = receiveMessageGroup();
            dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "TRANSFER_RESPONSE");
            if (!checkDirective(dir, DirectiveType.RESULT, "TRANSFER_RESPONSE")) {
                return false;
            }

            logger.info(dir);
            return true;
        } catch (Exception ex) {
            setLockNo();
            logger.error(ex);
            return false;
        }
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

    // <editor-fold defaultstate="collapsed" desc="checkAndInitConfigData">
    /**
     * Checks the {@link ConfigData} object for the required properties that
     * are necessary for the ActiveClient to operate. It also
     * generates a new RSA key pair if one is needed.
     *
     * @param config 
     *
     * @return true if the required properties are present, false otherwise
     */
    public static boolean checkAndInitConfigData(final ConfigData config) {
        if (null == config) {
            return false;
        }
        if (!config.hasProperty("identity")
                || !config.hasProperty("cookie")
                || !config.hasProperty("monitorHostname")
                || !config.hasProperty("monitorPort")
                || !config.hasProperty("serverHostname")
                || !config.hasProperty("serverPort")
                || !config.hasProperty("password")) {
            lgrDebug.error("Invalid config file format");
            return false;
        }
        if (!config.hasProperty("keyS")
                || !config.hasProperty("KeyN")
                || !config.hasProperty("keyV")) {
            RSAKeys keys = RSAKeys.getInstance();
            config.addOrSetProperty("keyS", keys.getS().toString(10));
            config.addOrSetProperty("keyN", keys.getN().toString(10));
            config.addOrSetProperty("keyV", keys.getV().toString(10));
            config.save();
        }

        // Test string formatting of numbers
        try {
            int testInt;
            BigInteger testBig;
            testInt = Integer.parseInt(config.getProperty("monitorPort"));
            testInt = Integer.parseInt(config.getProperty("serverPort"));
            testBig = new BigInteger(config.getProperty("keyS"),10);
            testBig = new BigInteger(config.getProperty("keyN"),10);
            testBig = new BigInteger(config.getProperty("keyV"),10);
        } catch ( NumberFormatException ex ) {
            lgrDebug.error("Invalid number format in config file: " + ex);
            return false;
        }

        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="expect(1)">
    private boolean expect(DirectiveType dt, String arg0) {
        if (null == msgs) {
            error("The MessageGroup is null when looking for directive: "
                    + dt);
            return false;
        }
        if (null == arg0) {
            dir = msgs.getFirstDirectiveOf(dt);
            if (null == dir) {
                error("Expected Directive: " + dt + " but none were found in "
                        + "the current MessageGroup: " + msgs);
                return false;
            }
        } else {
            dir = msgs.getFirstDirectiveOf(dt, arg0);
            if (null == dir) {
                error("Expected Directive: " + dt + " with argument0: " + arg0
                        + ", but none were found in the current MessageGroup: "
                        + msgs);
            }
        }
        return true;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="expect(2)">
    private boolean expect(DirectiveType dt) {
        return expect(dt, null);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="error">
    public void error(Object obj) {
        logger.error(obj);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="warn">
    public void warn(Object obj) {
        logger.warn(obj);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="debug">
    public void debug(Object obj) {
        logger.debug(obj);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="info">
    public void info(Object obj) {
        logger.info(obj);
    }
    // </editor-fold>
}
