/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653.testing;

import cs653.security.DiffieHellmanExchange;
import cs653.security.KarnCodec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.Socket;
import org.apache.log4j.Logger;

/**
 *
 * @author rmanders
 */
public class KarnTestClient {
     private static final Logger logger = Logger.getLogger("[KarnTestClient]");
   public static void main (String arg[]) {
      BufferedReader in;
      PrintWriter out;

      // Use Diffie Hellman to create a shared secret
      DiffieHellmanExchange dhe = null;
      try {
	 dhe = DiffieHellmanExchange.getInstance();
      } catch (Exception e) {
	 System.out.println("Error in getting DHKey from file.");
	 System.exit(1);
      }

      try {
	 // Connect to the server

          logger.debug("Trying to connect to test server on port 8280");
	 Socket connect = new Socket("localhost", 8280);
         logger.debug("Success connecting to server on port 8280");
	 in = new BufferedReader(
                 new InputStreamReader(connect.getInputStream()));
	 out = new PrintWriter(connect.getOutputStream(), true);
         logger.debug("Successfully created IO");

	 // Build a Karn encryptor from the shared secret
         BigInteger pKey = new BigInteger(in.readLine(),32);
         out.println(dhe.getPublicKey());
         KarnCodec karn = KarnCodec.getInstance(pKey);

	 // Encrypt plaintext from the command line and send it to Server
	 String plaintext = "hello there!"; //arg[0];
	 System.out.println("Client: plaintext:"+plaintext+"\n");
	 String ciphertext = karn.encrypt(plaintext);
	 System.out.println("Client: ciphertext:"+ciphertext+"\n");
	 out.println(ciphertext);

         // Leave
      } catch (Exception e) {
	 System.out.println("Yikes!");
      }
   }

}
