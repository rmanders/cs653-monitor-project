/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.net.UnknownHostException;
import java.util.List;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import cs653.security.DiffieHellmanExchange;
import cs653.security.KarnCodec;
import java.io.FileReader;
import java.io.FileWriter;
import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author Ryan Anderson
 *
 * Responsible for interpreting commands, sending them to the monitor and
 * handling the results. Handles encryption/decryption so subclasses don't have
 * to worry about it. Somewhat similar in function to Franco's MessageParser
 * class.
 *
 */
public class CommandInterpreter
{
    // Static variables

    /** Lock-file path **/
    //private static final String LOCKFILE = "/home/andersr9/lockfile.lck";
    private static final String LOCKFILE = "C:\\lockfile.txt";

    /** Expect encryption or not. **/
    protected static final boolean ENCRYPTION_ON = true;
    
    /** Use certificates or not (faster not to if using a lock-file) */
    protected static final boolean CERTIFICATION_ON = false;

    /** The maximum number of rounds to issue in a Zero-knowledge proof **/
    protected static final int ZKP_MAX_ROUNDS = 20;

    /** The minimum number of rounds to issue in a Zero-knowledge proof **/
    protected static final int ZKP_MIN_ROUNDS = 10;

    /** The monitors exponent used for certification **/
    protected static final BigInteger MON_EXP = new BigInteger("65537",10);


    private static final Pattern encPattern =
            Pattern.compile("^(RESULT):[\\s]+(IDENT)[\\s]+([a-zA-Z0-9]+)");

    private static final String[] requiredConfigProperties = 
            {"identity","password","monitorHostname","monitorPort",
             "serverHostname","serverPort"};

    // Instance variables
    protected ConfigData config = null;
    protected Socket socConnection = null;
    protected DiffieHellmanExchange dhe = null;
    protected KarnCodec karn = null;
    protected String identity;
    protected final Logger logger;
    
    private PrintWriter bufferOut = null;
    private BufferedReader bufferIn = null;

    public CommandInterpreter( String monitorHostname, int monitorPort,
            String serverHostname, int serverPort,
            String identity, String password, Logger logger ) {
        this.logger = logger;
        config = new ConfigData();
        config.addProperty("monitorHostname", monitorHostname);
        config.addProperty("monitorPort", String.valueOf(monitorPort));
        config.addProperty("serverHostname", serverHostname);
        config.addProperty("serverPort", String.valueOf(serverPort));
        config.addProperty("identity", identity);
        config.addProperty("password", password);
        this.identity = identity;
        logger.debug("Instanced CommandInterpreter");
    }

    public CommandInterpreter( String configfile, Logger logger ) {
        this.logger = logger;
        if( !loadConfig(configfile) ) {
            logger.error("FATAL ERROR: couldn't load configuration file");
            System.exit(1);
        }
        this.identity = config.getProperty("identity");
    }

    public CommandInterpreter( ConfigData config, Logger logger ) {
        this.logger = logger;
        if( !checkConfig(config)) {
            System.exit(1);
        }
        this.config = config;
        
        // TODO: this is a hack
        this.identity = this.config.getProperty("identity");
    }


