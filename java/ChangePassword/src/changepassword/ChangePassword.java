/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package changepassword;

import cs653.*;
import cs653.security.DiffieHellmanExchange;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.logging.Level;
import org.apache.log4j.Logger;


/**
 *
 * @author rmanders
 */
public class ChangePassword  {

    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    private static final String USAGE =
            "Usage: ChangePassword <account name> [time between changes (seconds)] [max number of changes]";

    private static String PROMPT = "%>";
    public static final long TIMEOUT_MON = 178; //seconds

    private static final int XFER_DELAY = 204; //seconds (3.4 min)
    //private static final int XFER_DELAY = 114; //seconds (1.8 min) at 60 sec intervals for pw changes

    private static final String XFER_FILE = "/home/andersr9/cs653/xfer.txt";
    private static final String TEST1 = "/home/andersr9/cs653/test1/test1.cfg";
    private static final String TEST2 = "/home/andersr9/cs653/test2/test2.cfg";
    private static final String TEST3 = "/home/andersr9/cs653/test3/test3.cfg";

    //private static final String XFER_FILE = "c:\\xfer.txt";
    //private static final String TEST1 = "c:\\test1.cfg";
    //private static final String TEST2 = "c:\\test2.cfg";
    //private static final String TEST3 = "c:\\test3.cfg";

    private static final Map<String,String> TO_IDENT;
    private static final Map<String,String> FROM_IDENT;

    static {
        Map<String,String> m = new HashMap<String,String>(3);
        m.put("TEST1", "TEST3");
        m.put("TEST2", "TEST1");
        m.put("TEST3", "TEST2");
        TO_IDENT = Collections.unmodifiableMap(m);

        Map<String,String> n = new HashMap<String,String>(3);
        n.put("TEST1", "TEST2");
        n.put("TEST2", "TEST3");
        n.put("TEST3", "TEST1");
        FROM_IDENT = Collections.unmodifiableMap(n);
    }


    protected ConfigData config = null;
    protected ActiveClient client = null;
    protected SecureRandom secRand = new SecureRandom();
    protected int sleepTime = 20000;
    protected int maxChanges = -1;
    protected int xferAmount = 0;
    protected final Logger logger;
    protected MessageGroup msgs = null;
    protected Directive dir = null;
    protected long startTime = 0;
    protected long endTime = 0;
    protected String identity;

