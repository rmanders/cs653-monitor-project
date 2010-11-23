/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653.security;

/**
 *
 * @author Ryan Anderson
 * @author Franco?
 *
 * Modified Franco's provided code to add a built-in de-serialization
 * method to load the serialized DHKey object from a file.
 *
 * All this class does is hold the public key values p & g, and allows them
 * to be loaded from a file. We'll pass a loaded DHKey object to our security
 * code in order to generate our secret key.
 *
 */
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Date;
import java.math.BigInteger;


/*
 * This object is used for Public Key Exchange.
 * The Crypto routines require it.  I haven't put the heavy
 * duty methods in here because I want it to stay small
 */

class DHKey implements Serializable {
    
    private BigInteger p;
    private BigInteger g;
    private String description;
    private Date created;

    // <editor-fold defaultstate="collapsed" desc="Constructor(1)">
    DHKey(BigInteger p, BigInteger g, String description) {
        this.p = p;
        this.g = g;
        this.description = description;
        this.created = new Date();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getInflatedInstance">
    /**
     * Use this static method to load(re-inflate) the DHKey object from a file
     *
     * @param filename The filename of the flattened object
     * @return {@link DHKey} Object
     */
    public static DHKey getInflatedInstance(final String filename) {
        try {
            FileInputStream fis = new FileInputStream(filename);
            ObjectInputStream in = new ObjectInputStream(fis);
            DHKey dhkey = (DHKey) in.readObject();
            in.close();
            return dhkey;
        } catch (Exception ex) {
            System.out.println(ex);
            return null;
        }
    }
    // </editor-fold>
   
    // <editor-fold defaultstate="collapsed" desc="toString">
    @Override
    public String toString() {
        StringBuilder scratch = new StringBuilder();
        scratch.append("Public Key(p): ").append(p.toString(32)).append("\n");
        scratch.append("Public Key(g): ").append(g.toString(32)).append("\n");
        scratch.append("Description: ").append(description).append("\n");
        scratch.append("Created: ").append(created);
        return scratch.toString();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Default Get Methods">

    public Date getCreated() {
        return created;
    }

    public String getDescription() {
        return description;
    }

    public BigInteger getG() {
        return g;
    }

    public BigInteger getP() {
        return p;
    }
    // </editor-fold>

}

