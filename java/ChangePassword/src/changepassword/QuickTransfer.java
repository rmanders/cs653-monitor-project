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
import java.util.Arrays;
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
public class QuickTransfer  {

    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    private static final String USAGE = "Usage: QuickTransfer <start Ident> <start Amount>";
    private static final String PROMPT = "%>";
    private static final long TIMEOUT_MON = 100; //seconds (actual is 180)
    private static final String IDENTITIES[] = {"TEST1","TEST2","TEST3"};
    private static final int MAX_COMMANDS_PER_CLIENT = 109;


    private static final String CONFIG_FILES[] =
    {
        "/home/andersr9/cs653/test1/test1.cfg",
        "/home/andersr9/cs653/test2/test2.cfg",
        "/home/andersr9/cs653/test3/test3.cfg"
    };
/*
    private static final String CONFIG_FILES[] =
    {
        "c:\\test1.cfg",
        "c:\\test2.cfg",
        "c:\\test3.cfg"
    };
*/
    private static final int TEST1 = 0;
    private static final int TEST2 = 1;
    private static final int TEST3 = 2;


    private static final Map<String,String> PREV_IDENT;
    private static final Map<String,String> NEXT_IDENT;
    private static final Map<String,Integer> IDENT_NUM;

    static {
        Map<String,String> m = new HashMap<String,String>(3);
        m.put("TEST1", "TEST3");
        m.put("TEST2", "TEST1");
        m.put("TEST3", "TEST2");
        PREV_IDENT = Collections.unmodifiableMap(m);

        Map<String,String> n = new HashMap<String,String>(3);
        n.put("TEST1", "TEST2");
        n.put("TEST2", "TEST3");
        n.put("TEST3", "TEST1");
        NEXT_IDENT = Collections.unmodifiableMap(n);

        Map<String,Integer> p = new HashMap<String,Integer>(3);
        p.put("TEST1", TEST1);
        p.put("TEST2", TEST2);
        p.put("TEST3", TEST3);
        IDENT_NUM = Collections.unmodifiableMap(p);
    }


    protected ConfigData config[] = null;
    protected ActiveClient client[] = null;
    protected SecureRandom secRand = new SecureRandom();
    protected int sleepTime = 20000;
    protected int maxChanges = -1;
    protected int xferAmount = 0;
    protected final Logger logger;
    protected MessageGroup msgs[] = null;
    protected Directive dir[] = null;
    protected long startTime = 0;
    protected long endTime = 0;
    protected final String startIdent;
    protected int commandCount[] = {0,0,0};

    // <editor-fold defaultstate="collapsed" desc="constructor(1)">
    private QuickTransfer(ConfigData[] config, int startAmount, String startIdent ) {
        this.config = config;
        this.logger = Logger.getLogger("QuickTransfer");
        this.client = new ActiveClient[3];
        this.client[TEST1] = new ActiveClient(this.config[TEST1]);
        this.client[TEST2] = new ActiveClient(this.config[TEST2]);
        this.client[TEST3] = new ActiveClient(this.config[TEST3]);
        this.xferAmount = startAmount;
        this.startIdent = startIdent;
        this.msgs = new MessageGroup[3];
        this.dir = new Directive[3];
    }
    // </editor-fold>        

    // <editor-fold defaultstate="collapsed" desc="getInstance(1)">
    public static QuickTransfer getInstance(ConfigData[] config, int startAmount, String startIdent) {
        if( null == config || config.length != 3) {
            return null;
        }
        if (!checkConfig(config[TEST1]) || !checkConfig(config[TEST2]) || !checkConfig(config[TEST3])) {
            return null;
        }
        return new QuickTransfer(config, startAmount, startIdent);
    }
    // </editor-fold>  

