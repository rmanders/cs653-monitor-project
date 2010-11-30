/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653.testing;

import cs653.security.DiffieHellmanExchange;
import cs653.security.KarnCodec;
import java.io.*;
import java.math.*;
import java.security.*;
import java.net.*;
import org.apache.log4j.Logger;
/**
 *
 * @author rmanders
 */


public class KarnTestServer {

    private static final Logger logger = Logger.getLogger("[KarnTestServer]");

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
	 // Wait for a connection from a client then connect
	 ServerSocket socket = new ServerSocket(8280);

         logger.debug("Listening on port 8280");
	 Socket connect = socket.accept();
         logger.debug("got a connection");
	 in = new BufferedReader(new InputStreamReader(connect.getInputStream()));
	 out = new PrintWriter (connect.getOutputStream(), true);
         logger.debug("Created IO");

	 // Build a Karn encryptor from the shared secret
	 //Karn karn = new Karn(dhe.getSecret(in, out));
         out.println(dhe.getPublicKey());
         BigInteger pKey = new BigInteger(in.readLine(),32);
         out.println(dhe.getPublicKey());
         KarnCodec karn = KarnCodec.getInstance(pKey);

	 // Receive encrypted message from client and decrypt
	 String ciphertext = in.readLine();
	 System.out.println("Server: ciphertext:"+ciphertext+"\n");
	 String plaintext = karn.decrypt(ciphertext);
	 System.out.println("Server: plaintext:"+plaintext);

	 logger.debug("Exiting test server");
      } catch (Exception e) {
	 System.out.println("Whoops! - no network:" + e);
      }
   }
}
