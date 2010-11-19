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
 */
public class Directive
{
    public static final int WAITING = 0;
    public static final int REQUIRE = 1;
    public static final int COMMAND_ERROR = 2;
    public static final int COMMENT = 3;
    public static final int RESULT = 4;
    public static final int PARTICIPANT_PASSWORD_CHECKSUM = 5;
    public static final Map<String, Integer> TOKENS;
    public static final String[] NAMES = {
    "WAITING","REQUIRE","COMMAND_ERROR","COMMENT","RESULT",
    "PARTICIPANT_PASSWORD_CHECKSUM"};

    static
    {
        Map<String, Integer> mi = new HashMap<String,Integer>(6);
        mi.put("WAITING", 0);
        mi.put("REQUIRE", 1);
        mi.put("COMMAND_ERROR", 2);
        mi.put("COMMENT", 3);
        mi.put("RESULT", 4);
        mi.put("PARTICIPANT_PASSWORD_CHECKSUM", 5);
        TOKENS = Collections.unmodifiableMap(mi);
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
