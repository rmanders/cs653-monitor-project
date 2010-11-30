/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package hw2;

import java.io.*;
import java.net.*;

/**
 *
 * @author rmanders
 */
class ConnectionHandler extends MessageParser implements Runnable
{
    private Socket socIncoming;
    private int counter;
    Thread runner;

   public ConnectionHandler (Socket socIncoming, int counter,
           String name, String password)
   {
       super(name, password);
       this.socIncoming = socIncoming;
       this.counter = counter;
   }

   public void run()
   {
       try
       {
           in = new BufferedReader(new InputStreamReader(socIncoming.getInputStream()));
           out = new PrintWriter(socIncoming.getOutputStream(),true);
           boolean done = false;
           HOST_PORT = Server.LOCAL_PORT;
           CType = 1;  //Indicates Server

           System.out.println("Starting login from Server..");
         if (Login()) {
            System.out.println("ConnectionHandler [run]: success Logged In!");
         } else {
	    System.out.println("Server could not log in.");
            if (IsVerified != 1) { }
         }
         socIncoming.close();
      } catch (IOException e) {
      } catch (NullPointerException n) {
      }
   }

   public void start() {
      if (runner == null) {
         runner = new Thread(this);
         runner.start();
      }
   }
}
