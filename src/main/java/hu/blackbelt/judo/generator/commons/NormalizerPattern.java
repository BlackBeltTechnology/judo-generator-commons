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

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Represents a regex pattern for content normalization.
 * Used to strip language-specific content (e.g., import statements) during comparison.
 */
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder(builderMethodName = "normalizerPatternBuilder")
public class NormalizerPattern {

    /**
     * The regex pattern string.
     */
    private String pattern;

    /**
     * The replacement string (default: empty string for removal).
     */
    @Builder.Default
    private String replacement = "";

    /**
     * Pattern flags (e.g., MULTILINE, DOTALL, CASE_INSENSITIVE).
     */
    @Builder.Default
    private List<String> flags = new ArrayList<>();

    private transient Pattern compiledPattern;

    /**
     * Gets the compiled Pattern instance, lazily compiling it if necessary.
     *
     * @return the compiled Pattern
     */
    public Pattern getCompiledPattern() {
        if (compiledPattern == null && pattern != null) {
            int patternFlags = 0;
            if (flags != null) {
                for (String flag : flags) {
                    switch (flag.toUpperCase()) {
                        case "MULTILINE":
                            patternFlags |= Pattern.MULTILINE;
                            break;
                        case "DOTALL":
                            patternFlags |= Pattern.DOTALL;
                            break;
                        case "CASE_INSENSITIVE":
                            patternFlags |= Pattern.CASE_INSENSITIVE;
                            break;
                        case "UNICODE_CASE":
                            patternFlags |= Pattern.UNICODE_CASE;
                            break;
                        case "COMMENTS":
                            patternFlags |= Pattern.COMMENTS;
                            break;
                        case "UNIX_LINES":
                            patternFlags |= Pattern.UNIX_LINES;
                            break;
                        case "LITERAL":
                            patternFlags |= Pattern.LITERAL;
                            break;
                        case "UNICODE_CHARACTER_CLASS":
                            patternFlags |= Pattern.UNICODE_CHARACTER_CLASS;
                            break;
                    }
                }
            }
            compiledPattern = Pattern.compile(pattern, patternFlags);
        }
        return compiledPattern;
    }

    /**
     * Applies this pattern to the given content, replacing matches with the replacement string.
     *
     * @param content the content to process
     * @return the processed content with matches replaced
     */
    public String apply(String content) {
        if (content == null || pattern == null) {
            return content;
        }
        return getCompiledPattern().matcher(content).replaceAll(replacement != null ? replacement : "");
    }
}
