package hu.blackbelt.judo.generator.commons;

import java.io.*;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChecksumUtil {

    public static String getMD5(byte[] input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            return format(md.digest(input));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could notnget Md5 sum", e);
        }
    }

    public static String getMD5(Path path) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (BufferedInputStream in = new BufferedInputStream((new FileInputStream(path.toFile())));
                 DigestOutputStream out = new DigestOutputStream(OutputStream.nullOutputStream(), md)) {
                in.transferTo(out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return format(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Could not nget Md5 sum", e);
        }
    }

    private static String format(byte[] md) {
        BigInteger number = new BigInteger(1, md);
        String hashtext = number.toString(16);
        // Now we need to zero pad it if you actually want the full 32 chars.
        while (hashtext.length() < 32) {
            hashtext = "0" + hashtext;
        }
        return hashtext;
    }
}