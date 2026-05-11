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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Canonical helper for turning arbitrary JSL identifiers (model names, qualified
 * names) into target-specific names: Java identifiers and packages, JavaScript
 * identifiers, POSIX environment variable names, and POSIX-safe file names.
 *
 * <p>All methods are static, pure functions and are null-safe: a {@code null}
 * input returns {@code null}, an empty input returns an empty string. Inputs
 * are assumed to be ASCII (JSL grammar guarantee); Unicode is not supported.
 *
 * <p><b>Lossy mapping warning.</b> The transforms are intentionally lossy
 * (e.g. {@code -}, {@code .}, and {@code /} all collapse to {@code _}). Two
 * distinct JSL names can therefore map to the same target name. Call sites
 * that require uniqueness must enforce it at a higher level.
 *
 * <p>JIRA: JNG-6395 — Allow dash character in JSL model names.
 */
public final class NameSanitizer {

    /** Java SE 21 reserved words and reserved identifiers (incl. {@code true}, {@code false}, {@code null}). */
    private static final Set<String> JAVA_RESERVED_WORDS;

    /** ECMAScript reserved words (current and future-reserved). */
    private static final Set<String> JS_RESERVED_WORDS;

    /** Characters that are unsafe in filenames on at least one of Linux / macOS / Windows. */
    private static final String UNSAFE_FILENAME_CHARS = "/\\:*?\"<>|";

    static {
        JAVA_RESERVED_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch",
                "char", "class", "const", "continue", "default", "do", "double",
                "else", "enum", "extends", "final", "finally", "float", "for",
                "goto", "if", "implements", "import", "instanceof", "int",
                "interface", "long", "native", "new", "package", "private",
                "protected", "public", "return", "short", "static", "strictfp",
                "super", "switch", "synchronized", "this", "throw", "throws",
                "transient", "try", "void", "volatile", "while",
                // Reserved literals
                "true", "false", "null",
                // Restricted identifiers / contextual keywords (Java 21) — safer to suffix.
                "record", "sealed", "permits", "yield", "var", "non-sealed"
        )));

        JS_RESERVED_WORDS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                "break", "case", "catch", "class", "const", "continue", "debugger",
                "default", "delete", "do", "else", "export", "extends", "finally",
                "for", "function", "if", "import", "in", "instanceof", "new",
                "return", "super", "switch", "this", "throw", "try", "typeof",
                "var", "void", "while", "with", "yield",
                // Strict-mode reserved
                "let", "static", "implements", "interface", "package", "private",
                "protected", "public",
                // Future reserved
                "enum", "await",
                // Literal-like identifiers
                "true", "false", "null"
        )));
    }

    private NameSanitizer() {
        throw new UnsupportedOperationException("Utility class — do not instantiate");
    }

    /**
     * Replaces every character outside {@code [A-Za-z0-9_]} with {@code _};
     * prefixes {@code _} if the result starts with a digit; suffixes {@code _}
     * if the result is a Java SE reserved word. Does NOT split on {@code ::} —
     * use {@link #toJavaPackage(String)} for qualified names.
     *
     * @param name input identifier (may be {@code null})
     * @return sanitized Java identifier, or {@code null} if {@code name} is {@code null},
     *         or {@code ""} if {@code name} is empty
     */
    public static String toJavaIdentifier(final String name) {
        if (name == null) {
            return null;
        }
        if (name.isEmpty()) {
            return "";
        }
        String sanitized = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized = "_" + sanitized;
        }
        if (JAVA_RESERVED_WORDS.contains(sanitized)) {
            sanitized = sanitized + "_";
        }
        return sanitized;
    }

    /**
     * Splits the input on the JSL qualifier separator {@code ::}, lowercases each
     * segment, sanitizes each segment with {@link #toJavaIdentifier(String)}, and
     * joins the segments with {@code .}.
     *
     * @param qualifiedName JSL qualified name (may be {@code null})
     * @return Java package path, or {@code null}/{@code ""} mirror of input
     */
    public static String toJavaPackage(final String qualifiedName) {
        if (qualifiedName == null) {
            return null;
        }
        if (qualifiedName.isEmpty()) {
            return "";
        }
        final String[] segments = qualifiedName.split("::", -1);
        final StringBuilder result = new StringBuilder(qualifiedName.length());
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) {
                result.append('.');
            }
            result.append(toJavaIdentifier(segments[i].toLowerCase(Locale.ROOT)));
        }
        return result.toString();
    }

    /**
     * Same character-replacement rules as {@link #toJavaIdentifier(String)} but
     * checks the result against the ECMAScript reserved-word list. Deterministic
     * underscore mapping — no camelCasing.
     *
     * @param name input identifier (may be {@code null})
     * @return sanitized JavaScript identifier
     */
    public static String toJsIdentifier(final String name) {
        if (name == null) {
            return null;
        }
        if (name.isEmpty()) {
            return "";
        }
        String sanitized = name.replaceAll("[^A-Za-z0-9_]", "_");
        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized = "_" + sanitized;
        }
        if (JS_RESERVED_WORDS.contains(sanitized)) {
            sanitized = sanitized + "_";
        }
        return sanitized;
    }

    /**
     * Produces a POSIX environment variable name: replaces {@code ::} with
     * {@code __}, replaces every other character outside {@code [A-Za-z0-9_]}
     * with {@code _}, uppercases the result, and prefixes {@code _} if the
     * result starts with a digit.
     *
     * @param name input identifier (may be {@code null})
     * @return environment variable name
     */
    public static String toEnvVarName(final String name) {
        if (name == null) {
            return null;
        }
        if (name.isEmpty()) {
            return "";
        }
        String sanitized = name.replace("::", "__");
        sanitized = sanitized.replaceAll("[^A-Za-z0-9_]", "_");
        sanitized = sanitized.toUpperCase(Locale.ROOT);
        if (Character.isDigit(sanitized.charAt(0))) {
            sanitized = "_" + sanitized;
        }
        return sanitized;
    }

    /**
     * Produces a POSIX-safe filename. Replaces {@code ::} with {@code __} and
     * the unsafe characters {@code / \ : * ? " < > |} plus ASCII control
     * characters with {@code _}. Dashes and other safe characters are preserved.
     *
     * @param name input identifier (may be {@code null})
     * @return POSIX-safe filename
     */
    public static String toFileNameSafe(final String name) {
        if (name == null) {
            return null;
        }
        if (name.isEmpty()) {
            return "";
        }
        String sanitized = name.replace("::", "__");
        final StringBuilder result = new StringBuilder(sanitized.length());
        for (int i = 0; i < sanitized.length(); i++) {
            final char c = sanitized.charAt(i);
            if (c < 0x20 || UNSAFE_FILENAME_CHARS.indexOf(c) >= 0) {
                result.append('_');
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
