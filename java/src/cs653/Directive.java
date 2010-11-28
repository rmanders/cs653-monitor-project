/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.StringTokenizer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Ryan Anderson
 *
 * This is a wrapper class for a directive that is received from the monitor.
 * The getInstance method is a Directive object factory that takes a single line
 * of plaintext (assumed to come from the monitor) and creates the directive
 * object from that text. If the received text is not a true directive, the
 * getInstance method simply returns null.
 *
 */
public class Directive
{
    public static final int WAITING = 0;
    public static final int REQUIRE = 1;
    public static final int COMMAND_ERROR = 2;
    public static final int COMMENT = 3;
    public static final int RESULT = 4;
    public static final int PARTICIPANT_PASSWORD_CHECKSUM = 5;
    public static final int TRANSFER_REQUEST = 6;
    public static final Map<String, Integer> TOKENS;
    public static final Map<String, String> PATTERNS;
    public static final String[] NAMES = {
    "WAITING","REQUIRE","COMMAND_ERROR","COMMENT","RESULT",
    "PARTICIPANT_PASSWORD_CHECKSUM","TRANSFER_REQUEST"};

    static
    {
        Map<String, Integer> mi = new HashMap<String,Integer>(8);
        mi.put("WAITING", 0);
        mi.put("REQUIRE", 1);
        mi.put("COMMAND_ERROR", 2);
        mi.put("COMMENT", 3);
        mi.put("RESULT", 4);
        mi.put("PARTICIPANT_PASSWORD_CHECKSUM", 5);
        mi.put("TRANSFER_REQUEST", 6);
        TOKENS = Collections.unmodifiableMap(mi);

        // Maps the directive name to it's regex pattern after being trimmed
        Map<String, String> pt = new HashMap<String,String>(8);
        pt.put("WAITING", "(WAITING:)");
        pt.put("REQUIRE", "^(REQUIRE:)[\\s]+([\\S]+)$");
        pt.put("COMMAND_ERROR", "^(COMMAND_ERROR:)[\\s](.*)");
        pt.put("COMMENT", "^(COMMENT:)[\\s](.*)");
        pt.put("RESULT", "^(RESULT:)[\\s]" + Command.COMMAND_PATTERNS + "(.*)");
        pt.put("PARTICIPANT_PASSWORD_CHECKSUM", "");
        pt.put("TRANSFER_REQUEST", "");
        PATTERNS = Collections.unmodifiableMap(pt);
    }

    private final int directive;
    private final String arg;
    private final String payload;
    private final String message;

    private Directive(String message, int directive, String arg, String payload)
    {
        this.message = message;
        this.directive = directive;
        this.arg = arg;
        this.payload = payload;
    }

    public static Directive getInstance( final String message )
    {
        StringTokenizer st = new StringTokenizer(message.trim(), " :");
        if( !st.hasMoreTokens() )
        {
            System.out.println("No tokens in message string");
            return null;
        }
        String cmd = st.nextToken().trim();
        if( !TOKENS.containsKey(cmd) )
        {
            return null;
        }

        String arg;
        String payload;

        switch(TOKENS.get(cmd))
        {
            case (WAITING):
            {
                return new Directive(message, WAITING, null, null);
            }
            case (REQUIRE):
            {
                arg = st.hasMoreTokens() ? st.nextToken().trim() : null;
                if( !isValid(arg) )
                {
                    System.out.println("Invalid Argrument on REQUIRE: " + arg);
                    return null;
                }
                return new Directive(message, REQUIRE, arg, null);
            }
            case (COMMAND_ERROR):
            {
                payload = null;
                if( st.hasMoreTokens() )
                {
                    payload = message.replaceFirst("COMMAND_ERROR([: ])*", "")
                            .trim();
                }
                return new Directive(message, COMMAND_ERROR, null, payload);
            }
            case (COMMENT):
            {
                payload = null;
                if( st.hasMoreTokens() )
                {
                    payload = message.replaceFirst("COMMENT([: ])*", "")
                            .trim();
                }
                return new Directive(message, COMMENT, null, payload);
            }
            case (RESULT):
            {
                arg = st.hasMoreTokens() ? st.nextToken().trim() : null;
                if( !isValid(arg) )
                {
                    System.out.println("Invalid Argrument on RESULT: " + arg);
                    return null;
                }
                payload = null;
                if( st.hasMoreTokens() )
                {
                    payload = message.substring(
                            message.indexOf(arg) + arg.length()).trim();
                }

                return new Directive(message, RESULT, arg, payload);
            }
            case (PARTICIPANT_PASSWORD_CHECKSUM):
            {
                arg = st.hasMoreTokens() ? st.nextToken().trim() : null;
                if( !isValid(arg) )
                {
                    System.out.println("Invalid Argrument on " +
                            "PARTICIPANT_PASSWORD_CHECKSUM: " + arg);
                    return null;
                }
                return new Directive(message, PARTICIPANT_PASSWORD_CHECKSUM,
                        arg, null);
            }
            case (TRANSFER_REQUEST):
            {

            }
            default:
            {
                return null;
            }
        }
    }

    private static boolean isValid(final String str)
    {
        if( null == str || str.trim().length() == 0)
        {
            return false;
        }
        return true;
    }

    public String getArg()
    {
        return arg;
    }

    public int getDirective()
    {
        return directive;
    }

    public String getPayload()
    {
        return payload;
    }

    @Override
    public String toString()
    {
        return "{" + String.valueOf(this.directive) + "}[" +
                NAMES[this.directive] + "] (" + this.arg + ") " +
                this.payload;
    }


}