    // <editor-fold defaultstate="collapsed" desc="MAIN">
    public static void main(String[] args) {

        // Check args
        if (args.length != 2) {
            System.out.println(USAGE);
            die();
        }

        // Check start ident
        String sStartIdent = args[0];
        boolean ok = false;
        for(String s: IDENTITIES) {
            if(s.equals(sStartIdent)) {
                ok = true;
            }
        }
        if(!ok) {
            echo("Invalid start identity");
            die();
        }

        int iStartAmount = 0;
        try {
            iStartAmount = Integer.parseInt(args[1]);
        } catch (NumberFormatException ex) {
            echo("2nd parameter <start amount> must be an integer");
            die();
        }


        // Attempt to load the config files quit if fails
        File file;
        ConfigData configs[] = new ConfigData[3];
        for(int i=0; i<3; i++ ) {
            file = new File(CONFIG_FILES[i]);
            if( !file.exists() || !file.canRead()) {
                echo("config file unreadable or does not exist: " +
                        CONFIG_FILES[i]);
                die();
            }
            configs[i] = ConfigData.getInstance(CONFIG_FILES[i]);
            if (null == configs[i]) {
                echo("FATAL: an error occurred while reading the config "
                        + "file:" + CONFIG_FILES[i]);
                die();
            }
        }

        // Make an instance of myself        
        QuickTransfer instance =
                QuickTransfer.getInstance(configs, iStartAmount, sStartIdent);
        if (null == instance) {
            echo("FATAL: couldn't get an instance of myself. Check the logs.");
        }

        // Run the instance
        instance.run();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="run">
    public void run() {

        //fired up and ready to go

        //(0) Before we log in, set up the first transfer
        Integer initiator = IDENT_NUM.get(PREV_IDENT.get(startIdent));
        String sender = startIdent;
        String receiver = NEXT_IDENT.get(startIdent);
        if( null == initiator || null == sender || null == receiver ) {
            echo("Error setting up first transfer variables Exiting...");
            die();
        }

        //(1) open all client connections
        if(!openAllConnections()) {
            die();
        }
        echo("All connections opened....");

        resetClock();

        //(2) Log Everyone in
        if(!logEveryoneIn()) {
            die();
        }

        //(3) Check the time
        if(!checkTimeout()) {
            echo("All the logins took too long! You better manually change "
                    + "passwords and try again later");
            die();
        }
        echo("All clients logged in...");

        //(4) Change Everyone's password
        if(!changeEveryonesPassword()) {
            die();
        }
        echo("All passwords changed...");

        int commands = maxVal(commandCount);

        while(commands < MAX_COMMANDS_PER_CLIENT && elapsedTime() < TIMEOUT_MON) {
            //() Get started transferring
            //echo("testing: " + IDENTITIES[initiator] + " transfers " + Integer.valueOf(xferAmount) + " from " + sender + " to " + receiver);
            //if(false) {
            if(!transfer(initiator, receiver, sender, xferAmount)) {
                echo("Transfer Failed!!");
                if(!changeEveryonesPassword()) {
                    everyonePanic();
                    echo("Failed to change " + IDENTITIES[initiator] + "' "
                            + "password.  ALL SERVERS SHUTDOWN! Check the "
                            + "logs ASAP!");
                    die();
                }

                everyonePanic();
                echo("All Passwords changed. Shutting everything down "
                        + "just to be safe.");
                die();
            }
            if(!changePassword(initiator)) {
                everyonePanic();
                echo("Failed to change " + IDENTITIES[initiator] + "' "
                        + "password.  ALL SERVERS SHUTDOWN! Check the "
                        + "logs ASAP!");
                die();
            }
            commandCount[initiator] += 6;

            //() Set up the next transfer
            xferAmount = amortize(xferAmount);
            initiator = IDENT_NUM.get(NEXT_IDENT.get(IDENTITIES[initiator]));
            sender = NEXT_IDENT.get(sender);
            receiver = NEXT_IDENT.get(receiver);

            commands = maxVal(commandCount);
        }

        //() Shut it all down

        //() Change Everyone's password one last time
        if(!changeEveryonesPassword()) {
            die();
        }
        echo("All passwords changed...");

        //() shutdown the servers
        everyonePanic();
        echo("All Servers shutting down...");

        //() close all connections()
        closeAllConnections();
        echo("All connections closed...");

        echo("Whew! We made it!!!");
    }
    // </editor-fold>
    
    // <editor-fold defaultstate="collapsed" desc="quit">
    public boolean quit(final int ident) {
        debug(ident, "Sending QUIT command");
        
        if (!checkWaitingNoRequire(ident)) {
            return false;
        }        

        boolean result = client[ident].executeCommand(Command.QUIT);
        if (!result) {
            error(ident, "Failed to send the QUIT command!");
            return false;
        }

        msgs[ident] = client[ident].receiveMessageGroup();
        if (!checkMsgs(ident)) {
            return false;
        }

        dir[ident] = msgs[ident].getFirstDirectiveOf(DirectiveType.RESULT, "QUIT");
        if (!expectedDirective(dir[ident], DirectiveType.RESULT, "QUIT")) {
            error(ident, "Did get expected quit result... oh well, "
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
    public boolean changePassword(final int ident) {
        debug(ident, "Trying to change password...");

        // Check state
        if (!checkWaitingNoRequire(ident)) {
            return false;
        }

        // Generate new password
        String pwd = new BigInteger(128, secRand).toString(32);
        String oldPwd = config[ident].getProperty("password");

        logger.info("Trying to CHANGE_PASSWORD from [" + oldPwd + " to [" + pwd + "]");
        boolean result = client[ident].executeCommand(Command.CHANGE_PASSWORD, oldPwd, pwd);
        if (!result) {
            error(ident, "Failed to send the CHANGE_PASSWORD command!");
            return false;
        }

        // Get Results
        msgs[ident] = client[ident].receiveMessageGroup();
        if (!checkWaitingNoRequire(ident)) {
            error(ident, "I send the CHANGE_PASSWORD command but I didn't get "
                    + "the expected reply (WAITING and no REQUIRE. "
                    + "I really got: " + msgs);
            return false;
        }
        dir[ident] = msgs[ident].getFirstDirectiveOf(DirectiveType.RESULT, "CHANGE_PASSWORD");
        if (!expectedDirective(dir[ident], DirectiveType.RESULT, "CHANGE_PASSWORD")) {
            error(ident, "FAILED TO CHANGE PASSWORD!");
            return false;
        }

        String cookie = dir[ident].getArg(1);
        logger.info("SUCCESS CHANGE_PASSWORD: " + dir);
        logger.info("NEW COOKIE: [" + cookie + "]");

        // Set the config properties and save the config file
        config[ident].setProperty("password", pwd);
        config[ident].setProperty("cookie", cookie);
        config[ident].save();
        logger.info("Configuration file saved with new password and cookie");

        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="login">
    public boolean login(final int ident) {

        debug(ident, "Doing monitor login.");

        msgs[ident] = client[ident].receiveMessageGroup();
        if (!checkMsgs(ident)) {
            return false;
        }

        // Expect a request for IDENT
        if (!checkAndSetDirective(ident,DirectiveType.REQUIRE, "IDENT")) {
            return false;
        }

        // Assume to always use encryption
        DiffieHellmanExchange dhe = DiffieHellmanExchange.getInstance();
        if (null == dhe) {
            error(ident, "Could not instance DiffieHellman");
            return false;
        }
        
        client[ident].setDhe(dhe);

        // Execute the ident command
        String identity = config[ident].getProperty("identity");
        String myPublicKey = dhe.getPublicKey().toString(32);
        if (!client[ident].executeCommand(Command.IDENT, identity, myPublicKey)) {
            error(ident, "Failed to execute IDENT command.");
            return false;
        }

        // Encryption should automatically be initialized by the message
        // receiving subsystem
        msgs[ident] = client[ident].receiveMessageGroup();

        // Expect a RESULT IDENT
        if (!checkAndSetDirective(ident,DirectiveType.RESULT, "IDENT")) {
            return false;
        }

        //Expect IDENT result to have the public key of the monitor
        if (dir[ident].getArgCount() != 2) {
            error(ident, "Monitor doesn't want to use encryption! "
                    + "Get out of here!");
            return false;
        }

        // Either require a password or require an alive
        dir[ident] = msgs[ident].getNext(DirectiveType.REQUIRE);

        // If we got a PASSWORD:
        if (dir[ident].getArg().equals("PASSWORD")) {
            if (!client[ident].executeCommand(Command.PASSWORD)) {
                error(ident, "Failed to execute PASSWORD command");
                return false;
            }
            // Expect a cookie here
            msgs[ident] = client[ident].receiveMessageGroup();
            if (!checkAndSetDirective(ident,DirectiveType.RESULT, "PASSWORD")
                    && dir[ident].getArgCount() < 2) {
                error(ident, "Login failed: expected password "
                        + "result with cookie.");
                return false;
            }

            // Store and save the cookie
            config[ident].addOrSetProperty("cookie", dir[ident].getArg(1));
            client[ident].getCONFIG().addOrSetProperty("cookie", dir[ident].getArg(1));
            config[ident].save();

        } else if (dir[ident].getArg().equals("ALIVE")) {
            // If we got an ALIVE:
            if (!client[ident].executeCommand(Command.ALIVE)) {
                error(ident, "Failed to execute ALIVE command");
                return false;
            }
            msgs[ident] = client[ident].receiveMessageGroup();

            if (!checkAndSetDirective(ident,DirectiveType.RESULT, "ALIVE")) {
                error(ident, "Sent Alive but didn't get ALIVE result.");
                return false;
            }

        } else {
            error(ident, "Login failed: expected ALIVE or PASSWORD, got: "
                    + dir);
            return false;
        }

        // See if the monitor ask for HOST_PORT
        msgs[ident].reset();
        dir[ident] = msgs[ident].getNext(DirectiveType.REQUIRE);
        if (null != dir[ident] && dir[ident].getArg().equals("HOST_PORT")) {
            if (!client[ident].executeCommand(Command.HOST_PORT)) {
                error(ident, "Failed to execute HOST_PORT command");
                return false;
            }
            msgs[ident] = client[ident].receiveMessageGroup();
            if (!checkAndSetDirective(ident,DirectiveType.RESULT, "HOST_PORT")) {
                error(ident, "Improper response for HOST_PORT command");
                return false;
            }
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="transfer">
    public boolean transfer(final int ident, String to, String from, int amount) {
        logger.info("Initiating transfer protocol[ from "
                + from + " to " + to + " amount: " + amount + "]");

        if (!checkWaitingNoRequire(ident)) {
            return false;
        }
        Random rand = new Random();

        // Command 1 (TRANSFER_REQUEST)
        if (!client[ident].executeCommand(Command.TRANSFER_REQUEST,
                to, Integer.toString(amount), "FROM", from)) {
            error(ident, "Failed to execute command: "
                    + Command.TRANSFER_REQUEST);
            return false;
        }

        msgs[ident] = client[ident].receiveMessageGroup();
        debug(ident, msgs[ident]);

        // Calulate a bogus Public Key
        BigInteger p = BigInteger.probablePrime(64, rand);
        BigInteger g = BigInteger.probablePrime(64, rand);

        // Command 2 (PUBLIC_KEY)
        if (!client[ident].executeCommand(Command.PUBLIC_KEY,
                p.toString(), g.toString())) {
            error(ident, "Failed to execute command: "
                    + Command.PUBLIC_KEY);
            return false;
        }

        // Expect REQUIRE Authorize set
        msgs[ident] = client[ident].receiveMessageGroup();
        dir[ident] = msgs[ident].getFirstDirectiveOf(DirectiveType.REQUIRE, "AUTHORIZE_SET");
        if (!expectedDirective(dir[ident], DirectiveType.REQUIRE, "AUTHORIZE_SET")) {
            return false;
        }

        // Calcuate a fake authorize set
        String fodder = "7863687 6129879 182987 2131908 289746 12097 "
                + "03947 17364 8763 846";

        // Command 3 (AUTHORIZ_SET)
        if (!client[ident].executeCommand(Command.AUTHORIZE_SET, fodder)) {

            error(ident, "Failed to execute command: " + Command.AUTHORIZE_SET);
            return false;
        }

        // Expect SUBSET_A
        msgs[ident] = client[ident].receiveMessageGroup();
        dir[ident] = msgs[ident].getFirstDirectiveOf(DirectiveType.RESULT, "SUBSET_A");
        if (!expectedDirective(dir[ident], DirectiveType.RESULT, "SUBSET_A")) {
            return false;
        }

        // Compute Subset_K
        Integer subA[] = dir[ident].getArgIntArray(1);
        String subStr = dir[ident].getArg(1);
        if (null == subA) {
            error(ident, "Error getting SUBSET_A arguments from: " + dir);
            return false;
        }

        //Expect REQUIRE SUBSET_K
        dir[ident] = msgs[ident].getFirstDirectiveOf(DirectiveType.REQUIRE, "SUBSET_K");
        if (!expectedDirective(dir[ident], DirectiveType.REQUIRE, "SUBSET_K")) {
            return false;
        }

        // Command 4 (SUBSET_K)
        if (!client[ident].executeCommand(Command.SUBSET_K, subStr)) {
            error(ident, "Failed to execute command: " + Command.SUBSET_K);
            return false;
        }

        // Expect Require SUBSET_J
        msgs[ident] = client[ident].receiveMessageGroup();
        dir[ident] = msgs[ident].getFirstDirectiveOf(DirectiveType.REQUIRE, "SUBSET_J");
        if (!expectedDirective(dir[ident], DirectiveType.REQUIRE, "SUBSET_J")) {
            return false;
        }

        // Calculate Subset J
        int i = (10 - subA.length);
        fodder = "";
        for (int j = 0; j < i; j++) {
            fodder += " " + rand.nextInt(1024);
        }

        // Unlock the lockfile
        logger.info("LOCKFILE unlock success?: " + client[ident].setLockYes());

        // Command 5 (SUBSET_J)
        if (!client[ident].executeCommand(Command.SUBSET_J, fodder)) {
            error(ident, "Failed to execute command: " + Command.SUBSET_J);
            return false;
        }

        // Expect Result TRANSFER_RESPONSE
        msgs[ident] = client[ident].receiveMessageGroup();
        dir[ident] = msgs[ident].getFirstDirectiveOf(DirectiveType.RESULT, "TRANSFER_RESPONSE");
        if (!expectedDirective(dir[ident], DirectiveType.RESULT, "TRANSFER_RESPONSE")) {
            return false;
        }

        if(dir[ident].getArgCount() >= 2) echo("Transfer Response: " + dir[ident].getArg(1));
        logger.info(dir);
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkWaitingNoRequire">
    public boolean checkWaitingNoRequire(final int ident) {
        if (!checkState(ident)) {
            return false;
        }
        if (null == client[ident].getSocket() || !client[ident].getSocket().isConnected()) {
            error(ident, "Checking for WAITING but not connected");
            return false;
        }
        if (null == msgs) {
            error(ident, "Trying to check monitor messages, but I have none.");
            return false;
        }
        if (msgs[ident].hasDirective(DirectiveType.REQUIRE)) {
            error(ident, "Message group contains a REQUIRE when there must "
                    + "be none");
            return false;
        }
        if (!msgs[ident].hasDirective(DirectiveType.WAITING)) {
            error(ident, "Message Group contains no WAITING directive when "
                    + "on was expected");
            return false;
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="checkState">
    public boolean checkState(final int ident) {
        if (null == client[ident]) {
            error(ident, "FATAL: ActiveCLient instance is null");
            return false;
        }
        if (null == config[ident]) {
            error(ident, "FATAL: config instance is null");
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
    public boolean checkMsgs(final int ident) {
        if (null == msgs[ident]) {
            error(ident, "Trying to check monitor messages, but I have none.");
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
    public boolean checkAndSetDirective(final int ident, DirectiveType dirType, String arg0) {

        Directive newDir = msgs[ident].getFirstDirectiveOf(dirType, arg0);
        this.dir[ident] = null;

        if (null == newDir) {
            error(ident, "Expected directive: " + dirType
                    + " got null instead");
            return false;
        }
        if (newDir.getDirectiveType() != dirType) {
            error(ident, "I expected a [" + dirType
                    + "] directive, but I got ["
                    + newDir.getDirectiveType() + "] instead");
            return false;
        }
        if (null != arg0 && !arg0.equals(newDir.getArg())) {
            error(ident, "I expected the directive argument to be ["
                    + arg0 + "] but I got [" + newDir.getArg() + "] instead");
            return false;
        }
        this.dir[ident] = newDir;
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
    public void panic(final int ident) {
        if (null != config[ident]) {
            config[ident].addOrSetProperty("die", "True");
            config[ident].save();
            warn(ident, "EMERCENGY SERVER SHUTDOWN!!");
            echo("EMERCENGY SERVER SHUTDOWN!!");
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="everyonePanic">
    public void everyonePanic() {
        for (int i = 0; i < 3; i++) {
            panic(i);
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

    // <editor-fold defaultstate="collapsed" desc="checkTimeout">
    public boolean checkTimeout() {
        if (elapsedTime() >= TIMEOUT_MON) {
            return false;
        }
        return true;
    }
    // </editor-fold>

    public int amortize(int amount) {
        return amount + Double.valueOf(amount * 0.01).intValue();
    }

    public void debug(final int ident, Object err) {
        logger.debug(" [" +IDENTITIES[ident] + "] " + err.toString());
    }

    public void error(final int ident, Object err) {
        logger.error(" [" +IDENTITIES[ident] + "] " + err.toString());
    }

    public void warn(final int ident, Object err) {
        logger.warn(" [" +IDENTITIES[ident] + "] " + err.toString());
    }
    
    public void info(final int ident, Object err) {
        logger.info(" [" +IDENTITIES[ident] + "] " + err.toString());
    }

    public static int maxVal(int array[]) {
        int max = Integer.MIN_VALUE;
        for(int i : array ) {
            if( i > max ) max = i;
        }
        return max;
    }
    
    // <editor-fold defaultstate="collapsed" desc="openAllConnections">
    public boolean openAllConnections() {
        for (int i = 0; i < 3; i++) {
            if (!client[i].openConnection()) {
                echo("Failed to open a connection for " + IDENTITIES[i]);
                return false;
            }
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="closeAllConnections">
    public void closeAllConnections() {
        for (int i = 0; i < 3; i++) {
            client[i].closeConnection();
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="logEveryoneIn">
    public boolean logEveryoneIn() {
        for (int i = 0; i < 3; i++) {
            if (!login(i)) {
                echo("Couldn't do Monitor login for " + IDENTITIES[i]
                        + "! YOU'RE PROBABLY FUCKED!");
                return false;
            }
        }
        return true;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="changeEveryonesPassword">
    public boolean changeEveryonesPassword() {
        boolean result = true;
        for (int i = 0; i < 3; i++) {
            if (!changePassword(i)) {
                result = false;
                echo("Couldn't change password for " + IDENTITIES[i]
                        + "! YOU'RE PROBABLY FUCKED BIGTIME!");
            }
            commandCount[i]++;
        }
        return result;
    }
    // </editor-fold>
}
