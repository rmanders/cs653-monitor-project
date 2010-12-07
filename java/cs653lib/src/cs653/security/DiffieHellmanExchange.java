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
    private BigInteger secretKey = null;
    private static final Logger logger =
            Logger.getLogger(DiffieHellmanExchange.class);

    private DiffieHellmanExchange(DHKey keys) {
        this.keys = keys;
        generatePublicKey();
    }

    // <editor-fold defaultstate="collapsed" desc="getInstance(1)">
    /**
     * Use this "factory" method to load the DEFAULT key file and instance
     * the exchange class. Hardcoded keys in case of filesystem issues.
     *
     * @return
     */
    public static DiffieHellmanExchange getInstance() {
        //DHKey keys = DHKey.getInflatedInstance("DHKey");
        //if (null == keys) {
        //    logger.error("Error loading DH key info from file: .\\DHKey");
        //    return null;
        //}
        //logger.debug("Successfully Loaded Diffie-Hellman Exchng Keys: " + keys);
         BigInteger p = new BigInteger("7897383601534681724700886135766287333879367007236994792380151951185032550914983506148400098806010880449684316518296830583436041101740143835597057941064647");
	 BigInteger g = new BigInteger("2333938645766150615511255943169694097469294538730577330470365230748185729160097289200390738424346682521059501689463393405180773510126708477896062227281603");
	 DHKey keys = new DHKey(p,g,"C653 DH key");

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
    protected final void generatePublicKey() {
        // Generate our exponent, "a"
        SecureRandom secRan = new SecureRandom();
        a = new BigInteger(keySize, secRan);

        // Generate our public key
        publicKey = keys.getG().modPow(a, keys.getP());
        logger.debug("Generated public key: " + publicKey.toString(32));
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getSecretKey">
    /**
     * Generates the secret key from the public key string sent by the monitor.
     * The secret key is also then stored in the local secretKey member.
     *
     * @param fromMonitor The public key as a string sent by the monitor.
     *
     * @return The secret key
     */
    public BigInteger getSecretKey(final String fromMonitor) {
        BigInteger pkey = null;
        try {
            pkey = new BigInteger(fromMonitor, 32);
        } catch (NumberFormatException ex) {
            logger.error("Unable to convert String[" + fromMonitor
                    + "] to BigInteger.");
            return null;
        }
        secretKey = pkey.modPow(a, keys.getP());
        return secretKey;
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Default get Methods">
    public BigInteger getA() {
        return a;
    }

    public BigInteger getPublicKey() {
        return publicKey;
    }

    public BigInteger getSecretKey() {
        return secretKey;
    }
    // </editor-fold>
}
