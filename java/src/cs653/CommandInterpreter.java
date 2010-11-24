/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileWriter;
import java.net.Socket;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.net.UnknownHostException;
import java.util.List;
import java.util.LinkedList;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileReader;
import java.util.Scanner;
import cs653.security.DiffieHellmanExchange;
import cs653.security.KarnCodec;


/**
 *
 * Responsible for interpreting commands, sending them to the monitor and
 * handling the results.
 *
 * @author Ryan Anderson
 */
public class CommandInterpreter
{

    protected static String COOKIE = null;
    protected static String HOSTNAME = null;
    protected static String PASSWORD;
    protected static String MONITORHOST = null;
    protected static int MONITORPORT;
    protected static int HOST_PORT;
    protected static boolean ENCRYPTION_ON = true;
    protected static final String FILENAME = "c:\\andersr9.txt";

    protected Socket socConnection = null;
    protected DiffieHellmanExchange dhe = null;
    protected KarnCodec karn = null;
    protected final String identity;
    
    private PrintWriter bufferOut = null;
    private BufferedReader bufferIn = null;
    private final Logger logger = Logger.getLogger(CommandInterpreter.class);

    public CommandInterpreter(
            String monitorHost,
            int monitorPort,
            int hostPort,
            String identity,
            String password) {
        MONITORHOST = monitorHost;
        MONITORPORT = monitorPort;
        HOST_PORT = hostPort;
        this.identity = identity;
        PASSWORD = password;
        loadConfig();
        logger.debug("Instanced CommandInterpreter");
    }

    public CommandInterpreter( String identity ) {
        this.identity = identity;
    }

    // <editor-fold defaultstate="collapsed" desc="openConnection">
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
            logger.debug("TEST1");
            socConnection = new Socket(host, port);
            logger.debug("TEST2");
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

    // <editor-fold defaultstate="collapsed" desc="initConnectionIO">
    /**
     * initialized the bufferIn and bufferOut for reading and writing to
     * the socket. Note if this is called with a closed socket it will return
     * false.
     *
     * @return True if IO buffers were successfully created for the socket
     * false otherwise.
     */
    protected boolean initConnectionIO() {
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
    protected void closeConnection() {
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
     * </p>
     *
     * @return
     */
    protected MessageGroup receiveMessageGroup() {
        try {
            List<Directive> dirs = new LinkedList<Directive>();
            Directive dir = null;

            // Handle Encryption
            String strReceived = bufferIn.readLine().trim();
            String strPlain = null;

            if( ENCRYPTION_ON && null != karn ) {
                strPlain = karn.decrypt(strReceived);
                logger.debug("Received Encrypted Text: " + strReceived);
                logger.debug("Decrypted to: " + strPlain);
            } else {
                strPlain = strReceived;
                logger.debug("Received Plaintext: " + strPlain);
            }
            
            while (!strPlain.matches("WAITING(.)*")) {
                dir = Directive.getInstance(strPlain);
                if (null == dir) {
                    logger.warn("Received invalid directive (ignoring): "
                            + strPlain);
                } else {
                    logger.debug("Received directive: " + dir);
                    dirs.add(dir);
                }
                strPlain = bufferIn.readLine().trim();
            }
            return new MessageGroup(dirs);
        } catch (java.io.IOException e) {
            logger.error(e);
            return null;
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

        // Handle Encrytion
        String sendText = null;
        if( ENCRYPTION_ON && null != karn ) {
            sendText = karn.encrypt(sendText);
        } else {
            sendText = message;
        }
        
        bufferOut.println(sendText);
        logger.debug("Sent command: " + message);

        return 1;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="executeCommand">
    protected boolean executeCommand(Command command, String... args) {
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
                    sendCommand(command, PASSWORD);
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
                    sendCommand(command, HOSTNAME, String.valueOf(HOST_PORT));
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
                    sendCommand(command, COOKIE);
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
            default: {
                logger.error("Command: [" + command + "] not handled");
                return false;
            }
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="saveConfig">
    public void saveConfig(String password, String cookie) {
        PrintWriter file = null;
        if (null != password && password.trim().length() > 0) {
            try {
                file = new PrintWriter(new FileWriter(FILENAME));
                file.println("PASSWORD = " + password);
                if (null != cookie && cookie.trim().length() > 0) {
                    file.println("COOKIE = " + cookie);
                }
                file.close();
            } catch (IOException e) {
                logger.error("Failed to write config file: " + e);
                return;
            }
            logger.debug("Saved Configuration file [" + FILENAME + "] Successfully.");
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="loadConfig">
    protected  final void loadConfig() {
        File file = null;
        Scanner scanner = null;
        try {
            file = new File(FILENAME);
            scanner = new Scanner(new FileReader(file));
            while (scanner.hasNextLine()) {
                String str = scanner.nextLine().trim();
                if (str.matches("PASSWORD(\\s)*=(.)*")) {
                    PASSWORD = str.replaceFirst("PASSWORD(\\s)*=(\\s)*", "");
                } else if (str.matches("COOKIE(\\s)*=(.)*")) {
                    COOKIE = str.replaceFirst("COOKIE(\\s)*=(\\s)*", "");
                }
            }
        } catch (IOException e) {
            logger.error("Failed to load config file: " + e);
            return;
        }
        logger.debug("Config file[" + FILENAME + "] loaded successfully.");
    }
    // </editor-fold>
}
