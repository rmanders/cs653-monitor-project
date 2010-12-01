/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package interactiveclient;

import cs653.*;
import java.io.File;
import java.util.Scanner;
import java.util.StringTokenizer;

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

        //client.doTransfer("TEST2", "TEST3", 10);

        System.out.println("monitor login succeeded");
        System.out.println(MOTD);

        while( loop ==  true) {
            System.out.print("%>");
            String inline = scanIn.nextLine().replace("%>", "");

            String command = inline.trim();
            if( command.equals("exit")) {
                loop = false;
            } 
            else if(command.substring(0, 1).equals("/")) {
                StringTokenizer toker = new StringTokenizer(command," ");
                String cmd = toker.nextToken();
                if( cmd.equals("/transfer") && toker.countTokens() == 3) {
                    String ident1 = toker.nextToken();
                    String ident2 = toker.nextToken();
                    String quant = toker.nextToken();
                    try {
                        int q = Integer.parseInt(quant);
                        client.doTransfer(ident1, ident2, q);
                    } catch (NumberFormatException ex ) {
                        System.out.println("%> last parameter of /trnsfer was not an integer");
                    }
                }
                else
                {
                    System.out.println("%> Invalid console command");
                }
            }
            else {
                
                client.sendText(inline);
                MessageGroup msgs = client.receiveMessageGroup();
                while( msgs.hasNext() ) {
                    System.out.println("%> Monitor said: " + msgs.getNext());
                }
            }
        } // End while

    }
}
