/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

/**
 *
 * @author Ryan Anderson
 *
 * @deprecated
 *
 * No longer used. Extended Command enum to handle result patterns*
 *
 */
@Deprecated
public class Result {
    
    /** The command this result is for */
    private final Command command;

    private Result( Command command ) {
        this.command = command;
    }


}
