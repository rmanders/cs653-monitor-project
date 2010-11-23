/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653.security;

import java.math.BigInteger;
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
public class DiffieHellmanExchangeTest {

    public DiffieHellmanExchangeTest() {
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

    /**
     * Test of getInstance method, of class DiffieHellmanExchange.
     */
    @Test
    public void testGetInstance_0args() {
        System.out.println("[TESTING] ===> getInstance(1)");
        DiffieHellmanExchange result = DiffieHellmanExchange.getInstance();
        assertNotNull(result);
    }

    /**
     * Test of getInstance method, of class DiffieHellmanExchange.
     */
    @Test
    public void testGetInstance_String() {
        System.out.println("[TESTING] ===> getInstance(2)");
        String filename = "DHKey.test";
        DiffieHellmanExchange result = DiffieHellmanExchange.getInstance(filename);
        assertNotNull(result);
    }

    /**
     * Test of generatePublicKey method, of class DiffieHellmanExchange.
     */
    @Test
    public void testGeneratePublicKey() {
        System.out.println("[TESTING] ===> generatePublicKey");
        DiffieHellmanExchange instance = DiffieHellmanExchange.getInstance();
        instance.generatePublicKey();
        System.out.println("\t" + instance.getPublicKey() );
        assertNotNull(instance.getPublicKey());
    }

    /**
     * Test of getSecretKey method, of class DiffieHellmanExchange.
     */
    @Test
    public void testGetSecretKey_String() {
        System.out.println("[TESTING] ===> getSecretKey(1)");
        String fromMonitor = "1m8kvh7f2h0ds98u6ood62o3rmaotk5nc3ckm0umgs2vbcijttceksku2d7sg1tjtj9k8upv4r52t2ihonispnl5i8ubcu90o56q92c";
        DiffieHellmanExchange instance = DiffieHellmanExchange.getInstance();
        BigInteger result = instance.getSecretKey(fromMonitor);
        System.out.println("\tComputed Secret Key: " + result.toString(32));
        assertNotNull(result);
    }

    /**
     * Test of getA method, of class DiffieHellmanExchange.
     */
    @Test
    public void testGetA() {
        System.out.println("[TESTING] ===> getA");
        DiffieHellmanExchange instance = DiffieHellmanExchange.getInstance();
        BigInteger result = instance.getA();
        assertNotNull(result);
    }

    /**
     * Test of getPublicKey method, of class DiffieHellmanExchange.
     */
    @Test
    public void testGetPublicKey() {
        System.out.println("[TESTING] ===> getPublicKey");
        DiffieHellmanExchange instance = DiffieHellmanExchange.getInstance();
        BigInteger result = instance.getPublicKey();
        assertNotNull(result);
    }

    /**
     * Test of getSecretKey method, of class DiffieHellmanExchange.
     */
    @Test
    public void testGetSecretKey_0args() {
        System.out.println("[TESTING] ===> getSecretKey");
        String fromMonitor = "1m8kvh7f2h0ds98u6ood62o3rmaotk5nc3ckm0umgs2vbcijttceksku2d7sg1tjtj9k8upv4r52t2ihonispnl5i8ubcu90o56q92c";
        DiffieHellmanExchange instance = DiffieHellmanExchange.getInstance();
        assertEquals(null,instance.getSecretKey());
        BigInteger result1 = instance.getSecretKey(fromMonitor);
        BigInteger result2 = instance.getSecretKey();
        assertEquals(result1,result2);
    }

}