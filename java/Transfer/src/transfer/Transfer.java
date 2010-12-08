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

    private static final String PROMPT = "%>";

    private static final String TEST1 = "/home/andersr9/cs653/test1/test1.cfg";
    private static final String TEST2 = "/home/andersr9/cs653/test2/test2.cfg";
    private static final String TEST3 = "/home/andersr9/cs653/test3/test3.cfg";

    private static final Logger logger = Logger.getLogger("TRANSFER");

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        boolean result = false;
        String ident = "UNKNOWN";

        // Check Arguments
        if( args.length != 3
                || (!args[1].toUpperCase().equals("TEST1")
                && !args[1].toUpperCase().equals("TEST2")
                && !args[1].toUpperCase().equals("TEST3"))){
            echo(USAGE);
            die();
        }

        // Format and place arguments
        String to = args[0].trim().toUpperCase();
        String from = args[1].trim().toUpperCase();
        String sAmount = args[2];
        int amount = 0;

        // Fail if can't parse int
        try {
            amount = Integer.parseInt(sAmount);
        } catch (NumberFormatException ex ) {
            echo("ERROR: Paramter 3 must be an integer, got: " + sAmount);
            die();
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
        echo("TRANSFER_CLIENT [" + ident + "]");

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
        ActiveClient client = ActiveClient.getInstance(config);
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
        
        logger.info("...AANND WE'RE DONE!");
        client.closeConnection();
    }

    // <editor-fold defaultstate="collapsed" desc="echo">
    static void echo(String out) {
        System.out.println(PROMPT + out);
        logger.debug(out);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="die">
    public static void die() {
        System.exit(1);
    }
    // </editor-fold>

}
