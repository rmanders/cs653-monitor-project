/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package changepassword;

import cs653.*;
import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;


/**
 *
 * @author rmanders
 */
public class ChangePassword {

    private static final String USAGE =
            "Usage: ChangePassword <account name>";

    private static final String TEST1 = "/home/andersr9/cs653/test1/test1.cfg";
    private static final String TEST2 = "/home/andersr9/cs653/test2/test2.cfg";
    private static final String TEST3 = "/home/andersr9/cs653/test3/test3.cfg";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        int newPort = -1;
        SecureRandom secRand = new SecureRandom();
        boolean result = false;
        MessageGroup msgs;
        Directive dir;
        Logger logger;
        String ident = "UNKNOWN";

        // Check args
        if( args.length != 1 ) {
            System.out.println(USAGE);
            System.exit(1);
        }

        // get args into vars
        String acct = args[0].trim().toUpperCase();

        // Determine which config file to open.
        String cfgFile = "";
        if(acct.equals("TEST1")) {
            cfgFile = TEST1;
            ident = "TEST1";
        }
        else if (acct.equals("TEST2")) {
            cfgFile = TEST2;
            ident = "TEST2";
        }
        else if(acct.equals("TEST3")) {
            cfgFile = TEST3;
            ident="TEST3";
        }

        // Make a logger
        logger = Logger.getLogger("CHANGE_PASSWORD_CLIENT [" + ident + "]");

        // Attempt to load the config file quit if fails
        File file = new File(cfgFile);
        if(!file.exists() || !file.canRead()) {
            logger.error("Config file unreadable or does not exist.");
            System.exit(1);
        }

        ConfigData config = ConfigData.getInstance(cfgFile);
        if(null == config) {
            logger.error("FATAL: an error occurred while reading the config file");
            System.exit(1);
        }

        // Create a new Active client with the current config
        ActiveClient client = new ActiveClient(config);
        client.openConnection(config.getProperty("monitorHostname"),
                Integer.parseInt(config.getProperty("monitorPort")));

        System.out.println("initiating login protocol");
        if (!client.login() ) {
            logger.error("monitor login failed. Check the log files");
            client.closeConnection();
            System.exit(1);
        }
        logger.info("monitor login succeeded");

        // CHANGE THE PASSWORD
        String pwd = BigInteger.probablePrime(128, secRand).toString(32);
        String oldPwd = config.getProperty("password");

        logger.info("Trying to CHANGE_PASSWORD from [" + oldPwd + " to [" + pwd + "]");
        result = client.executeCommand(Command.CHANGE_PASSWORD, oldPwd, pwd);
        if(!result) {
            logger.error("Failed to execite Change_password command, exiting with no changes");
            client.closeConnection();
            System.exit(1);
        }

        msgs = client.receiveMessageGroup();
        dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT, "CHANGE_PASSWORD");
        if (null == dir ) {
            logger.error("CHANGE PASSWORD FAILED. NO CHANGES MADE TO FILE");
            client.closeConnection();
            System.exit(1);
        }
        logger.info("CHANGE_PASSWORD SUCCESS: " + dir );
        String cookie = dir.getArg(1);
        logger.info("NEW COOKIE: [" + cookie + "]");

        // Set the config properties and save the config file
        config.setProperty("password", pwd);
        config.setProperty("cookie", cookie);
        config.save();
        logger.info("Configuration File Saved (Better have been!)");

        logger.debug("SENDING QUIT");
        client.sendText("QUIT");
        msgs = client.receiveMessageGroup();
        if( null == msgs){
            logger.warn("Got No Result After Sending Quit.");
        } else {
            dir = msgs.getFirstDirectiveOf(DirectiveType.RESULT,"QUIT");
            if( null == dir ) {
                logger.warn("Got no Result for QUIT");
            } else {
                logger.info("Successfully QUIT session");
            }
        }

        client.closeConnection();
    }


}
