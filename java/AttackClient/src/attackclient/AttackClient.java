/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package attackclient;
import cs653.*;

/**
 *
 * @author Ryan Anderson
 */
public class AttackClient {

    private static final String USAGE =
            "Usage: AttackClient <Ident> <Password> <AliveCookie>\n" +
            "Usage: AttackClient <Ident> <Password> <AliveCookie> "
            + "<command1;command2;...;>\n";

    private static final String MOTD =
            "\n\n\n ==== AttackClient (version 0.1) ==== \n " +
            "All monitor commands are free-form text. Hit Enter to send.\n" +
            "Assuming monitor: gauss.ececs.uc.edu: 8150" +
            "Type \"QUIT\" to quit the program\n\n";

    private static final String PRM = "%>";

    public static void main(String[] args) {

        // Check args
        if( args.length != 3 || args.length != 4) {
            System.out.println(USAGE);
            System.exit(1);
        }

        // get args into vars
        String ident = args[0].trim().toUpperCase();
        String password = args[1].trim().toUpperCase();
        String cookie = args[2].trim().toUpperCase();

        // Build a config
        ConfigData config = new ConfigData();
        config.addProperty("identity", ident);
        config.addProperty("password", password);
        config.addProperty("cookie", cookie);
        config.addProperty("monitorHostname", "gauss.ececs.uc.edu");
        config.addProperty("monitorPort", "8150");
        config.addProperty("serverHostname", "unknown");
        config.addProperty("serverPort", "2100");

        // Get connected
        ActiveClient client = new ActiveClient(config);
        console("Connecting...");
        boolean result = client.openConnection();

        if(!result) {
            console("Unable to establish a connection with the montior.");
            console("Check the logs. Exiting...");
            die();
        }

        // Receive messages
        MessageGroup msgs = client.receiveMessageGroup();
        checkMsgsOrDie(msgs);
        outputMsgs(msgs);

        msgs.reset();
        Directive dir = msgs
                .getFirstDirectiveOf(DirectiveType.REQUIRE, "IDENT");
        checkDirOrDie(dir,client);


        
    }

    public static void die() {
        System.exit(1);
    }
    public static void fromMon(String out) {
        console("Received: " + out);
    }

    public static void console(String out) {
        System.out.println(PRM + out);
    }

    public static void prompt() {
        System.out.print(PRM);
    }

    public static void checkMsgsOrDie(MessageGroup msgs) {
        if( null == msgs) {
            console("Monitor didn't send anything, exiting...");
            die();
        }
    }

    public static void outputMsgs(MessageGroup msgs) {
        if(null == msgs) return;
        while(msgs.hasNext()) {
            Directive dir = msgs.getNext();
            String line =
            dir.getDirectiveType().getName() + ": ";
            for(int i=0; i<dir.getArgCount(); i++ ) {
                line += dir.getArg(i);
            }
            fromMon(line);
        }
    }

    public static void checkDirOrDie(Directive dir, ActiveClient client ) {
        if( null == dir ) {
            console("A required directive was not received. Exiting.");
            die();
        }
    }

}
