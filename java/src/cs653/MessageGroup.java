/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653;

import java.util.List;
import java.util.Iterator;
/**
 *
 * @author Ryan Anderson
 */
public class MessageGroup
{
    private List<Directive> messages = null;
    private Iterator<Directive> iter = null;
    
    public MessageGroup( final List<Directive> messages ) {
        this.messages = messages;
        this.iter = this.messages.iterator();
    }

    public int getMessageCount() {
        return this.messages.size();
    }

    public Directive getNext() {
        return iter.next();
    }

    // <editor-fold defaultstate="collapsed" desc="getNext">
    /**
     *
     * Gets the next directive of DirectiveType or null if it doesn't exist
     *
     * @param directiveType The directive type to search for
     *
     * @return The next instance of Directive or null if none exist.
     */
    public Directive getNext(DirectiveType directiveType) {
        while (iter.hasNext()) {
            Directive dir = iter.next();
            if (dir.getDirectiveType() == directiveType) {
                return dir;
            }
        }
        return null;
    }
    // </editor-fold>

    public boolean hasNext() {
        return iter.hasNext();
    }
    
    public void reset() {
        iter = messages.iterator();
    }

    public boolean hasDirective( DirectiveType directive ) {
        for( Directive d : messages ) {
            if( d.getDirectiveType() == directive ) {
                return true;
            }
        }
        return false;
    }

    public int getCountOfDirective( DirectiveType directive ) {
        int count = 0;
        for( Directive d : messages ) {
            if( d.getDirectiveType() == directive ) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MESSAGE GROUP\n");
        for( Directive dir : messages ) {
            sb.append("\t").append(dir).append("\n");
        }
        return sb.toString();
    }

}