    // <editor-fold defaultstate="collapsed" desc="openConnection(1)">
    /**
     * Opens a socket connection at the specified port and host
     *
     * @param host Hostname or ip address
     * @param port The Host port
     * @return
     */
    public boolean openConnection(final String host, final int port) {
        try {
            logger.debug("Connecting to: " + host + ", " + port);
            socConnection = new Socket(host, port);
        } catch (UnknownHostException ex) {
            logger.error(ex);
            socConnection = null;
            return false;
        } catch (IOException ex) {
            logger.error(ex);
            socConnection = null;
            return false;
        }
        logger.debug("Successfully opened Socket connection to "
                + host + " at port " + String.valueOf(port));

        return initConnectionIO();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="OpenConnection(2)">
    /**
     *
     * Opens a connection using the config data
     *
     * @return
     */
    public boolean openConnection() {
        try {
            String host = config.getProperty("monitorHostname");
            int port = Integer.parseInt(config.getProperty("monitorPort"));
            return openConnection(host, port);
        } catch (Exception ex) {
            logger.error(ex);
            return false;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="initConnectionIO">
    /**
     * initialized the bufferIn and bufferOut for reading and writing to
     * the socket. Note if this is called with a closed socket it will return
     * false.
     *
     * @return True if IO buffers were successfully created for the socket
     * false otherwise.
     */
    public boolean initConnectionIO() {
        try {
            bufferIn = new BufferedReader(new InputStreamReader(socConnection.getInputStream()));
            logger.debug("Successfully opened input buffer.");
            bufferOut = new PrintWriter(socConnection.getOutputStream(), true);
            logger.debug("Successfully opened output buffer.");

        } catch (IOException ex) {
            logger.error(ex);
            closeConnection();
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="closeConnection">
    /**
     * Closes the Socket and I/O buffers (if open)
     */
    public void closeConnection() {
        try {
            if (null != bufferOut) {
                bufferOut.close();
                bufferOut = null;
                logger.debug("Output buffer closed");
            }
            if (null != bufferIn) {
                bufferIn.close();
                bufferIn = null;
                logger.debug("Input buffer closed");
            }
            if (null != socConnection) {
                socConnection.close();
                socConnection = null;
                logger.debug("Socket connection closed.");
            }
        } catch (IOException ex) {
            logger.error(ex);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="receiveMessageGroup">
    /**
     * Receives a message group from the monitor.
     *
     * NOTE: This method blocks until a "WAITING" directive is received.
     *
     * <p>
     * NOTE ON ENCRYPTION: One problem with my architecture and implementing
     * encryption is that if I send the monitor an indication that I want
     * encryption, it will send back a message group that is partially
     * encrypted. That is, it will send me it's public key in plain text but
     * then the rest of that message group, including the REQUIRE and WAITING
     * directives will be encrypted. This poses a problem since I originally
     * handled the setup of encryption at a higher level (in the login
     * handshake). By handling it that way, I can't avoid getting a partially
     * encrypted message that I can't decrypt. This happens because even though
     * the encryption setup happens at a higher level, the actual encrypting and
     * decrypting happens here. So I can't decrypt until the setup is complete.
     * <br \><br \>
     * For this reason, I am handling ALL of the encryption setup at this level.
     * This is done by monitoring the messages and setting up encryption when
     * triggered by a RESULT IDENT <public key> directive.
     * </p>
     *
     * @return
     */
    public MessageGroup receiveMessageGroup() {
        try {
            List<Directive> dirs = new LinkedList<Directive>();
            Directive dir = null;

            // Get and process the raw data
            String rawData = bufferIn.readLine();
            if( null == rawData || rawData.length() == 0) {
                logger.error("Received Empty message: " + rawData);
                return null;
            }
            String strPlain = processMessage(rawData.trim());
            
            while (!strPlain.matches("WAITING:(.)*")) {

                dir = Directive.getInstance(strPlain);
                if (null == dir) {
                    logger.warn("Received invalid directive (ignoring): "
                            + strPlain);
                } else {
                    logger.debug("Received directive: " + dir);
                    dirs.add(dir);
                }

                // Get and process the raw data
                // NOTE: readLine can be null if a quit was issued
                rawData =  bufferIn.readLine();
                if( null == rawData ) {
                    break;
                }
                strPlain = processMessage(rawData.trim());
            } // End While

            if (strPlain.matches("WAITING:(.)*")) {
                dir = Directive.getInstance(strPlain);
                if( null != dir ) {
                    dirs.add(dir);
                }
            }
            return new MessageGroup(dirs);
        } catch (java.io.IOException e) {
            logger.error(e);
            return null;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="processMessage">
    /**
     * This method filters and monitors all incoming messages. A message
     * is considered a string terminated by a newline. This method also handles
     * the decryption of all messages if Encryption is enabled. In addition,
     * it examines each incoming message and if it detects the pattern:
     * RESULT: IDENT <key>, then it immediately sets up encryption
     *
     * @param message encrypted or plaintext message from somewhere on the
     * internets.
     *
     * @return plaintext
     */
    protected String processMessage(String message) {
        logger.debug("Processing Message: " + message);
        String msg = message.trim();
        Matcher matcher = encPattern.matcher(msg);

        // if I get this pattern then I know the next messages will be encrypted
        if (matcher.matches() && null == karn) {
            // assume diffie-hellman has been started
            logger.info("Got encyption trigger message! -> " + msg);
            if (null == dhe) {
                logger.warn("WARNING: Expecting encrypted messages from "
                        + "the monitor, but the local instance of Diffie-Hellman"
                        + " is null, nothing will be decrypted.");
                return msg;
            }

            // This message will contain the monitor's public key
            // From this, Generate the Secret Key
            BigInteger secretKey = dhe.getSecretKey(matcher.group(3));
            karn = KarnCodec.getInstance(secretKey);
            if (null == karn) {
                logger.error("FATAL ENCRYPTION ERROR: Unable to instantiate "
                        + "KarnCodec encryption. New messages can't be "
                        + "decrypted");
                return msg;
            }
            logger.info("Successfully started encryption. "
                    + "Secure Connection enabled");
            return msg;
        } else if (ENCRYPTION_ON && null != karn) {
            return karn.decrypt(msg);
        } else {
            return msg;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="sendCommand">
    /**
     * Sends a command to the monitor server. Checks for the minimum number
     * of required arguments.
     *
     * @param command Type of command being sent.
     * @param args Arguments for the command
     * @return Always returns 1 if sent, 0 otherwise
     */
    protected int sendCommand(Command command, String... args) {

        if (args.length < command.getMinArgs()) {
            logger.error("Too few arguments for command: [" + command + "]");
            return 0;
        }
        if (args.length > command.getMaxArgs()) {
            logger.warn("Argument count exceeds max arguments for command: ["
                    + command + "]");
        }

        String message = command.getCommandString() + " ";
        for (String arg : args) {
            message += arg + " ";
        }
        message = message.trim();
        logger.debug("Sending message: " + message);

        // Handle Encrytion
        String sendText = null;
        if( ENCRYPTION_ON && null != karn ) {
            sendText = karn.encrypt(message);
        } else {
            sendText = message;
        }
        
        bufferOut.println(sendText);
        logger.debug("Sent command: " + message);

        return 1;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="sendText">
    /**
     * Sends free-form text to the monitor server.
     * Encrypts if encryption is active
     *
     * @param textmsg
     * @param args Arguments for the command
     * @return Always returns 1 if sent, 0 otherwise
     */
    public int sendText( String textmsg) {


        // Handle Encrytion
        String sendText = null;
        if( ENCRYPTION_ON && null != karn ) {
            sendText = karn.encrypt(textmsg);
        } else {
            sendText = textmsg;
        }

        bufferOut.println(sendText);
        logger.debug("Sent free-form text message: " + textmsg);

        return 1;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="executeCommand">
    /**
     *
     * Checks command and its parameters and then send it to the monitor.
     * most of the commands just get sent without checking, but the structure
     * is in place if more rigorous parameter checking is required.
     *
     * @param command A {@link Command} object
     * @param args Command arguments
     * @return True if the command is a known/valid command. False otherwise.
     */
    public boolean executeCommand(Command command, String... args) {
        switch (command) {
            case IDENT: {
                if (args.length == 1 || args.length == 2) {
                    sendCommand(command, args);
                } else {
                    sendCommand(command, identity);
                }
                break;
            }
            case QUIT: {
                sendCommand(command);
                break;
            }
            case PASSWORD: {
                if (args.length == 1) {
                    sendCommand(command, args[0]);
                } else {
                    sendCommand(command, config.getProperty("password"));
                }
                break;
            }
            case CHANGE_PASSWORD: {
                sendCommand(command, args);
                break;
            }
            case HOST_PORT: {
                if (args.length == 2) {
                    sendCommand(command, args[0], args[1]);
                } else {
                    sendCommand(command, config.getProperty("serverHostname"),
                            config.getProperty("serverPort"));
                }
                break;
            }
            case SIGN_OFF: {
                sendCommand(command);
                break;
            }
            case ALIVE: {
                if (args.length == 1) {
                    sendCommand(command, args[0]);
                } else {
                    if( config.hasProperty("cookie") ) {
                        sendCommand(command, config.getProperty("cookie"));
                    } else {
                        logger.error("No monitor cookie found in CONFIG "
                                + "properties, sending null cookie value");
                        sendCommand(command, "null");
                    }
                }
                break;
            }
            case GET_GAME_IDENTS: {
                sendCommand(command);
                break;
            }
            case RANDOM_PARTICIPANT_HOST_PORT: {
                sendCommand(command);
                break;
            }
            case PARTICIPANT_HOST_PORT: {
                sendCommand(command, args);
            }
            case PARTICIPANT_STATUS: {
                sendCommand(command, args);
            }
            case TRANSFER_REQUEST: {
                sendCommand(command, args);
                return true;
            }
            case TRANSFER_RESPONSE: {
                sendCommand(command, args);
                return true;
            }
            case PUBLIC_KEY: {
                sendCommand(command, args);
                return true;
            }
            case ROUNDS: {
                if( args.length == 1) {
                    sendCommand(command, args[0]);
                    return true;
                } else {
                    logger.error("Invalid number of arguments["
                            + args.length + " for Command: " + command );
                    return false;
                }
            }
            case AUTHORIZE_SET: {
                sendCommand(command, args);
                return true;
            }
            case SUBSET_A: {
                sendCommand(command, args);
                return true;
            }
            case SUBSET_J: {
                sendCommand(command, args);
                return true;
            }
            case SUBSET_K: {
                sendCommand(command, args);
                return true;
            }
            case GET_MONITOR_KEY: {
                sendCommand(command, args);
                return true;
            }
            case GET_CERTIFICATE: {
                sendCommand(command, args);
                return true;
            }
            case MAKE_CERTIFICATE: {
                sendCommand(command, args);
                return true;
            }
            default: {
                logger.error("Command: [" + command + "] not handled");
                return false;
            }
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="loadConfig">
    protected final boolean loadConfig( String filename ) {
        // TODO: verify integer value as integer prsable
        config = ConfigData.getInstance(filename);
        if( null == config ) {
            logger.error("Failed to load configuration file: " + filename);
            return false;
        }

        // Check for existence of required properties
        for( String prop : requiredConfigProperties ) {
            if(!config.hasProperty(prop)) {
                logger.error("FATAL ERROR: while loading configuration file. "
                        + "The required property [" + prop +
                        "] was not found in  the file.");
                return false;
            }
        }

        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkConfig">
    protected final boolean checkConfig(ConfigData config) {
        for (String prop : requiredConfigProperties) {
            if (!config.hasProperty(prop)) {
                logger.error("FATAL ERROR: while reading config data: "
                        + "The required property [" + prop +
                        "] was not found.");
                return false;
            }
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getSocket">
    public Socket getSocket() {
        return socConnection;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="setLockNo">
    /**
     *
     * Set the lockfile value to no
     *
     * @return True if we succeeded in writing to the lockfile, false otherwise
     */
    public boolean setLockNo() {
        try {
            PrintWriter file = new PrintWriter(new FileWriter(LOCKFILE));
            file.println("no");
            file.close();
            return true;
        } catch (Exception ex) {
            logger.error(ex);
            return false;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="setLockYes">
    /**
     * Set the lockfile value to yes
     *
     * @return True if we succeeded in writing to the lockfile, false otherwise
     */
    public boolean setLockYes() {
        try {
            PrintWriter file = new PrintWriter(new FileWriter(LOCKFILE));
            file.println("yes");
            file.close();
            return true;
        } catch (Exception ex) {
            logger.error(ex);
            return false;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="isLockOpen">
    /**
     *
     * Reads the lock file to determine if it is safe to approve a transfer
     *
     * @return true if it is safe to transfer, false otherwise.
     */
    public boolean isLockOpen() {
        try {
            BufferedReader reader =
                    new BufferedReader(new FileReader(LOCKFILE));
            String line = reader.readLine().trim().toLowerCase();
            reader.close();
            return (line != null && line.equals("yes"));
        } catch (Exception ex) {
            logger.error(ex);
            return false;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="setDhe">
    public void setDhe(DiffieHellmanExchange dhe) {
        this.dhe = dhe;
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="getCONFIG">
    /**
     * Gets this objects config info
     *
     * @return {@link ConfigData} reference
     */
    public ConfigData getCONFIG() {
        return config;
    }
    // </editor-fold>


}
