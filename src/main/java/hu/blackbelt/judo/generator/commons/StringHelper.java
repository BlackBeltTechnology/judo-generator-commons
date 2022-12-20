package hu.blackbelt.judo.generator.commons;

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
