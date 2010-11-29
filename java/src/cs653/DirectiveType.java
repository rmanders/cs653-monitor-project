/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.regex.Pattern;

/**
 *
 * @author Ryan Anderson
 *
 * We use this enum to define the types of directives that we can handle.
 * The enum also stores all the info we need to know about each directive. This
 * is used to check to see if plaintext that is supposed to be a mointor
 * directive is indeed a valid directive.
 *
 */
public enum DirectiveType {


    WAITING("WAITING","(WAITING:)",0,0),
    REQUIRE("REQUIRE","^(REQUIRE:)[\\s]+([\\S]+)$",1,1),
    COMMAND_ERROR("COMMAND_ERROR", "^(COMMAND_ERROR:)[\\s](.*)",0,1),
    COMMENT("COMMENT", "^(COMMENT:)[\\s](.*)",0,1),
    RESULT("RESULT", "^(RESULT:)[\\s]" + Command.COMMAND_PATTERNS + "(.*)",2,2),
    PARTICIPANT_PASSWORD_CHECKSUM("PARTICIPANT_PASSWORD_CHECKSUM",
                "^(PARTICIPANT_PASSWORD_CHECKSUM:)[\\s]+([a-zA-Z0-9]+)$",1,1),
    TRANSFER("TRANSFER",
    "^(TRANSFER:)[\\s]+([\\w]+)[\\s]+([\\d]+)[\\s]+([\\w]+)[\\s]+(FROM)"
            + "[\\s]+([\\w]+)", 5,5);

    public static final String ALL_DIRECTIVES =
            "(WAITING|REQUIRE|COMMAND_ERROR|COMMENT|RESULT|"
            + "PARTICIPANT_PASSWORD_CHECKSUM|TRANSFER)";

    private final String name;
    private final Pattern pattern;
    private final int minArgs;
    private final int maxArgs;


    DirectiveType( final String name, final String pattern,
            final int minArgs, final int maxArgs ) {
        this.name = name;
        this.pattern = Pattern.compile(pattern);
        this.minArgs = minArgs;
        this.maxArgs = maxArgs;
    }

    public int getMaxArgs() {
        return maxArgs;
    }

    public int getMinArgs() {
        return minArgs;
    }

    public String getName() {
        return name;
    }

    public Pattern getPattern() {
        return pattern;
    }

}
