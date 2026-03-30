package main;

import java.security.SecureRandom;

public class RandomizationUtils {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public static long randomizeDelay(long baseDelayMillis) {
        long entropy = System.nanoTime() ^ System.currentTimeMillis();
        long randomPart = SECURE_RANDOM.nextInt(1000) + (entropy % 1000);
        return baseDelayMillis + randomPart;
    }
}
