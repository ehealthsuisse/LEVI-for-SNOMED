package ch.ehealth.levi.gui.util;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Utility class for encrypting and decrypting passwords using AES-256
 */
public class EncryptionUtil {
    
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;
    private static final String SALT_PREFIX = "LEVI:";
    
    // Static key for application - in production, this should be more secure
    private static final String SECRET = "LEVI-for-SNOMED-2025-SecretKey";
    private static final byte[] SALT = "LeviSalt".getBytes(StandardCharsets.UTF_8);
    
    /**
     * Encrypts a password using AES-256
     * 
     * @param password the plain text password
     * @return encrypted password as Base64 string
     */
    public static String encrypt(String password) {
        if (password == null || password.isEmpty()) {
            return "";
        }
        
        try {
            SecretKey secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            
            // Generate random IV
            byte[] iv = new byte[16];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and encrypted data
            byte[] combined = new byte[iv.length + encryptedBytes.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encryptedBytes, 0, combined, iv.length, encryptedBytes.length);
            
            return SALT_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting password", e);
        }
    }
    
    /**
     * Decrypts an encrypted password
     * 
     * @param encryptedPassword the encrypted password as Base64 string
     * @return decrypted plain text password
     */
    public static String decrypt(String encryptedPassword) {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            return "";
        }
        
        // Check if password is encrypted
        if (!encryptedPassword.startsWith(SALT_PREFIX)) {
            // Assume it's plain text (for backward compatibility)
            return encryptedPassword;
        }
        
        try {
            String base64Data = encryptedPassword.substring(SALT_PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64Data);
            
            // Extract IV and encrypted data
            byte[] iv = new byte[16];
            byte[] encryptedBytes = new byte[combined.length - 16];
            System.arraycopy(combined, 0, iv, 0, 16);
            System.arraycopy(combined, 16, encryptedBytes, 0, encryptedBytes.length);
            
            SecretKey secretKey = generateKey();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting password", e);
        }
    }
    
    /**
     * Generates a secret key from the application secret
     */
    private static SecretKey generateKey() throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(SECRET.toCharArray(), SALT, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), KEY_ALGORITHM);
    }
}
