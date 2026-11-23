package com.ssafy.live.util;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
/** SQL 더미와 동일한 PBKDF2WithHmacSHA256 형식. */
public final class PasswordUtil {
    private PasswordUtil() {
    }
    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return "pbkdf2_sha256$600000$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, 600000));
    }
    public static boolean verify(String password, String encoded) {
        if (password == null || encoded == null)
            return false;
        try {
            String[] parts = encoded.split("\\$", -1);
            if (parts.length != 4 || !parts[0].equals("pbkdf2_sha256"))
                return false;
            int rounds = Integer.parseInt(parts[1]);
            if (rounds < 10000 || rounds > 2000000)
                return false;
            return MessageDigest.isEqual(Base64.getDecoder().decode(parts[3]),
                    derive(password, Base64.getDecoder().decode(parts[2]), rounds));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    private static byte[] derive(String password, byte[] salt, int rounds) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, rounds, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("비밀번호 해시를 처리할 수 없습니다.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
