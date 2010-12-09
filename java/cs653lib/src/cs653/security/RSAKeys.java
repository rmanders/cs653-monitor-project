/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653.security;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import org.apache.log4j.Logger;

/**
 *
 * @author Ryan Anderson
 *
 * Wraps the RSA public and private keys used to authenticate a transaction
 * initiator.
 *
 */
public class RSAKeys {

    private static final Logger logger = Logger.getLogger(RSAKeys.class);
    private static final String KEY_ALG = "RSA";
    private static final int KEY_SIZE = 512;

    protected final BigInteger s;
    protected final BigInteger n;
    protected final BigInteger v;

    private RSAKeys(BigInteger s, BigInteger n, BigInteger v) {
        this.s = s;
        this.n = n;
        this.v = v;
    }
    
    public static RSAKeys getInstance() {
        try {
            KeyFactory keyFac = KeyFactory.getInstance(KEY_ALG);
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance(KEY_ALG);
            keyGen.initialize(KEY_SIZE);
            KeyPair keyPair = keyGen.genKeyPair();
            RSAPublicKeySpec keySpec = keyFac
                    .getKeySpec(keyPair.getPublic(), RSAPublicKeySpec.class);

            BigInteger s = new BigInteger(KEY_SIZE, new SecureRandom());
            BigInteger n = keySpec.getModulus();
            BigInteger v = s.pow(2).mod(n);

            return new RSAKeys(s, n, v);

        } catch (NoSuchAlgorithmException ex) {
            logger.error("Error in RSAKeys factory method getInstance: " + ex);
            return null;
        } catch (InvalidKeySpecException ex) {
            logger.error("In getInstance, Invalid key speficiation: " + ex);
            return null;
        }
    }

    public BigInteger getN() {
        return n;
    }

    public BigInteger getS() {
        return s;
    }

    public BigInteger getV() {
        return v;
    }

}
