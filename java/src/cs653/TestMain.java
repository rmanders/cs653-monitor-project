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
        ActiveClient client = new ActiveClient("localhost",8150,8250,"andersr9","password");
        Server server = new Server(8250,"andersr9","password");
        server.startServer();
        //client.openConnection("localhost", 8150);
        //client.login();
        //client.closeConnection();
        client.launch();
    }
}
