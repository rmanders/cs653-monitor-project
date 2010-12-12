/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package cs653.testing;

import java.math.BigInteger;
import java.security.SecureRandom;


/**
 *
 * @author Ryan Anderson
 */
public class MakeSomeKeys {

    public static void main( String args[] ) {

        BigInteger p = BigInteger.probablePrime(2048, new SecureRandom());
        BigInteger q = BigInteger.probablePrime(2048, new SecureRandom());
        BigInteger n = p.multiply(q);
        System.out.println(n.toString(32));
    }

}
