package com.innogrid.tabcloudit.o11ymanager.infrastructure.util;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;

public class ChaCha20Poly3105Util {

    private ChaCha20Poly3105Util() {
        throw new IllegalStateException("Utility class");
    }

    private static final String ALGORITHM = "ChaCha20-Poly1305";
    private static final int KEY_FILE_SIZE = 8192; // 키 파일 크기
    private static final int NONCE_SIZE = 12;
    private static final int KEY_SIZE = 32;
    public static final String KEY_FILE_PATH = "o11y-manager.key";

    public static void generateKeyFile() throws Exception {
        SecureRandom random = new SecureRandom();
        byte[] keyData = new byte[ChaCha20Poly3105Util.KEY_FILE_SIZE];
        random.nextBytes(keyData);

        try (FileOutputStream fos = new FileOutputStream(ChaCha20Poly3105Util.KEY_FILE_PATH)) {
            fos.write(keyData);
        }
    }

    public static byte[] loadKeyFromFile() throws Exception {
        try (FileInputStream fis = new FileInputStream(ChaCha20Poly3105Util.KEY_FILE_PATH)) {
            byte[] keyFileData = new byte[KEY_FILE_SIZE];
            fis.read(keyFileData);

            return Arrays.copyOf(keyFileData, KEY_SIZE);
        }
    }

    public static String encryptString(String plainText) throws Exception {
        byte[] key = loadKeyFromFile();
        byte[] plainData = plainText.getBytes(StandardCharsets.UTF_8);

        byte[] nonce = new byte[NONCE_SIZE];
        new SecureRandom().nextBytes(nonce);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKey secretKey = new SecretKeySpec(key, "ChaCha20");
        IvParameterSpec ivSpec = new IvParameterSpec(nonce);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] ciphertext = cipher.doFinal(plainData);

        ByteBuffer byteBuffer = ByteBuffer.allocate(nonce.length + ciphertext.length);
        byteBuffer.put(nonce);
        byteBuffer.put(ciphertext);

        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    public static String decryptString(String encryptedText) throws Exception {
        byte[] key = loadKeyFromFile();
        byte[] encryptedData = Base64.getDecoder().decode(encryptedText);

        ByteBuffer byteBuffer = ByteBuffer.wrap(encryptedData);
        byte[] nonce = new byte[NONCE_SIZE];
        byteBuffer.get(nonce);

        byte[] ciphertext = new byte[encryptedData.length - NONCE_SIZE];
        byteBuffer.get(ciphertext);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        SecretKey secretKey = new SecretKeySpec(key, "ChaCha20");
        IvParameterSpec ivSpec = new IvParameterSpec(nonce);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);

        byte[] decryptedData = cipher.doFinal(ciphertext);

        return new String(decryptedData, StandardCharsets.UTF_8);
    }
}
