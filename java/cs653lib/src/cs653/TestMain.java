/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

/**
 *
 * @author rmanders
 */
public class TestMain {

    public static boolean run = true;

    public static void main( String args[] ) {

        run = true;
        ConfigData config = ConfigData.getInstance("c:\\andersr9.txt");
        ActiveClient client = new ActiveClient("c:\\andersr9.txt");
        Server server = new Server(config);
        server.startServer();
        //client.openConnection("localhost", 8150);
        //client.login();
        //client.closeConnection();
        client.launch();
    }
}
