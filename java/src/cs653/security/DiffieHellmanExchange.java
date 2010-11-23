/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653.security;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.apache.log4j.Logger;

/**
 *
 * @author Ryan Anderson
 *
 */
public class DiffieHellmanExchange {

    private int keySize = 512;
    private DHKey keys = null;
    private BigInteger a = null;
    private BigInteger publicKey = null;
    private static final Logger logger =
            Logger.getLogger(DiffieHellmanExchange.class);

    private DiffieHellmanExchange(DHKey keys) {
        this.keys = keys;
    }

    // <editor-fold defaultstate="collapsed" desc="getInstance(1)">
    /**
     * Use this "factory" method to load the DEFAULT key file and instance
     * the exchange class.
     *
     * @return
     */
    public static DiffieHellmanExchange getInstance() {
        DHKey keys = DHKey.getInflatedInstance("DHKey");
        if (null == keys) {
            logger.error("Error loading DH key info from file: .\\DHKey");
            return null;
        }
        logger.debug("Successfully Loaded Diffie-Hellman Exchng Keys: " + keys);
        return new DiffieHellmanExchange(keys);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getInstance(2)">
    /**
     *
     * Use this "factory" method to load the DH key file from a
     * SPECIFIED LOCATION and create an instance of the DH exchange class.
     *
     * @param filename The file containing the Diffie-Hellman p & g keys
     * @return
     */
    public static DiffieHellmanExchange getInstance(final String filename) {
        DHKey keys = DHKey.getInflatedInstance(filename);
        if (null == keys) {
            logger.error("Error loading DH key info from file: " + filename);
            return null;
        }
        logger.debug("Successfully Loaded Diffie-Hellman Exchng Keys: " + keys);
        return new DiffieHellmanExchange(keys);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="generatePublicKey">
    /**
     * Creates our public key from the dh key file and our generated
     * secret key, "a".
     *
     * @return The generated public key
     */
    public BigInteger generatePublicKey() {
        // Generate our exponent, "a"
        SecureRandom secRan = new SecureRandom();
        a = new BigInteger(keySize, secRan);

        // Generate our public key
        publicKey = keys.getG().modPow(a, keys.getP());
        logger.debug("Generated public key: " + publicKey.toString(32));
        return publicKey;
    }
    // </editor-fold>
}