    // <editor-fold defaultstate="collapsed" desc="constructor(1)">
    private ChangePassword(ConfigData config) {
        this.config = config;
        this.logger = Logger.getLogger("ChangePassword ["
                + this.config.getProperty("identity").toUpperCase());
        this.client = new ActiveClient(this.config);
        this.identity = config.getProperty("identity").toUpperCase();
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="constructor(2)">
    private ChangePassword(ConfigData config, int sleepTime, int maxChanges) {
        this.config = config;
        this.logger = Logger.getLogger("ChangePassword ["
                + this.config.getProperty("identity").toUpperCase());
        this.sleepTime = sleepTime;
        this.maxChanges = maxChanges;
        this.client = new ActiveClient(this.config);
        this.identity = config.getProperty("identity").toUpperCase();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getInstance(1)">
    public static ChangePassword getInstance(ConfigData config) {
        if (!checkConfig(config)) {
            return null;
        }
        return new ChangePassword(config);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getInstance(2)">
    public static ChangePassword getInstance(ConfigData config,
            int sleepTime, int maxChanges) {
        if (!checkConfig(config)) {
            return null;
        }
        if (sleepTime < 0) {
            return null;
        }
        if (maxChanges < -1) {
            return null;
        }
        return new ChangePassword(config, sleepTime, maxChanges);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="MAIN">
    public static void main(String[] args) {

        // Check args
        if (args.length < 1 || args.length > 3) {
            System.out.println(USAGE);
            die();
        }

        // get args into vars
        String acct = args[0].trim().toUpperCase();

        // Determine which config file to open.
        String cfgFile = "";
        if (acct.equals("TEST1")) {
            cfgFile = TEST1;
        } else if (acct.equals("TEST2")) {
            cfgFile = TEST2;
        } else if (acct.equals("TEST3")) {
            cfgFile = TEST3;
        }

        // Get sleep
        int sleep = 20000;
        if (args.length >= 1) {
            try {
                sleep = Integer.parseInt(args[1]) * 1000;
                if (sleep < 0) {
                    echo("SleepTime was less than 0, setting to default");
                    sleep = 20000;
                }
            } catch (NumberFormatException ex) {
                echo("Invalid sleepTime format");
                echo(USAGE);
                die();
            }
        }

        // get maxLoops
        int maxLoops = -1;
        if (args.length >= 2) {
            try {
                maxLoops = Integer.parseInt(args[2]);
                if (maxLoops < -1) {
                    echo("maxChanges was less than -1, setting to default: "
                            + "loop forever");
                    maxLoops = -1;
                }
            } catch (NumberFormatException ex) {
                echo("Inavlid maxChanges format.");
                echo(USAGE);
                die();
            }
        }

        // Attempt to load the config file quit if fails
        File file = new File(cfgFile);
        if (!file.exists() || !file.canRead()) {
            echo("Config file unreadable or does not exist.");
            die();
        }

        ConfigData config = ConfigData.getInstance(cfgFile);
        if (null == config) {
            echo("FATAL: an error occurred while reading the config file");
            die();
        }

        // Make an instance of myself
        
        ChangePassword instance =
                ChangePassword.getInstance(config, sleep, maxLoops);
        if (null == instance) {
            echo("FATAL: couldn't get an instance of myself. Check the logs.");
        }

        // Run the instance
        instance.run();

    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="run">
    public void run() {

        boolean result;
        String account = "UNKNOWN";

        if (null != config) {
            account = config.getProperty("identity");
        }

        // Try to log in
        echo("Trying to log in...");
        if (!client.openConnection()) {
            echo("Could not open client connection. Check logs");
            die();
        }

        if (!login()) {
            echo("Tried to log into the monitor but couldn't. Check the logs.");
            panic();
            die();
        }
        echo("Monitor Log in succeeded.");

        int iterations = 1000000;
        if (maxChanges != -1) {
            iterations = maxChanges;
        }

        if (!checkMsgs()) {
            echo("No messages when some were expected. Check logs");
            panic();
            die();
        }
        msgs.reset();

        // Timing variables

        // Main loop
        resetClock();

        for (int i = 0; i < iterations; i++) {

            echo("Elappsed: " + elapsedTime());

            // Reconnect and do login if client period has passed
            if (elapsedTime() >= TIMEOUT_MON || null == client) {
                echo("Dropping and relogging in...");
                if (null != client) {
                    client.closeConnection();
                }
                client = new ActiveClient(config);
                if (!checkState()) {
                    panic();
                    die();
                }

                resetClock();
                client.openConnection();

                if (!login()) {
                    panic();
                    die();
                }
            }

            result = changePassword();
            if (!result) {
                echo("Change password failed on iteration number " + i
                        + " Exiting program.. Check the logs!");
                panic();
                break;
            }
            echo(now() + "[" + account + "]  Password Changed. New cookie["
                    + config.getProperty("cookie") + "]");

            // TRANSFER?
            int xferAmt = checkXferFile();
            if (0 < xferAmt) {
                String to = TO_IDENT.get(identity);
                String from = FROM_IDENT.get(identity);

                if (!transfer(to, from, xferAmt)) {
                    writeXferFail();
                    client.closeConnection();
                    client = null;
                    echo("TRANSFER FAILED. Turning off transfers. "
                            + "Check the logs");
                } else {
                    this.writeXferAmt(xferAmt);
                    echo("Transfered " + xferAmt + " from " + from
                            + " to " + to);
                }
            }

            sleep();
        }

        echo("Quitting...");
        client.closeConnection();
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="quit">
    public boolean quit() {
        logger.debug("Sending QUIT command");
        
        if (!checkWaitingNoRequire()) {
            return false;
        }        

        boolean result = client.executeCommand(Command.QUIT);
        if (!result) {
            logger.error("Failed to send the QUIT command!");
            return false;
        }

        msgs = client.receiveMessageGroup();
        if (!checkMsgs()) {
            return false;
        }

        dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "QUIT");
        if (!expectedDirective(dir, DirectiveType.RESULT, "QUIT")) {
            logger.error("Did get expected quit result... oh well, "
                    + "probably disconnecting anyway");
            msgs = null;
            return false;
        }
        logger.info("Successfully QUIT monitor session");
        msgs = null;
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="changePassword">
    public boolean changePassword() {
        logger.debug("Trying to change password...");

        // Check state
        if (!checkWaitingNoRequire()) {
            return false;
        }

        // Generate new password
        String pwd = new BigInteger(128, secRand).toString(32);
        String oldPwd = config.getProperty("password");

        logger.info("Trying to CHANGE_PASSWORD from [" + oldPwd + " to [" + pwd + "]");
        boolean result = client.executeCommand(Command.CHANGE_PASSWORD, oldPwd, pwd);
        if (!result) {
            logger.error("Failed to send the CHANGE_PASSWORD command!");
            return false;
        }

        // Get Results
        msgs = client.receiveMessageGroup();
        if (!checkWaitingNoRequire()) {
            logger.error("I send the CHANGE_PASSWORD command but I didn't get "
                    + "the expected reply (WAITING and no REQUIRE. "
                    + "I really got: " + msgs);
            return false;
        }
        dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "CHANGE_PASSWORD");
        if (!expectedDirective(dir, DirectiveType.RESULT, "CHANGE_PASSWORD")) {
            logger.error("FAILED TO CHANGE PASSWORD!");
            return false;
        }

        String cookie = dir.getArg(1);
        logger.info("SUCCESS CHANGE_PASSWORD: " + dir);
        logger.info("NEW COOKIE: [" + cookie + "]");

        // Set the config properties and save the config file
        config.setProperty("password", pwd);
        config.setProperty("cookie", cookie);
        config.save();
        logger.info("Configuration file saved with new password and cookie");

        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="login">
    public boolean login() {

        logger.debug("Doing monitor login.");

        msgs = client.receiveMessageGroup();
        if (!checkMsgs()) {
            return false;
        }

        // Expect a request for IDENT
        if (!checkAndSetDirective(DirectiveType.REQUIRE, "IDENT")) {
            return false;
        }

        // Assume to always use encryption
        DiffieHellmanExchange dhe = DiffieHellmanExchange.getInstance();
        if (null == dhe) {
            logger.error("Could not instance DiffieHellman");
            return false;
        }
        
        client.setDhe(dhe);

        // Execute the ident command
        String identity = config.getProperty("identity");
        String myPublicKey = dhe.getPublicKey().toString(32);
        if (!client.executeCommand(Command.IDENT, identity, myPublicKey)) {
            logger.error("Failed to execute IDENT command.");
            return false;
        }

        // Encryption should automatically be initialized by the message
        // receiving subsystem
        msgs = client.receiveMessageGroup();

        // Expect a RESULT IDENT
        if (!checkAndSetDirective(DirectiveType.RESULT, "IDENT")) {
            return false;
        }

        //Expect IDENT result to have the public key of the monitor
        if (dir.getArgCount() != 2) {
            logger.error("Monitor doesn't want to use encryption! "
                    + "Get out of here!");
            return false;
        }

        // Either require a password or require an alive
        dir = msgs.getNext(DirectiveType.REQUIRE);

        // If we got a PASSWORD:
        if (dir.getArg().equals("PASSWORD")) {
            if (!client.executeCommand(Command.PASSWORD)) {
                logger.error("Failed to execute PASSWORD command");
                return false;
            }
            // Expect a cookie here
            msgs = client.receiveMessageGroup();
            if (!checkAndSetDirective(DirectiveType.RESULT, "PASSWORD")
                    && dir.getArgCount() < 2) {
                logger.error("Login failed: expected password "
                        + "result with cookie.");
                return false;
            }

            // Store and save the cookie
            config.addOrSetProperty("cookie", dir.getArg(1));
            client.getCONFIG().addOrSetProperty("cookie", dir.getArg(1));
            config.save();

        } else if (dir.getArg().equals("ALIVE")) {
            // If we got an ALIVE:
            if (!client.executeCommand(Command.ALIVE)) {
                logger.error("Failed to execute ALIVE command");
                return false;
            }
            msgs = client.receiveMessageGroup();

            if (!checkAndSetDirective(DirectiveType.RESULT, "ALIVE")) {
                logger.error("Sent Alive but didn't get ALIVE result.");
                return false;
            }

        } else {
            logger.error("Login failed: expected ALIVE or PASSWORD, got: "
                    + dir);
            return false;
        }

        // See if the monitor ask for HOST_PORT
        msgs.reset();
        dir = msgs.getNext(DirectiveType.REQUIRE);
        if (null != dir && dir.getArg().equals("HOST_PORT")) {
            if (!client.executeCommand(Command.HOST_PORT)) {
                logger.error("Failed to execute HOST_PORT command");
                return false;
            }
            msgs = client.receiveMessageGroup();
            if (!checkAndSetDirective(DirectiveType.RESULT, "HOST_PORT")) {
                logger.error("Improper response for HOST_PORT command");
                return false;
            }
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="transfer">
    public boolean transfer(String to, String from, int amount) {
        logger.info("Initiating transfer protocol[ from "
                + from + " to " + to + " amount: " + amount + "]");

        if (!checkWaitingNoRequire()) {
            return false;
        }
        Random rand = new Random();

        // Command 1 (TRANSFER_REQUEST)
        if (!client.executeCommand(Command.TRANSFER_REQUEST,
                to, Integer.toString(amount), "FROM", from)) {
            logger.error("Failed to execute command: "
                    + Command.TRANSFER_REQUEST);
            return false;
        }

        msgs = client.receiveMessageGroup();
        logger.debug(msgs);

        // Calulate a bogus Public Key
        BigInteger p = BigInteger.probablePrime(64, rand);
        BigInteger g = BigInteger.probablePrime(64, rand);

        // Command 2 (PUBLIC_KEY)
        if (!client.executeCommand(Command.PUBLIC_KEY,
                p.toString(), g.toString())) {
            logger.error("Failed to execute command: "
                    + Command.PUBLIC_KEY);
            return false;
        }

        // Expect REQUIRE Authorize set
        msgs = client.receiveMessageGroup();
        dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE, "AUTHORIZE_SET");
        if (!expectedDirective(dir, DirectiveType.REQUIRE, "AUTHORIZE_SET")) {
            return false;
        }

        // Calcuate a fake authorize set
        String fodder = "7863687 6129879 182987 2131908 289746 12097 "
                + "03947 17364 8763 846";

        // Command 3 (AUTHORIZ_SET)
        if (!client.executeCommand(Command.AUTHORIZE_SET, fodder)) {

            logger.error("Failed to execute command: " + Command.AUTHORIZE_SET);
            return false;
        }

        // Expect SUBSET_A
        msgs = client.receiveMessageGroup();
        dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "SUBSET_A");
        if (!expectedDirective(dir, DirectiveType.RESULT, "SUBSET_A")) {
            return false;
        }

        // Compute Subset_K
        Integer subA[] = dir.getArgIntArray(1);
        String subStr = dir.getArg(1);
        if (null == subA) {
            logger.error("Error getting SUBSET_A arguments from: " + dir);
            return false;
        }

        //Expect REQUIRE SUBSET_K
        dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE, "SUBSET_K");
        if (!expectedDirective(dir, DirectiveType.REQUIRE, "SUBSET_K")) {
            return false;
        }

        // Command 4 (SUBSET_K)
        if (!client.executeCommand(Command.SUBSET_K, subStr)) {
            logger.error("Failed to execute command: " + Command.SUBSET_K);
            return false;
        }

        // Expect Require SUBSET_J
        msgs = client.receiveMessageGroup();
        dir = msgs.getFirstDirectiveOf(DirectiveType.REQUIRE, "SUBSET_J");
        if (!expectedDirective(dir, DirectiveType.REQUIRE, "SUBSET_J")) {
            return false;
        }

        // Calculate Subset J
        int i = (10 - subA.length);
        fodder = "";
        for (int j = 0; j < i; j++) {
            fodder += " " + rand.nextInt(1024);
        }

        // Unlock the lockfile
        logger.info("LOCKFILE unlock success?: " + client.setLockYes());

        // Command 5 (SUBSET_J)
        if (!client.executeCommand(Command.SUBSET_J, fodder)) {
            logger.error("Failed to execute command: " + Command.SUBSET_J);
            return false;
        }

        // Expect Result TRANSFER_RESPONSE
        msgs = client.receiveMessageGroup();
        dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "TRANSFER_RESPONSE");
        if (!expectedDirective(dir, DirectiveType.RESULT, "TRANSFER_RESPONSE")) {
            return false;
        }

        if(dir.getArgCount() >= 2) echo("Transfer Response: " + dir.getArg(1));
        logger.info(dir);
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkWaitingNoRequire">
    public boolean checkWaitingNoRequire() {
        if (!checkState()) {
            return false;
        }
        if (null == client.getSocket() || !client.getSocket().isConnected()) {
            logger.error("Checking for WAITING but not connected");
            return false;
        }
        if (null == msgs) {
            logger.error("Trying to check monitor messages, but I have none.");
            return false;
        }
        if (msgs.hasDirective(DirectiveType.REQUIRE)) {
            logger.error("Message group contains a REQUIRE when there must "
                    + "be none");
            return false;
        }
        if (!msgs.hasDirective(DirectiveType.WAITING)) {
            logger.error("Message Group contains no WAITING directive when "
                    + "on was expected");
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkState">
    public boolean checkState() {
        if (null == client) {
            logger.error("FATAL: ActiveCLient instance is null");
            return false;
        }
        if (null == config) {
            logger.error("FATAL: config instance is null");
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkConfig">
    public static boolean checkConfig(final ConfigData config) {
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
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkMsgs">
    public boolean checkMsgs() {
        if (null == msgs) {
            logger.error("Trying to check monitor messages, but I have none.");
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="expectedDirective">
    public boolean expectedDirective(Directive dir, DirectiveType dirType, String arg0) {
        if (null == dir) {
            logger.error("Expected directive: " + dirType
                    + " got null instead");
            return false;
        }
        if (dir.getDirectiveType() != dirType) {
            logger.error("I expected a [" + dirType
                    + "] directive, but I got ["
                    + dir.getDirectiveType() + "] instead");
            return false;
        }
        if (null != arg0 && !arg0.equals(dir.getArg())) {
            logger.error("I expected the directive argument to be ["
                    + arg0 + "] but I got [" + dir.getArg() + "] instead");
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkAndSetDirective">
    public boolean checkAndSetDirective(DirectiveType dirType, String arg0) {

        Directive newDir = msgs.getFirstDirectiveOf(dirType, arg0);
        this.dir = null;

        if (null == newDir) {
            logger.error("Expected directive: " + dirType
                    + " got null instead");
            return false;
        }
        if (newDir.getDirectiveType() != dirType) {
            logger.error("I expected a [" + dirType
                    + "] directive, but I got ["
                    + newDir.getDirectiveType() + "] instead");
            return false;
        }
        if (null != arg0 && !arg0.equals(newDir.getArg())) {
            logger.error("I expected the directive argument to be ["
                    + arg0 + "] but I got [" + newDir.getArg() + "] instead");
            return false;
        }
        this.dir = newDir;
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="echo">
    static void echo(String out) {
        System.out.println(PROMPT + out);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="die">
    public static void die() {
        System.exit(1);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="sleep">
    public void sleep() {
        try {
            Thread.currentThread().sleep(sleepTime);
        } catch (InterruptedException ex) {
            logger.error(ex);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="now">
    public static String now() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="panic">
    public void panic() {
        if (null != config) {
            config.addOrSetProperty("die", "True");
            config.save();
            logger.warn("EMERCENGY SERVER SHUTDOWN!!");
            echo("EMERCENGY SERVER SHUTDOWN!!");
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="elapsedTime">
    public long elapsedTime() {
        return System.nanoTime() / 1000000000 - startTime;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="resetClock">
    public void resetClock() {
        startTime = System.nanoTime() / 1000000000;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="writeXferAmt">
    public boolean writeXferAmt(int amount) {
        try {
            String newAmount =
                    Integer.toString(amount + Double.valueOf(amount * 0.01).intValue());
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
            String nextIdent = FROM_IDENT.get(this.identity);
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.SECOND, XFER_DELAY);

            PrintWriter file = new PrintWriter(new FileWriter(XFER_FILE));
            file.println(nextIdent);
            file.println(sdf.format(cal.getTime()));
            file.println(newAmount);
            file.close();
            return true;
        } catch (Exception ex) {
            logger.error("While writing xfer file: " + ex);
            return false;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="writeXferFile">
    public boolean writeXferFail() {
        try {

            PrintWriter file = new PrintWriter(new FileWriter(XFER_FILE));
            file.println("FAIL");
            file.println("2010-12-04 00:00:00");
            file.println("0");
            file.close();
            return true;
        } catch (Exception ex) {
            logger.error("While writing xfer file: " + ex);
            return false;
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkXferFile">
    public int checkXferFile() {
        try {
            int amount = 0;
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
            BufferedReader reader =
                    new BufferedReader(new FileReader(XFER_FILE));

            String ident = reader.readLine().toUpperCase().trim();
            String date = reader.readLine().trim();
            String sAmount = reader.readLine().trim();
            reader.close();

            Date trigger = sdf.parse(date);

            if (identity.equals(ident)) {
                echo(identity + " set to transfer:");
                echo("trigger: " + sdf.format(trigger));
                echo(" system: " + sdf.format(cal.getTime()));
                if (cal.getTime().after(trigger)) {
                    echo("Transfer triggered. Amount: " + sAmount);
                    amount = Integer.parseInt(sAmount);
                    echo("Parsed amount: " + amount);
                }
            }
            return amount;
        } catch (Exception ex) {
            logger.error("While checking xfer file: " + ex);
            return 0;
        }
    }
    // </editor-fold>
}
