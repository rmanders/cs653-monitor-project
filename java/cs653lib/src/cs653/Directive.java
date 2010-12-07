/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.math.BigInteger;
import org.apache.log4j.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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

    /** Used to help determine proper directive structure */
    private static final Map<String, DirectiveType> TOKENS;

    /** The general regex pattern for matching a directive */
    private static final Pattern DIRECTIVE_PATTERN
            = Pattern.compile("[\\s]*"
            + DirectiveType.ALL_DIRECTIVES + ":(.*)");

    /** Logs messages for the class */
    private static final Logger logger = Logger.getLogger(Directive.class);

    static
    {
        Map<String, DirectiveType> mi = new HashMap<String,DirectiveType>(7);
        mi.put("WAITING", DirectiveType.WAITING);
        mi.put("REQUIRE", DirectiveType.REQUIRE);
        mi.put("COMMAND_ERROR", DirectiveType.COMMAND_ERROR);
        mi.put("COMMENT", DirectiveType.COMMENT);
        mi.put("RESULT", DirectiveType.RESULT);
        mi.put("PARTICIPANT_PASSWORD_CHECKSUM", 
                DirectiveType.PARTICIPANT_PASSWORD_CHECKSUM);
        mi.put("TRANSFER", DirectiveType.TRANSFER);
        TOKENS = Collections.unmodifiableMap(mi);
    }

    private final String message;
    private final DirectiveType directiveType;
    private final String args[];

    private Directive(String message, DirectiveType directiveType, String args[])
    {
        this.message = message;
        this.directiveType = directiveType;
        this.args = args;
    }

    /**
     *
     * Creates a new Directive object from the message parameter. The message
     * parameter is expected to be a directive from the monitor. This method
     * attempts to parse that message, wraps a Directive object around it, and
     * returns that Directive object. If the message does not match the pattern
     * of any known directives, null is returned.
     *
     * @param message A single-line string from the monitor that is expected to
     * be a directive.
     *
     * @return A {@link Directive} object or 
     */
    public static Directive getInstance( final String message )
    {        
        String trimmed = message.trim();
        Matcher matcher = DIRECTIVE_PATTERN.matcher(trimmed);

        if( !matcher.matches() )
        {
            logger.warn("Message string [" + message +
                    "]does not match any known directives: ");
            return null;
        }

        final String dir = matcher.group(1);
        if( !TOKENS.containsKey(dir) )
        {
            logger.error("Message [" + message + "] matched general directive "
                    + "pattern but Directive token [" + dir + "] was not "
                    + "found in known directives token patterns");
            return null;
        }

        // Now try to match the message to the specific type of directive and
        // make sure the message fits that directive's pattern
        DirectiveType dirType = TOKENS.get(dir);
        Matcher dirMatcher = dirType.getPattern().matcher(trimmed);

        if( !dirMatcher.matches() ) {
            logger.warn("The message: [" + message + "] did not match the "
                    + "directive pattern for [" + dirType + "].");
            return null;
        }

        // Get all the directive arguments
        String args[];// = new String[dirMatcher.groupCount()-1];
        //for( int i=0; i<dirMatcher.groupCount()-1; i++ ) {
        //    args[i] = (String)dirMatcher.group(i+2);
        //}

        // Handle arguments for each directive type
        switch(dirType) {
            case WAITING: {
                // Expect no arguments
                args = new String[0];
                return new Directive( trimmed, dirType, args );
            }
            case REQUIRE: {
                // Expect exactly 1 argument
                args = new String[1];
                args[0] = dirMatcher.group(2);
                return new Directive( trimmed, dirType, args );
            }
            case COMMAND_ERROR: {
                // Expect 0 or 1 arguments
                if( null != dirMatcher.group(3) ) {
                    args = new String[1];
                    args[0] = dirMatcher.group(3);
                } else {
                    args = new String[0];
                }
                return new Directive( trimmed, dirType, args );
            }
            case COMMENT: {
                // Expect 0 or 1 arguments
                if( null != dirMatcher.group(3) ) {
                    args = new String[1];
                    args[0] = dirMatcher.group(3);
                } else {
                    args = new String[0];
                }
                return new Directive( trimmed, dirType, args );
            }
            case RESULT: {
                // Expect 1 or 2 arguments
                if( null != dirMatcher.group(4) ) {
                    args = new String[2];
                    args[0] = dirMatcher.group(2);
                    args[1] = dirMatcher.group(4);
                } else {
                    args = new String[1];
                    args[0] = dirMatcher.group(2);
                }
                return new Directive( trimmed, dirType, args );
            }
            case PARTICIPANT_PASSWORD_CHECKSUM: {
                // Expect exactly 1 argument
                args = new String[1];
                args[0] = dirMatcher.group(2);
                return new Directive( trimmed, dirType, args );
            }
            case TRANSFER: {
                //Expect exactly 4 arguments
                args = new String[4];
                args[0] = dirMatcher.group(2);
                args[1] = dirMatcher.group(3);
                args[2] = dirMatcher.group(4);
                args[3] = dirMatcher.group(5);
                return new Directive( trimmed, dirType, args );
            }
            default: {
                return null;
            }
        }

        //return new Directive( trimmed, dirType, args );
    }

    public String getArg()
    {
        if( args.length > 0 ) {
            return args[0];
        } else {
            return "";
        }
    }

    public String getArg( int i ) {
        if( i >= 0 && i < args.length ) {
            return args[i];
        } else {
            return "";
        }
    }

    public int getArgCount() {
        return args.length;
    }

    public DirectiveType getDirectiveType()
    {
        return directiveType;
    }

    public String[] getArgStringArray( int argNo ) {
        if( argNo < 0 || argNo >= args.length ) {
            logger.error("Error in getArgStringArray: Referenced argNo[" +
                    argNo + " but only " + args.length + " args.");
            return null;
        }
        List<String> results =  new LinkedList<String>();
        StringTokenizer toker = new StringTokenizer(args[argNo]," ");
        while( toker.hasMoreTokens() ) {
            results.add(toker.nextToken().trim());
        }
        return results.toArray(new String[results.size()]);
    }

    public Integer[] getArgIntArray( int argNo ) {
        if( argNo < 0 || argNo >= args.length ) {
            logger.error("Error in getArgIntArray: Referenced argNo[" +
                    argNo + " but only " + args.length + " args.");
            return null;
        }
        List<Integer> results =  new LinkedList<Integer>();
        StringTokenizer toker = new StringTokenizer(args[argNo]," ");
        while( toker.hasMoreTokens() ) {
            try {
                Integer i = Integer.parseInt(toker.nextToken().trim());
                results.add(i);
            } catch (NumberFormatException ex ) {
                logger.error("Error in getArgIntArray: Could not parse ["
                        + args[argNo] + " into array of ints.");
                return null;
            }
        }
        return results.toArray(new Integer[results.size()]);
    }

    public BigInteger[] getArgBigIntegerArray( int argNo ) {
        if( argNo < 0 || argNo >= args.length ) {
            logger.error("Error in getArgBigIntegerArray: Referenced argNo[" +
                    argNo + " but only " + args.length + " args.");
            return null;
        }
        List<BigInteger> results =  new LinkedList<BigInteger>();
        StringTokenizer toker = new StringTokenizer(args[argNo]," ");
        while( toker.hasMoreTokens() ) {
            try {
                BigInteger i = new BigInteger(toker.nextToken().trim());
                results.add(i);
            } catch (Exception ex ) {
                logger.error("Error in getArgBigIntegerArray: Could not parse ["
                        + args[argNo] + " into array of BigIntegers: " + ex);
                return null;
            }
        }
        return results.toArray(new BigInteger[results.size()]);
    }

    @Override
    public String toString()
    {
        StringBuilder out = new StringBuilder();
        out.append("{").append(this.directiveType).append("}");
        for( String arg : this.args ) {
            out.append("[").append(arg).append("] ");
        }
        return out.toString();
    }


}
