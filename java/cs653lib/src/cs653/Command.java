/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.regex.Pattern;

/**
 *
 * Encapsulates and defines commands to be issued to a monitor server
 *
 * @author Ryan Anderson
 */
public enum Command
{
    IDENT("IDENT",1,2,"(\\w+)"),
    QUIT("QUIT",0,0,null),
    PASSWORD("PASSWORD",1,1,"(\\w+)"),
    CHANGE_PASSWORD("CHANGE_PASSWORD",2,2,"(\\w+)"),
    HOST_PORT("HOST_PORT",2,2,"(\\w+)\\s+([0-9]+)"),
    SIGN_OFF("SIGN_OFF",0,0,null),
    ALIVE("ALIVE",1,1,null),
    GET_GAME_IDENTS("GET_GAME_IDENTS",0,0,null),
    RANDOM_PARTICIPANT_HOST_PORT("RANDOM_PARTICIPANT_HOST_PORT",0,0,null),
    PARTICIPANT_STATUS("PARTICIPANT_STATUS",0,0,null),
    PARTICIPANT_HOST_PORT("PARTICIPANT_HOST_PORT",1,1,null),
    TRANSFER_REQUEST("TRANSFER_REQUEST",4,4,null),
    TRANSFER_RESPONSE("TRANSFER_RESPONSE",1,1,null),
    PUBLIC_KEY("PUBLIC_KEY",2,2,"\\s*(\\w+\\s+)(\\w+)\\s*"),
    ROUNDS("ROUNDS",1,1,"\\s*(\\d+)\\s*"),
    AUTHORIZE_SET("AUTHORIZE_SET",1,20,null),
    SUBSET_A("SUBSET_A",1,20,"\\s*(\\w+\\s*){1,50}+\\s*"),
    SUBSET_K("SUBSET_K",1,20,"\\s*(\\w+\\s*){1,50}+\\s*"),
    SUBSET_J("SUBSET_J",1,20,"\\s*(\\w+\\s*){1,50}+\\s*"),
    GET_MONITOR_KEY("GET_MONITOR_KEY",0,0,null),
    GET_CERTIFICATE("GET_CERTIFICATE",1,1,null),
    MAKE_CERTIFICATE("MAKE_CERTIFICATE",2,2,null);

    public static final String COMMAND_PATTERNS =
            "(IDENT|QUIT|PASSWORD|CHANGE_PASSWORD|HOST_PORT|SIGN_OFF|ALIVE|"
            + "GET_GAME_IDENTS|RANDOM_PARTICIPANT_HOST_PORT|"
            + "PARTICIPANT_HOST_PORT|TRANSFER_REQUEST|TRANSFER_RESPONSE|"
            + "PUBLIC_KEY|ROUNDS|AUTHORIZE_SET|SUBSET_A|SUBSET_K|SUBSET_J|"
            + "GET_MONITOR_KEY|GET_CERTIFICATE|MAKE_CERTIFICATE)";

    private String commandString;
    private int minArgs;
    private int maxArgs;
    private String resultPattern;

    Command( String commandString, int minArgs, int maxArgs,
            String resultPattern)
    {
        this.commandString = commandString;
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
        this.resultPattern = resultPattern;

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

    public Pattern getResultPattern() {
        if( null == resultPattern) {
            return null;
        }
        try {
            return Pattern.compile(resultPattern);
        } catch (Exception ex) {
            System.err.println(ex);
            return null;
        }
    }

    public boolean hasResultPattern() {
        return (resultPattern != null);
    }

    @Override
    public String toString() {
        return commandString;
    }
}
