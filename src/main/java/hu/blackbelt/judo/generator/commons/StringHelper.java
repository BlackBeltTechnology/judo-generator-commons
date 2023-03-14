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

import com.google.common.base.CaseFormat;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;

import static com.github.jknack.handlebars.internal.lang3.StringUtils.*;

@TemplateHelper
public class StringHelper extends StaticMethodValueResolver {
    @Override
    public Object resolve(Object context, String name) {
        if (context instanceof String) {
            if ("firstToUpperCase".equals(name)) {
                return capitalize((String) context);
            } else if ("firstToLowerCase".equals(name)) {
                return uncapitalize((String) context);
            }
        }
        return super.resolve(context, name);
    }

    public static String lowerCase(String string) {
        return string.toLowerCase();
    }

    public static String upperCase(String string) {
        return string.toUpperCase();
    }

    public static String camelCaseToSnakeCase(String text) {
        return text != null ? CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, text) : null;
    }

    public static String decorateWithAsterisks(String text) {
        return text.replace("\n", "\n * ");
    }

    public static boolean notEmpty(String text) {
        return text != null && text.trim().length() > 0;
    }

    public static boolean empty(String text) {
        return text == null || text.trim().length() == 0;
    }

    public static boolean isTrue(String text) {
        return Boolean.parseBoolean(text);
    }

    public static String cleanup(String string) {
        return string.replaceAll("[\\n\\t ]", "");
    }

}
