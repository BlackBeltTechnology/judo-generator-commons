## 1. Failing tests (TDD red)

- [x] 1.1 Create `src/test/java/hu/blackbelt/judo/generator/commons/NameSanitizerTest.java` (JUnit 5).
- [x] 1.2 Add tests for `toJavaIdentifier`: null, empty, `my-app` → `my_app`, `1abc` → `_1abc`, `class` → `class_`, `a.b-c d` → `a_b_c_d`.
- [x] 1.3 Add tests for `toJavaPackage`: null, empty, `Acme::my-app` → `acme.my_app`, `MY::sub::dash-here` → `my.sub.dash_here`, `MyApp` → `myapp`.
- [x] 1.4 Add tests for `toJsIdentifier`: null, empty, `my-app` → `my_app`, `1abc` → `_1abc`, `function` → `function_`.
- [x] 1.5 Add tests for `toEnvVarName`: null, empty, `my-app` → `MY_APP`, `acme::my-app::v2` → `ACME__MY_APP__V2`, `1abc` → `_1ABC`.
- [x] 1.6 Add tests for `toFileNameSafe`: null, empty, `acme::my-app` → `acme__my-app`, `with/slash` → `with_slash`, `a*b?c|d` → `a_b_c_d`.
- [x] 1.7 Add a test that asserts the class is `final` and the no-arg constructor is `private` (reflection check).
- [x] 1.8 Run `mvn -pl . clean test`; confirm the new tests fail (RED).

## 2. Implementation (TDD green)

- [x] 2.1 Create `src/main/java/hu/blackbelt/judo/generator/commons/NameSanitizer.java` as a `final` utility class with a private no-arg constructor.
- [x] 2.2 Implement `JAVA_RESERVED_WORDS` and `JS_RESERVED_WORDS` as `Set<String>` constants.
- [x] 2.3 Implement `toJavaIdentifier(String)` per design D3.
- [x] 2.4 Implement `toJavaPackage(String)` per design D4.
- [x] 2.5 Implement `toJsIdentifier(String)` per design D3/D5.
- [x] 2.6 Implement `toEnvVarName(String)` per design D4.
- [x] 2.7 Implement `toFileNameSafe(String)` per design D6.
- [x] 2.8 Add Javadoc on the class and every public method, including the lossy-mapping warning from design D-Risks.
- [x] 2.9 Run `mvn -pl . clean test`; confirm all tests pass (GREEN).

## 3. Verification

- [x] 3.1 Run `mvn -pl . clean verify`; confirm the module builds green end-to-end.
- [x] 3.2 Confirm no new Maven dependencies were added (`git diff pom.xml` is empty).
- [x] 3.3 Confirm `git branch --show-current` prints `feature/JNG-6395_Model_name_including_dash_character`.
- [x] 3.4 Stage only the OpenSpec change folder and the two new Java files; commit with prefix `JNG-6395:`.
