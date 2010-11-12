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

    public Directive getNext( int directive ) {
        Directive d = iter.next();
        while( d.getDirective() != directive ) {
            d = iter.next();
        }
        return d;
    }

    public boolean hasNext() {
        return iter.hasNext();
    }
    
    public void reset() {
        iter = messages.iterator();
    }

    public boolean hasDirective( int directive ) {
        for( Directive d : messages ) {
            if( d.getDirective() == directive ) {
                return true;
            }
        }
        return false;
    }

    public int getCountOfDirective( int directive ) {
        int count = 0;
        for( Directive d : messages ) {
            if( d.getDirective() == directive ) {
                count++;
            }
        }
        return count;
    }

}
