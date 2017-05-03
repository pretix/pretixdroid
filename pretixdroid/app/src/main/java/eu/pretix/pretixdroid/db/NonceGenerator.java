package eu.pretix.pretixdroid.db;


import java.math.BigInteger;
import java.security.SecureRandom;

public final class NonceGenerator {
    private static SecureRandom random = new SecureRandom();

    public static String nextNonce() {
        return new BigInteger(130, random).toString(32);
    }
}