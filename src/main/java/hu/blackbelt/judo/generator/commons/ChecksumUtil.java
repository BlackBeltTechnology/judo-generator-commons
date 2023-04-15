package hu.blackbelt.judo.generator.commons;

/*-
 * #%L
 * JUDO Generator commons
 * %%
 * Copyright (C) 2018 - 2023 BlackBelt Technology
 * %%
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the Eclipse
 * Public License, v. 2.0 are satisfied: GNU General Public License, version 2
 * with the GNU Classpath Exception which is
 * available at https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 * #L%
 */

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
