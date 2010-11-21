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

    public static void main( String args[] ) {

        ActiveClient client = new ActiveClient("localhost",8150, "andersr9","password");
        client.openConnection("localhost", 8150);
        client.login();
        client.closeConnection();
    }
}
