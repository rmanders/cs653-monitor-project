/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package interactiveclient;

import cs653.*;
import java.io.File;
import java.util.Scanner;

/**
 *
 * @author Ryan Anderson
 *
 *
 */
public class Main {

    private static final String USAGE =
            "Usage: InteractiveClient <path and name of config file>";
    private static final String MOTD =
            "\n\n\n ==== InteractiveClient (version 0.0.0.1) ==== \n " +
            "All monitor commands are free-form text. Hit Enter to send.\n" +
            "Type \"exit\" to quit the program\n\n";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        boolean loop = true;
        Scanner scanIn = new Scanner(System.in);

        // Check args
        if( args.length != 1 ) {
            System.out.println(USAGE);
            System.exit(1);
        }

        String cfgFile = args[0];
        File file = new File(cfgFile);
        if(!file.exists() || !file.canRead()) {
            System.out.println("Config file unreadable or does not exist.");
            System.out.println(USAGE);
        }

        ConfigData config = ConfigData.getInstance(cfgFile);
        if(null == config) {
            System.out.println("FATAL: an error occurred while reading the config file");
            System.out.println(USAGE);
        }

        ActiveClient client = new ActiveClient(config);
        client.openConnection(config.getProperty("monitorHostname"),
                Integer.parseInt(config.getProperty("monitorPort")));
        try {
            client.getSocket().setSoTimeout(750000);
        } catch( Exception ex ) {
            System.out.println(ex);
            System.exit(1);
        }

        System.out.println("initiating login protocol");
        if (!client.login() ) {
            System.out.println("monitor login failed. Check the log files");
            System.exit(1);
        }

        System.out.println("monitor login succeeded");
        System.out.println(MOTD);

        while( loop ==  true) {
            System.out.print("%>");
            String inline = scanIn.nextLine().replace("%>", "");

            if( inline.trim().equals("exit")) {
                loop = false;
            } else {
                
                client.sendText(inline);
                MessageGroup msgs = client.receiveMessageGroup();
                while( msgs.hasNext() ) {
                    System.out.println("%> Monitor said: " + msgs.getNext());
                }
            }
        } // End while

    }
}