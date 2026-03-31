package main;

import java.security.MessageDigest;
// Unused import removed for PMD compliance
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class HmacUtils {
    public static byte[] computeHmacSha256(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(key, "HmacSHA256");
            mac.init(keySpec);
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    public static boolean verifyHmacSha256(byte[] key, byte[] data, byte[] expectedHmac) {
        byte[] actual = computeHmacSha256(key, data);
        return MessageDigest.isEqual(actual, expectedHmac);
    }
}
