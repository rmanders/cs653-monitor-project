/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653.security;

import java.io.ObjectOutputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Date;
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
public class DHKeyTest {

    public DHKeyTest() {
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
     * Test of getInflatedInstance method, of class DHKey.
     */
    @Test
    public void testDHKeyIO() {
        System.out.println("Testing DHKey Serializable IO");
        String filename = "DHKey.test";
        BigInteger p = new BigInteger("7897383601534681724700886135766287333879367007236994792380151951185032550914983506148400098806010880449684316518296830583436041101740143835597057941064647");
        BigInteger g = new BigInteger("2333938645766150615511255943169694097469294538730577330470365230748185729160097289200390738424346682521059501689463393405180773510126708477896062227281603");
        DHKey expResult = new DHKey(p,g,"C653 DH key");
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(expResult);
      } catch (Exception e) {
	 fail(e.toString());
      }

        DHKey result = DHKey.getInflatedInstance(filename);
        assertEquals(expResult.getG(), result.getG());
        assertEquals(expResult.getP(), result.getP());
        assertEquals(expResult.getDescription(), result.getDescription());
    }
}