package secvault;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.spec.KeySpec;

public class CryptoUtils {
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 10000;
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 16;
    
    // Algoritma murni bawaan Java JDK
    private static final String FACTORY_ALGO = "PBKDF2WithHmacSHA256";
    private static final String CIPHER_ALGO = "AES/CBC/PKCS5Padding";

    /**
     * Mengenkripsi stream data secara on-the-fly.
     * Secara otomatis menyisipkan Salt dan IV di header awal.
     */
    public static void encryptStream(InputStream in, OutputStream out, String password) throws Exception {
        SecureRandom random = new SecureRandom();
        
        // Buat Salt dan IV acak untuk menghindari serangan rainbow table
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        // Tulis Salt dan IV ke awal output stream sebagai header metadata rahasia
        out.write(salt);
        out.write(iv);

        // Bangkitkan SecretKey AES-256
        SecretKey secretKey = generateKey(password, salt);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

        // Bungkus output stream dengan Cipher
        try (CipherOutputStream cos = new CipherOutputStream(out, cipher)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                cos.write(buffer, 0, read);
            }
        }
    }

    /**
     * Mendekripsi stream data. Membaca Salt dan IV dari header awal.
     */
    public static void decryptStream(InputStream in, OutputStream out, String password) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        if (in.read(salt) != SALT_LENGTH) {
            throw new Exception("Format file korup: Salt tidak ditemukan.");
        }
        
        byte[] iv = new byte[IV_LENGTH];
        if (in.read(iv) != IV_LENGTH) {
            throw new Exception("Format file korup: IV tidak ditemukan.");
        }

        // Rekonstruksi SecretKey dari password dan salt yang ada di header
        SecretKey secretKey = generateKey(password, salt);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

        // Dekripsi aliran sisa data
        try (CipherInputStream cis = new CipherInputStream(in, cipher)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = cis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        }
    }

    /**
     * Fungsi KDF (Key Derivation Function) memeras password string menjadi AES 256-bit
     */
    private static SecretKey generateKey(String password, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(FACTORY_ALGO);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), "AES");
    }
}
