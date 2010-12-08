/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.Scanner;
import java.util.StringTokenizer;

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
        ActiveClient client =
                ActiveClient.getInstance("c:\\andersr9.txt");
        client.openConnection("localhost", 8150);
        client.login();

        while( loop ==  true) {
            inline = scanIn.nextLine();

            if( inline.trim().equals("quit")) {
                loop = false;
            } else {
                StringTokenizer toke = new StringTokenizer(inline," ");
                if( toke.hasMoreTokens() ) {
                    Command cmd = makeCommand(toke.nextToken());
                    if( null == cmd ) {
                        System.out.println("Invalid Command");
                        continue;
                    }
                    String bargs[] = new String[toke.countTokens()];
                    int i = 0;
                    while( toke.hasMoreTokens() ) {
                        bargs[i] = toke.nextToken();
                        i++;
                    }
                    client.executeCommand(cmd, bargs);
                    MessageGroup msgs = client.receiveMessageGroup();
                    System.out.println(msgs);
                }
            }

        } // End while
        client.closeConnection();
    }

    public static Command makeCommand( String cmd ) {
        if( cmd.equals("IDENT") ) {
            return Command.IDENT;
        }
        else if (cmd.equals("QUIT")) {
            return Command.QUIT;
        }
        else if (cmd.equals("PASSWORD")) {
            return Command.PASSWORD;
        }
        else if (cmd.equals("CHANGE_PASSWORD")) {
            return Command.CHANGE_PASSWORD;
        }
        else if (cmd.equals("HOST_PORT")) {
            return Command.HOST_PORT;
        }
        else if (cmd.equals("SIGN_OFF")) {
            return Command.SIGN_OFF;
        }
        else if (cmd.equals("ALIVE")) {
            return Command.ALIVE;
        }
            else if (cmd.equals("GET_GAME_IDENTS")) {
            return Command.GET_GAME_IDENTS;
        }
        else if (cmd.equals("RANDOM_PARTICIPANT_HOST_PORT")) {
            return Command.RANDOM_PARTICIPANT_HOST_PORT;
        }
        else if (cmd.equals("PARTICIPANT_HOST_PORT")) {
            return Command.PARTICIPANT_HOST_PORT;
        }
        else {
            return null;
        }
    }
}
