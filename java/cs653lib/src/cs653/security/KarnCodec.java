/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653.security;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import org.apache.log4j.Logger;

/**
 *
 * @author Ryan Anderson
 *
 * This class essentially does the same stuff as the  Karn class that the
 * course instructor provides. It's encrypts and decrypts text. Add some utility
 * methods for my convenience.
 *
 */
public class KarnCodec {

    public final static int RADIX = 32;
    public final static int PADSIZE = 40;

    private final static MessageDigest md;
    private final static Logger logger = Logger.getLogger(KarnCodec.class);

    static {
        MessageDigest temp = null;
        try {
            temp = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex ) {
            logger.error("FATAL CODEC ERROR - Can't create static instance " +
                    "of MessageDigest SHA - No Such Algorithm");
            temp = null;
        }
        md = temp;
    }

    private byte key[];
    private byte key_left[];
    private byte key_right[];
    private final SecureRandom secRand;


    private KarnCodec( byte key[], byte key_left[], byte key_right[] ) {
        this.key = key;
        this.key_left = key_left;
        this.key_right = key_right;
        this.secRand = new SecureRandom();
    }

    // <editor-fold defaultstate="collapsed" desc="quickSha(1)">
    /**
     * This function is used to get a SHA digest of the input and return it
     * in a hexadecimal string representation.
     *
     * @param input Some string
     * @return SHA digest in hexadecimal format
     */
    public static String quickSha(final String input) {
        MessageDigest lmd = null;
        try {
            lmd = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
            logger.error("FATAL: quickSha failed to get SHA algorithm. All "
                    + "attempts for the monitor to connect to passive server "
                    + "will be rejected!");
            return null;
        }
        lmd.reset();
        lmd.update(input.toUpperCase().getBytes());
        return (new BigInteger(1, lmd.digest())).toString(16);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="quickSha(2)">
    /**
     * This function is used to get a SHA digest of a set of inputs and return
     * it in a hexadecimal string representation.
     *
     * @param inputs an array of some strings
     * @return SHA digest in hexadecimal format
     */
    public static String quickSha(final String... inputs) {
        MessageDigest lmd = null;
        try {
            lmd = MessageDigest.getInstance("SHA");
        } catch (NoSuchAlgorithmException ex) {
            logger.error("FATAL: quickSha failed to get SHA algorithm. All "
                    + "attempts for the monitor to connect to passive server "
                    + "will be rejected!");
            return null;
        }
        lmd.reset();
        if( null == inputs) {
            return null;
        }

        for( String enc : inputs ) {
            lmd.update(enc.toUpperCase().getBytes());
        }
        return (new BigInteger(1, lmd.digest())).toString(16);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="getInstance">
    /**
     * Use this factory method to get an instance of the KarnCodec class
     *
     * @param secretKey The Diffie-Hellman secret key that is shared with the
     * monitor
     * @return an instance of {@link KarnCodec} or null if it could not be
     * instanced.
     *
     */
    public static KarnCodec getInstance(BigInteger secretKey) {
        if (null == md) {
            logger.error("Can't get instance of Codec: MessageDigest was null");
            return null;
        }
        byte key[] = secretKey.toByteArray();

        // Split the secret key as per the specification
        final int size = key.length / 2;

        byte key_left[] = new byte[size];
        byte key_right[] = new byte[size];
        for (int i = 0; i < size; i++) {
            key_left[i] = key[i];
            key_right[i] = key[size + i];
        }
        return new KarnCodec(key, key_left, key_right);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="padString">
    /**
     * Takes a plaintext string, and adds random data in order to pad it such
     * that PADSIZE divides the string length without a remainder.
     *
     * @param plaintext the plaintext to be padded
     * @return the padded bytes of the plaintext string
     */
    protected byte[] padString(String plaintext) {
        logger.debug("Padding String: " + plaintext);
        byte ptBytes[] = plaintext.getBytes();
        int ptLen = plaintext.length();
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        // Write all bytes in plaintext to the outputstream
        // TODO: This seems to make the asumtion that string characters
        // even unicode characters and one byte...this could be dangerous.
        os.write(ptBytes, 0, ptLen);
        os.write(0);

        // If there is padding to be done, fill the space with random stuff
        int padLen = PADSIZE - (ptLen + 1) % PADSIZE;
        byte padJunk[] = new byte[padLen];
        secRand.nextBytes(padJunk);
        os.write(padJunk, 0, padLen);

        return os.toByteArray();
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="stripPadding">
    /**
     * Remove the padding from the plaintext
     * @param ptBytes plaintext byte array
     * @return plaintext string
     */
    protected String stripPadding(final byte[] ptBytes) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        final int ptLen = ptBytes.length;
        int pos = 0;
        while (pos < ptLen && ptBytes[pos] != 0) {
            os.write(ptBytes[pos]);
            pos++;
        }
        return new String(os.toByteArray());
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="encrypt">
    /**
     * Encrypts a plaintext string using the Karn method.
     *
     * @param plaintext text to be encrypted
     *
     * @return cipher text
     */
    public String encrypt(final String plaintext) {
        final int size = PADSIZE / 2;
        byte pt_left[] = new byte[size];
        byte pt_right[] = new byte[size];
        byte ct_left[] = new byte[size];
        byte ct_right[] = new byte[size];
        byte digest[] = new byte[size];
        byte input[] = padString(plaintext);

        int pos = 0;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        // Write the guardbyte
        os.write(42);
        while (pos < input.length) {

            // Split the next text block between left and right
            for (int i = 0; i < size; i++) {
                pt_left[i] = input[pos + i];
                pt_right[i] = input[size + pos + i];
            }

            // Hash the left side
            md.reset();
            md.update(pt_left);
            md.update(key_left);
            digest = md.digest();

            // XOR the digested left side with the right side plaintext to
            // get the right side cipher text
            for (int i = 0; i < size; i++) {
                ct_right[i] = (byte) (digest[i] ^ pt_right[i]);
            }

            // Now get the left side cipher text as per karn
            md.reset();
            md.update(ct_right);
            md.update(key_right);
            digest = md.digest();
            for (int i = 0; i < size; i++) {
                ct_left[i] = (byte) (digest[i] ^ pt_left[i]);
            }

            // Now we can write this encrypted block out and advance to the next
            // block.
            os.write(ct_left, 0, size);
            os.write(ct_right, 0, size);
            pos += PADSIZE;
        }

        // Convert the cipher text to big integer string of radix 32 and return
        BigInteger cipher = new BigInteger(os.toByteArray());
        return cipher.toString(RADIX);
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="decrypt">
    /**
     * Decrypt the ciphertext using karn
     *
     * @param ciphertext the ciphertext to decrypt
     * @return the plaintext message
     */
    public String decrypt(final String ciphertext) {
        // TODO: add checking for incorrect block sizes
        BigInteger cipher = new BigInteger(ciphertext, RADIX);
        final int size = PADSIZE / 2;
        byte pt_left[] = new byte[size];
        byte pt_right[] = new byte[size];
        byte ct_left[] = new byte[size];
        byte ct_right[] = new byte[size];
        byte digest[] = new byte[size];

        // Get the input ciphertext bytes and shave off the first byte
        // which is the guard byte.
        byte input[] = cipher.toByteArray();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        os.write(input, 1, input.length - 1);
        input = os.toByteArray();
        os = null;

        // Set up for decryption using reverse Karn
        int pos = 0;
        os = new ByteArrayOutputStream();

        // Go through each text block
        while (pos < input.length) {

            // Split the current block into left & right
            for (int i = 0; i < size; i++) {
                ct_left[i] = input[pos + i];
                ct_right[i] = input[size + pos + i];
            }

            // Process the right ciphertext
            md.reset();
            md.update(ct_right);
            md.update(key_right);
            digest = md.digest();

            // XOR the digest with the left ciphertext to get the left
            // plaintext
            for (int i = 0; i < size; i++) {
                pt_left[i] = (byte) (digest[i] ^ ct_left[i]);
            }

            // Now get the left plaintext
            md.reset();
            md.update(pt_left);
            md.update(key_left);
            digest = md.digest();

            // XOR the digest with the left ciphertext to get the right
            // plaintext
            for (int i = 0; i < size; i++) {
                pt_right[i] = (byte) (digest[i] ^ ct_right[i]);
            }

            // Write the plaintext to the output stream and advance to the next
            // ciphertext block
            os.write(pt_left, 0, size);
            os.write(pt_right, 0, size);
            pos += PADSIZE;
        }

        String plaintext = stripPadding(os.toByteArray());
        return plaintext;
    }
    // </editor-fold>
}
