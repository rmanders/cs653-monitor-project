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
public class KarnCodecTest {


    public KarnCodecTest() {
    }



    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        System.out.println("SETUP");
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getInstance method, of class KarnCodec.
     */
    @Test
    public void testKarn() {
        System.out.println("getInstance");

        DiffieHellmanExchange client_dh = DiffieHellmanExchange.getInstance();
        DiffieHellmanExchange server_dh = DiffieHellmanExchange.getInstance();
        String client_public = client_dh.getPublicKey().toString(32);
        String server_public = server_dh.getPublicKey().toString(32);
       
        // Simulate Public key exchange
        BigInteger server_secret = server_dh.getSecretKey(client_public);
        BigInteger client_secret = client_dh.getSecretKey(server_public);

        System.out.println("Server's Secret Key: " + server_secret);
        System.out.println("Client's Secret Key: " + client_secret);
        assertEquals(server_secret,client_secret);

        // OK so far, now set up KarnCodec for the server and client
        KarnCodec client_karn = KarnCodec.getInstance(client_secret);
        KarnCodec server_karn = KarnCodec.getInstance(server_secret);

        String plaintext = "Can you hear me?";
        System.out.println("Plaintext: " + plaintext);

        String ciphertext = client_karn.encrypt(plaintext);
        System.out.println("Ciphertext: " + ciphertext);

        String decrypted_plaintext = server_karn.decrypt(ciphertext);
        System.out.println("Decrypted Plaintext: " + decrypted_plaintext);

        assertEquals(plaintext,decrypted_plaintext);
        
    }


}