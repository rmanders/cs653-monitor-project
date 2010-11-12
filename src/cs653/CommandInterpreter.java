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


/**
 *
 * Responsible for interpreting commands, sending them to the monitor and
 * handling the results.
 *
 * @author Ryan Anderson
 */
public class CommandInterpreter
{
    private final Logger logger =
            Logger.getLogger(CommandInterpreter.class);

    protected static String COOKIE = null;
    protected static String HOST_PORT = null;
    protected static String HOSTNAME = null;
    protected static String PASSWORD;
    protected static String MONITORHOST = null;
    protected static int MONITORPORT;
    protected static final String FILENAME = "c:\\andersr9.txt";

    private final String identity;
    private Socket sockConnection = null;
    private PrintWriter bufferOut = null;;
    private BufferedReader bufferIn = null;

    public CommandInterpreter(
            String monitorHost,
            int monitorPort,
            String identity,
            String password) {
        MONITORHOST = monitorHost;
        MONITORPORT = monitorPort;
        this.identity = identity;
        PASSWORD = password;
        loadConfig();
        logger.debug("Instanced CommandInterpreter");
    }

    // <editor-fold defaultstate="collapsed" desc="openConnection">
    /**
     * Opens a socket connection at the specified port and host
     *
     * @param host Hostname or ip address
     * @param port The Host port
     * @return
     */
    protected boolean openConnection(final String host, final int port) {
        try {
            logger.debug("Connecting to: " + host + ", " + port);
            sockConnection = new Socket(host, port);
        } catch (UnknownHostException ex) {
            logger.error(ex);
            sockConnection = null;
            return false;
        } catch (IOException ex) {
            logger.error(ex);
            sockConnection = null;
            return false;
        }
        logger.debug("Successfully opened Socket connection to "
                + host + " at port " + String.valueOf(port));
        try {
            bufferIn = new BufferedReader(new InputStreamReader(sockConnection
                    .getInputStream()));
            logger.debug("Successfully opened input buffer.");
            bufferOut = new PrintWriter(sockConnection.getOutputStream(), true );
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
            if (null != sockConnection) {
                sockConnection.close();
                sockConnection = null;
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
     * @return
     */
    protected MessageGroup receiveMessageGroup() {
        try {
            List<Directive> dirs = new LinkedList<Directive>();
            Directive dir = null;
            String strPlain = bufferIn.readLine().trim();
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
        if (args.length > command.getMinArgs()) {
            logger.warn("Argument count exceeds max arguments for command: ["
                    + command + "]");
        }

        String message = command.getCommandString() + " ";
        for (String arg : args) {
            message += arg + " ";
        }
        message = message.trim();

        bufferOut.println(message);
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
                    sendCommand(command, HOSTNAME, HOST_PORT);
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
            logger.debug("Saved Configuration Successfully.");
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
        logger.debug("Config file loaded successfully.");
    }
    // </editor-fold>
}
