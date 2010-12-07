/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package CommandInterpreter;

import cs653.ActiveClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author rmanders
 */
public class CommandInterpreterTest {

    public CommandInterpreterTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void BigAssTest() {
        //ActiveClient client = new ActiveClient("localhost",8150, 8250,"andersr9","password");
        //client.launch();
        Double g = 612686 * .01;
        echo(g.toString());
        echo( Integer.toString(g.intValue()) );
    }

    public static void echo (String out) {
        System.out.println(out);
    }

}