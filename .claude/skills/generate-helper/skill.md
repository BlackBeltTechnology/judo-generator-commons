Create a new Handlebars helper class for JUDO Generator Commons.

**When to use**: When you need to add a new helper function that can be used in Handlebars templates or SpringEL expressions.

**Input**: Describe the helper functionality you need. Include:
- What the helper should do
- Expected input/output
- Example usage

**Steps**

1. **Gather requirements**
   - Ask for helper name if not provided
   - Ask for the transformation/function purpose
   - Clarify input types and edge cases

2. **Determine helper type**
   - **Simple helper**: Transform a single value (most common)
   - **Context-aware helper**: Needs access to template parameters (use @ContextAccessor)

3. **Create the helper class**

   Location: `src/main/java/hu/blackbelt/judo/generator/commons/`

   **Simple helper template:**
   ```java
   package hu.blackbelt.judo.generator.commons;

   import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;

   @TemplateHelper
   public class {{HelperName}}Helper extends StaticMethodValueResolver {

       /**
        * {{description}}
        * @param obj the input value
        * @return the transformed value
        */
       public static {{returnType}} {{methodName}}(Object obj) {
           if (obj == null) {
               return {{nullDefault}};
           }
           // Implementation
           return {{implementation}};
       }
   }
   ```

   **Context-aware helper template:**
   ```java
   package hu.blackbelt.judo.generator.commons;

   import hu.blackbelt.judo.generator.commons.annotations.ContextAccessor;
   import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
   import java.util.Map;

   @TemplateHelper
   @ContextAccessor
   public class {{HelperName}}Helper extends StaticMethodValueResolver {

       public static void bindContext(Map<String, ?> context) {
           ThreadLocalContextHolder.bindContext(context);
       }

       public static synchronized {{returnType}} {{methodName}}(Object obj) {
           {{varType}} {{varName}} = ({{varType}}) ThreadLocalContextHolder.getVariable("{{variableName}}");
           // Implementation using context variable
           return {{implementation}};
       }
   }
   ```

4. **Write tests**

   Location: `src/test/java/hu/blackbelt/judo/generator/commons/`

   ```java
   package hu.blackbelt.judo.generator.commons;

   import org.junit.jupiter.api.Test;
   import static org.hamcrest.MatcherAssert.assertThat;
   import static org.hamcrest.Matchers.*;

   public class {{HelperName}}HelperTest {

       @Test
       public void test{{MethodName}}() {
           // Arrange
           Object input = {{testInput}};

           // Act
           {{returnType}} result = {{HelperName}}Helper.{{methodName}}(input);

           // Assert
           assertThat(result, is({{expectedOutput}}));
       }

       @Test
       public void test{{MethodName}}WithNull() {
           assertThat({{HelperName}}Helper.{{methodName}}(null), is({{nullExpected}}));
       }
   }
   ```

5. **Verify helper discovery**
   - Helper classes are auto-discovered by `TemplateHelperFinder`
   - No registration needed if `@TemplateHelper` annotation is present
   - Run: `mvn test -Dtest={{HelperName}}HelperTest`

6. **Document usage**
   - Show template usage: `{{methodName value}}`
   - Show SpringEL usage: `#methodName(#variable)`

**Output**

Provide:
- The complete helper class implementation
- Test class with edge case coverage
- Usage examples for templates and expressions

**Requirements Checklist**

- [ ] Class annotated with `@TemplateHelper`
- [ ] Class extends `StaticMethodValueResolver`
- [ ] Methods are `public static`
- [ ] Methods have 0 or 1 parameter
- [ ] Null inputs handled gracefully
- [ ] Test coverage for normal and edge cases
- [ ] Context access uses `@ContextAccessor` if needed

**Example**

User: "I need a helper to convert camelCase to SCREAMING_SNAKE_CASE"

Result:
```java
@TemplateHelper
public class CaseHelper extends StaticMethodValueResolver {

    public static String toScreamingSnake(Object obj) {
        if (obj == null) {
            return "";
        }
        String input = obj.toString();
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, input);
    }
}
```

Usage:
- Template: `{{toScreamingSnake propertyName}}`
- SpringEL: `#toScreamingSnake(#self.name)`
