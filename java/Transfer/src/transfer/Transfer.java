/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package transfer;

import cs653.*;
import java.io.File;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;
import java.util.Scanner;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import sun.security.util.BigInt;


/**
 *
 * @author rmanders
 */
public class Transfer {

    private static final String USAGE =
            "Usage: Transfer <to> <from> <amount>";
    private static final String MOTD =
            "\n\n\n ==== InteractiveClient (version 0.0.0.1) ==== \n " +
            "All monitor commands are free-form text. Hit Enter to send.\n" +
            "Type \"exit\" to quit the program\n\n";

    private static final String TEST1 = "/home/andersr9/cs653/test1/test1.cfg";
    private static final String TEST2 = "/home/andersr9/cs653/test2/test2.cfg";
    private static final String TEST3 = "/home/andersr9/cs653/test3/test3.cfg";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        SecureRandom secRand = new SecureRandom();
        boolean result = false;
        MessageGroup msgs;
        Directive dir;
        Logger logger;
        String ident = "UNKNOWN";

        // Check args
        if( args.length != 3 ) {
            System.out.println(USAGE);
            System.exit(1);
        }

        // get args into vars
        String to = args[0].trim().toUpperCase();
        String from = args[1].trim().toUpperCase();
        String sAmount = args[2];
        int amount = 0;

        // Fail if can't parse int
        try {
            amount = Integer.parseInt(sAmount);
        } catch (NumberFormatException ex ) {
            System.out.println("ERROR: Paramter 3 must be an integer, got: " +
                    sAmount);
            System.exit(1);
        }

        // Determine which config file to open.
        String cfgFile = "";
        if ( (to.equals("TEST1") && from.equals("TEST2")) || (to.equals("TEST2") && from.equals("TEST1")) ) {
            cfgFile = TEST3;
            ident = "TEST3";
        }
        else if ( (to.equals("TEST1") && from.equals("TEST3")) || (to.equals("TEST3") && from.equals("TEST1")) ) {
            cfgFile = TEST2;
            ident = "TEST2";
        }
        else if ( (to.equals("TEST2") && from.equals("TEST3")) || (to.equals("TEST3") && from.equals("TEST2")) ) {
            cfgFile = TEST1;
            ident = "TEST1";
        } else {
            System.out.println("ERROR: could not figure out which account to initiate transfer from");
            System.exit(1);
        }

        // Make a logger
        logger = Logger.getLogger("TRANSFER_CLIENT [" + ident + "]");

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
        logger.info("Initiating transfer protocol: from " + from + " to " + to + ", Amount: " + amount);
        
        // Try to do a transfer        
        logger.info("Doing a transfer...");
        result = client.doTransfer(to, from, amount);
        if( result ) {
            System.out.println("TRANSFER REQUEST COMPLETED");
        } else {
            System.out.println("TRANSFER REQUEST FAILED, No Changes made, Exiting");
        }


        // CHANGE THE PASSWORD        
        String pwd = new BigInteger(128, secRand).toString(32);
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
        
        logger.info("...AANND WE'RE DONE!");
        client.closeConnection();
    }


}
