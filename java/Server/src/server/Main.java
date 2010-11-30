/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package server;

import cs653.Server;
import cs653.ConfigData;
import java.io.File;

/**
 *
 * @author Ryan Anderson
 */
public class Main {

    /**
     * @param args the command line arguments
     */

    private static final String USAGE =
            "Usage: Server <configfile>";

    private static final String[] REQ_PROPS =
            {"identity","password",""};

    public static void main(String[] args) {
        
        String cfgFile;

        if( args.length != 1 ) {
            System.out.println(USAGE);
            System.exit(1);
        }

        cfgFile = args[0];
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

        // Get nessecary config file data

        Server server = new Server(config);
        server.run();
        System.out.println("Server Successfully started");
    }

}
