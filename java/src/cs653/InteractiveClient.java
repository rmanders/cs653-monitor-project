/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.Scanner;

/**
 *
 * @author Ryan Anderson
 *
 * Use this to create an active client connection and be able to interactively
 * handle tasks.
 *
 */
public class InteractiveClient  {


    public static void main( String args[] ) {
        String inline = null;
        boolean loop = true;
        Scanner scanIn = new Scanner(System.in);

        while( loop ==  true) {
            inline = scanIn.nextLine();

            if( inline.trim().equals("quit")) {
                loop = false;
            } else {
                System.out.println(">" + inline);
            }

        } // End while
    }

}
