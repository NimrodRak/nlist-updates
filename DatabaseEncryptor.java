import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.DigestInputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class DatabaseEncryptor {
    public static final String REMOTE_AES_KEY = "XXX";
    public static Key k;

    public static void main(String[] args) {
        byte[] bytedKey = new BigInteger(REMOTE_AES_KEY, 16).toByteArray();
        byte[] tmp = new byte[bytedKey.length - 1];
        System.arraycopy(bytedKey, 1, tmp, 0, tmp.length);
        bytedKey = tmp;

        k = new SecretKeySpec(bytedKey, 0, bytedKey.length, "AES");
        encrypt(".\\NList_Android.txt", "");
        decrypt();
    }

    private static void encrypt(String string, String version) {
        if (!new File(string).exists()) {
            System.out.println("Failed to encrypt database: Path doesn't exist.");
            return;
        }
        try {
            String fileName = new File(string).getParent() + "\\EncryptedDB" + version;
            FileInputStream is = new FileInputStream(new File(string));

            Cipher eC = Cipher.getInstance("AES/ECB/PKCS5Padding");
            eC.init(Cipher.ENCRYPT_MODE, k);

            FileOutputStream fOut = new FileOutputStream(fileName);
            CipherOutputStream cOut = new CipherOutputStream(fOut, eC);

            cOut.write(is.readAllBytes());
            cOut.flush();
            cOut.close();

        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IOException e) {
            e.printStackTrace();
        }
    }

    private static void decrypt() {
        try {
            String fileName = ".\\EncryptedDB2";
            InputStream remoteStream = new FileInputStream(fileName);

            String stringedKey = new BigInteger(1, k.getEncoded()).toString(16);
            System.out.println(stringedKey);
            Cipher aes2 = Cipher.getInstance("AES/ECB/PKCS5Padding");
            aes2.init(Cipher.DECRYPT_MODE, k);
            MessageDigest md = MessageDigest.getInstance("MD5");

            CipherInputStream in = new CipherInputStream(remoteStream, aes2);
            DigestInputStream din = new DigestInputStream(in, md);
            BufferedReader reader = new BufferedReader(new InputStreamReader(din, Charset.forName("Windows-1255")));
            String line;
            Set<String> lines = new HashSet<>();
            int duplicates = 0;
            while ((line = reader.readLine()) != null) {
                if (lines.contains(line)) {
                    duplicates++;
                } else {
                    lines.add(line);
                }
                System.out.println(line);
            }
            reader.close();
            if (duplicates > 10) {
                System.err.println("Too many empty lines detected, file format is corrupt!");
                new File(fileName).delete();
            }
        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
    }
}
