/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

/**
 *
 * Encapsulates and defines commands to be issued to a monitor server
 *
 * @author Ryan Anderson
 */
public enum Command
{
    IDENT("IDENT",1,2),
    QUIT("QUIT",0,0),
    PASSWORD("PASSWORD",1,1),
    CHANGE_PASSWORD("CHANGE_PASSWORD",2,2),
    HOST_PORT("HOST_PORT",2,2),
    SIGN_OFF("SIGN_OFF",0,0),
    ALIVE("ALIVE",1,1),
    GET_GAME_IDENTS("GET_GAME_IDENTS",0,0),
    RANDOM_PARTICIPANT_HOST_PORT("RANDOM_PARTICIPANT_HOST_PORT",0,0),
    PARTICIPANT_HOST_PORT("PARTICIPANT_HOST_PORT",1,1),
    TRANSFER_REQUEST("TRANSFER_REQUEST",4,4),
    TRANSFER_RESPONSE("TRANSFER_RESPONSE",1,1),
    PUBLIC_KEY("PUBLIC_KEY",2,2),
    ROUNDS("ROUNDS",1,1),
    AUTHORIZE_SET("AUTHORIZE_SET",1,100),
    SUBSET_A("SUBSET_A",1,100),
    SUBSET_K("SUBSET_K",1,100),
    SUBSET_J("SUBSET_J",1,100);

    public static final String COMMAND_PATTERNS =
            "(IDENT|QUIT|PASSWORD|CHANGE_PASSWORD|HOST_PORT|SIGN_OFF|ALIVE|"
            + "GET_GAME_IDENTS|RANDOM_PARTICIPANT_HOST_PORT|"
            + "PARTICIPANT_HOST_PORT|TRANSFER_REQUEST|TRANSFER_RESPONSE|"
            + "PUBLIC_KEY|ROUNDS|AUTHORIZE_SET|SUBSET_A|SUBSET_K|SUBSET_J)";

    private String commandString;
    private int minArgs;
    private int maxArgs;

    Command( String commandString, int minArgs, int maxArgs)
    {
        this.commandString = commandString;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    public String getCommandString() {
        return commandString;
    }

    public int getMaxArgs() {
        return maxArgs;
    }

    public int getMinArgs() {
        return minArgs;
    }

    @Override
    public String toString() {
        return commandString;
    }
}
