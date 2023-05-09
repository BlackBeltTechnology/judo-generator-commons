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

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.io.URLTemplateLoader;
import com.google.common.base.Charsets;
import com.google.common.io.ByteStreams;
import hu.blackbelt.epsilon.runtime.execution.api.Log;
import hu.blackbelt.epsilon.runtime.execution.impl.BufferedSlf4jLogger;
import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class loads descriptor yaml file and processing it.
 * The yaml file contains a entries describes the generation itself. On entry can
 * be used to generate several entries of files. @see {@link GeneratorTemplate}
 */
@Slf4j
public class ModelGenerator<M> {
    public static final String NEWLINE = System.getProperty("line.separator");

    public static final String GENERATED_FILES = ".generated-files";

    public static final String NAME = "name";
    public static final Boolean TEMPLATE_DEBUG = System.getProperty("templateDebug") != null;
    public static final String YAML = ".yaml";

    public static GeneratedFile generateFile(
            final ModelGeneratorContext generatorContext,
            final StandardEvaluationContext evaluationContext,
            final TemplateEvaulator templateEvaulator,
            final GeneratorTemplate generatorTemplate,
            final Context.Builder contextBuilder,
            final Log log) {


        GeneratedFile generatedFile = new GeneratedFile();
        boolean condition = true;
        if (templateEvaulator.getConditionExpression() != null) {
            try {
                condition = templateEvaulator.getConditionExpression().getValue(evaluationContext, Boolean.class);
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not evaluate condition expression in " + generatorTemplate.toString());
            }
        }
        generatedFile.setCondition(condition);

        try {
            generatedFile.setPath(templateEvaulator.getPathExpression().getValue(evaluationContext, String.class));
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not evaluate path expression in " + generatorTemplate.toString());
        }

        if (generatorTemplate.isCopy()) {
            String location = generatorTemplate.getTemplateName();
            if (location.startsWith("/")) {
                location =  location.substring(1);
            }
            location = generatorContext.getTemplateLoader().resolve(location);
            try {
                URL resource = generatorContext.getUrlResolver().getResource(location);
                if (resource != null) {
                    generatedFile.setContent(ByteStreams.toByteArray(resource.openStream()));
                }  else {
                    log.error("Could not locate: " + location);
                }
            } catch (Exception e) {
                log.error("Could not resolve: " + location);
            }
        } else {
            StringWriter sourceFile = new StringWriter();
            try {
                Context context = contextBuilder.build();
                callBindContextForTypeIfCan(generatorContext, Context.class, context);
                templateEvaulator.getTemplate().apply(context, sourceFile);
            } catch (Exception e) {
                throw new RuntimeException("Could not generate file: " + generatedFile.getPath(), e);
            }
            generatedFile.setContent(sourceFile.toString().getBytes(Charsets.UTF_8));
        }

        String permissions = null;
        if (generatorTemplate.getPermission() != null) {
            permissions = generatorTemplate.getPermission();
        } else if (generatorContext.getGeneratorModel().getPermission() != null) {
            permissions = generatorContext.getGeneratorModel().getPermission();
        }
        if (permissions != null) {
            try {
                Set<PosixFilePermission> posixFilePermissions = PosixFilePermissions.fromString(permissions);
                generatedFile.setPermissions(posixFilePermissions);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Illegal permission for: " + generatedFile.getPath() + " Permission: " + permissions);
            }
        }

        return generatedFile;
    }

    public static <D> Consumer<Map.Entry<D, Collection<GeneratedFile>>> getDirectoryWriterForActor(
            Function<D, File> actorTypeTargetDirectoryResolver,
            Function<D, String> actorTypeNameResolver,  boolean validateChecksum, Log log) {
        return e -> writeDirectory(e.getValue(), actorTypeTargetDirectoryResolver.apply(e.getKey()), GENERATED_FILES + "-" + actorTypeNameResolver.apply(e.getKey()), validateChecksum);

    }

    public static Consumer<Collection<GeneratedFile>> getDirectoryWriter(Supplier<File> targetDirectoryResolver, boolean validateChecksum, Log log) {
        return e -> writeDirectory(e, targetDirectoryResolver.get(), GENERATED_FILES, validateChecksum);
    }

    public static void writeDirectory(Collection<GeneratedFile> generatedFiles, File targetDirectory, String generatorFilesName, boolean validateChecksum) {
        Collection<GeneratedFile> generatedFilesFilteredWithCondition = generatedFiles.stream().filter(f -> f.isCondition()).collect(Collectors.toList());

        GeneratorIgnore generatorIgnore = new GeneratorIgnore(targetDirectory.toPath());
        Collection<GeneratorFileEntry> generatorFileEntryCollection = getGeneratorFiles(generatedFilesFilteredWithCondition);
        Collection<GeneratorFileEntry> savedFileEntryCollection = readGeneratedFiles(targetDirectory, generatorFilesName);
        Collection<GeneratorFileEntry> filesystemFileEntryCollection = readFilesystemEntries(targetDirectory, savedFileEntryCollection);

        Map<String, GeneratorFileEntry> generatorFileEntryMap = generatorFileEntryCollection.stream()
                .collect(Collectors.toMap(GeneratorFileEntry::getPath, v -> v, (a1, a2) -> a1));
        Map<String, GeneratorFileEntry> savedFileEntryMap = savedFileEntryCollection.stream()
                .collect(Collectors.toMap(GeneratorFileEntry::getPath, v -> v, (a1, a2) -> a1));
        Map<String, GeneratorFileEntry> filesystemFileEntryMap = filesystemFileEntryCollection.stream()
                .collect(Collectors.toMap(GeneratorFileEntry::getPath, v -> v, (a1, a2) -> a1));

        // Delete files which is presented in the saved entry collection and presented in the filesystem, but it's not presented
        // in the saved collection
        filesystemFileEntryCollection.parallelStream()
                .filter(f -> !generatorFileEntryMap.containsKey(f.getPath()))
                .filter(f -> !generatorIgnore.shouldExcludeFile(new File(targetDirectory, f.getPath()).toPath()))
                .forEach(f -> new File(targetDirectory, f.getPath()).delete());

        if (validateChecksum) {
            // Check files where filesystem checksum does not match with the last generated ones and it's not ignored.
            List<GeneratorFileEntry> checksumMismatchInFilesystem = filesystemFileEntryCollection.stream()
                    .filter(f -> savedFileEntryMap.containsKey(f.getPath()))
                    .filter(f -> !f.getChecksum().equals(savedFileEntryMap.get(f.getPath()).getChecksum()))
                    .filter(f -> !generatorIgnore.shouldExcludeFile(new File(targetDirectory, f.getPath()).toPath()))
                    .collect(Collectors.toList());

            if (checksumMismatchInFilesystem.size() > 0) {
                throw new IllegalStateException("There are manual changes in the generated files.\n" +
                        "Please discard the changes, delete file or put them to .generator-ignore:\n\t" +
                        String.join("\n\t", checksumMismatchInFilesystem.stream()
                                .map(f -> f.getPath()).collect(Collectors.toList())));
            }
        }

        // Check files where generated checksum does not match with the last generated ones.
        Map<String, GeneratorFileEntry> haveToGenerate = generatorFileEntryCollection.stream()
                .filter(f ->
                        !filesystemFileEntryMap.containsKey(f.getPath()) ||
                        !savedFileEntryMap.containsKey(f.getPath()) ||
                                !(savedFileEntryMap.containsKey(f.getPath())
                                        && savedFileEntryMap.get(f.getPath()).getChecksum().equals(f.getChecksum())))
                .filter(f -> !generatorIgnore.shouldExcludeFile(new File(targetDirectory, f.getPath()).toPath()))
                .collect(Collectors.toMap(GeneratorFileEntry::getPath, v -> v, (a1, a2) -> a1));

        generatedFilesFilteredWithCondition.parallelStream()
                .filter(f -> haveToGenerate.containsKey(f.getPath()))
                .forEach(f -> writeFile(targetDirectory, generatorIgnore, f));

        writeGeneratedFiles(targetDirectory, generatorFileEntryCollection, generatorFilesName);
    }


    public static <D> Consumer<D> getDirectoryChecksumCalculatorForActor(
            Function<D, File> actorTypeTargetDirectoryResolver,
            Function<D, String> actorTypeNameResolver) {
        return e -> recalculateChecksumToDirectory(actorTypeTargetDirectoryResolver.apply(e), GENERATED_FILES + "-" + actorTypeNameResolver.apply(e));

    }

    public static Runnable getDirectoryChecksumCalculator(Supplier<File> targetDirectoryResolver) {
        return () -> recalculateChecksumToDirectory(targetDirectoryResolver.get(), GENERATED_FILES);
    }

    public static void recalculateChecksumToDirectory(File targetDirectory, String generatorFilesName) {
        Collection<GeneratorFileEntry> savedFileEntryCollection = readGeneratedFiles(targetDirectory, generatorFilesName);
        Collection<GeneratorFileEntry> filesystemFileEntryCollection = readFilesystemEntries(targetDirectory, savedFileEntryCollection);
        writeGeneratedFiles(targetDirectory, filesystemFileEntryCollection, generatorFilesName);
    }

    public static List<GeneratorFileEntry> getGeneratorFiles(Collection<GeneratedFile> generatedFiles) {
        ArrayList<GeneratorFileEntry> result = new ArrayList();
        result.addAll(generatedFiles.stream().map(
                        f -> GeneratorFileEntry.generatorFileEntry()
                                .path(f.getPath())
                                .checksum(ChecksumUtil.getMD5(f.getContent())).build())
                .collect(Collectors.toList()));

        Collections.sort(result);
        return result;
    }

    private static void writeFile(File targetDirectory, GeneratorIgnore generatorIgnore, GeneratedFile generatedFile) {
        File outFile = new File(targetDirectory, generatedFile.getPath());
        outFile.getParentFile().mkdirs();

        try {
            if (outFile.exists()) {
                log.debug("File already exists, overwrite: " + outFile.getAbsolutePath());
                outFile.delete();
            }
            ByteStreams.copy(new ByteArrayInputStream(generatedFile.getContent()), new FileOutputStream(outFile));
            if (generatedFile.getPermissions() != null) {
                if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
                    Files.setPosixFilePermissions(outFile.toPath(), generatedFile.getPermissions());
                } else {
                    if (generatedFile.getPermissions().contains(PosixFilePermission.OWNER_EXECUTE)) {
                        outFile.setExecutable(true,
                                !(generatedFile.getPermissions().contains(PosixFilePermission.GROUP_EXECUTE)
                                        || generatedFile.getPermissions().contains(PosixFilePermission.OTHERS_EXECUTE)));
                    }
                    if (generatedFile.getPermissions().contains(PosixFilePermission.OWNER_READ)) {
                        outFile.setReadable(true,
                                !(generatedFile.getPermissions().contains(PosixFilePermission.GROUP_READ)
                                        || generatedFile.getPermissions().contains(PosixFilePermission.OTHERS_READ)));
                    }
                    if (generatedFile.getPermissions().contains(PosixFilePermission.OWNER_WRITE)) {
                        outFile.setReadable(true,
                                !(generatedFile.getPermissions().contains(PosixFilePermission.GROUP_WRITE)
                                        || generatedFile.getPermissions().contains(PosixFilePermission.OTHERS_WRITE)));
                    }
                }
            }
        } catch (Exception exception) {
            log.error("Could not write file: " + outFile.getAbsolutePath());
            throw new RuntimeException(exception);
        }
    }

    public static void writeGeneratedFiles(File targetDirectory, Collection<GeneratorFileEntry> generatorFileEntryCollection, String generatedFileName) {
        try {
            targetDirectory.mkdirs();
            Files.write(Paths.get(targetDirectory.getAbsolutePath(), generatedFileName),
                    String.join(NEWLINE, generatorFileEntryCollection.stream().map(f -> f.toString()).collect(Collectors.toList()))
                            .getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not write file: "
                    + Paths.get(targetDirectory.getAbsolutePath(), generatedFileName).toFile().getAbsolutePath(), e);
        }
    }

    public static Collection<GeneratorFileEntry> readFilesystemEntries(File targetDirectory, Collection<GeneratorFileEntry> generatorFileEntryCollection) {

        return generatorFileEntryCollection.parallelStream().map(f -> {
            File file = new File(targetDirectory, f.getPath());
            if (file.exists()) {
                GeneratorFileEntry currentFileEntry = GeneratorFileEntry.generatorFileEntry()
                        .checksum(ChecksumUtil.getMD5(file.toPath()))
                        .path(f.getPath())
                        .build();
                return currentFileEntry;
            }
            return null;
        }).filter(f -> f != null).collect(Collectors.toList());
    }

    public static Collection<GeneratorFileEntry> readGeneratedFiles(File targetDirectory, String generatedFileName) {
        try {
            if (Paths.get(targetDirectory.getAbsolutePath(), generatedFileName).toFile().exists()) {
                List<String> files = Files.readAllLines(Paths.get(targetDirectory.getAbsolutePath(), generatedFileName),
                        StandardCharsets.UTF_8);
                List<GeneratorFileEntry> entries = files.stream().map(s -> GeneratorFileEntry.fromString(s)).collect(Collectors.toList());
                return entries;
            } else {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read file: "
                    + Paths.get(targetDirectory.getAbsolutePath(), generatedFileName).toFile().getAbsolutePath(), e);
        }
    }

    public static void generateToDirectory(GeneratorParameter.GeneratorParameterBuilder builder) throws Exception {
        generateToDirectory(builder.build());
    }

    public static <T> void generateToDirectory(GeneratorParameter<T> parameter) throws Exception {
        final AtomicBoolean loggerToBeClosed = new AtomicBoolean(false);
        Log log = Objects.requireNonNullElseGet(parameter.log,
                                                () -> {
                                                    loggerToBeClosed.set(true);
                                                    return new BufferedSlf4jLogger(ModelGenerator.log);
                                                });

        try {
            GeneratorResult<T> result = parameter.performExecutor.apply(parameter);

            result.generatedByDiscriminator
                    .entrySet()
                    .stream()
                    .filter(e -> parameter.getDiscriminatorPredicate().test(e.getKey()))
                    .forEach(getDirectoryWriterForActor(
                            parameter.getDiscriminatorTargetDirectoryResolver(),
                            parameter.getDiscriminatorTargetNameResolver(),
                            parameter.isValidateChecksum(),
                            log));
            getDirectoryWriter(parameter.targetDirectoryResolver, parameter.isValidateChecksum(), log).accept(result.generated);
        } finally {
            if (loggerToBeClosed.get()) {
                log.close();
            }
        }
    }

    public static <T> void recalculateChecksumToDirectory(GeneratorParameter<T> parameter, Collection<T> discriminators) {
        discriminators.forEach(getDirectoryChecksumCalculatorForActor(
                parameter.getDiscriminatorTargetDirectoryResolver(),
                parameter.getDiscriminatorTargetNameResolver()));
        getDirectoryChecksumCalculator(parameter.targetDirectoryResolver).run();
    }


    @SneakyThrows(IOException.class)
    public static InputStream getGeneratedFilesAsZip(Collection<GeneratedFile> generatedFiles) {
        ByteArrayOutputStream generatedZip = new ByteArrayOutputStream();
        ZipOutputStream zipOutputStream = new ZipOutputStream(generatedZip);
        for (GeneratedFile generatedFile : generatedFiles) {
            zipOutputStream.putNextEntry(new ZipEntry(generatedFile.getPath()));
            zipOutputStream.write(generatedFile.getContent(), 0, generatedFile.getContent().length);
            zipOutputStream.flush();
            zipOutputStream.closeEntry();
        }
        zipOutputStream.flush();
        zipOutputStream.close();
        return new ByteArrayInputStream(generatedZip.toByteArray());
    }


    @Builder
    @Getter
    public static final class CreateGeneratorContextArgument {
        String descriptorName;
        @Builder.Default
        LinkedHashMap<String, URI> uris = new LinkedHashMap<>();
        @Builder.Default
        Collection<Class> helpers = new ArrayList<>();

        @Builder.Default
        Class contextAccessor = null;
        @Builder.Default
        Function<Collection<URI>, URLTemplateLoader> urlTemplateLoaderFactory = null;
        @Builder.Default
        Function<Collection<URI>, URLResolver> urlResolverFactory = null;

        @Builder.Default
        Supplier<Class<?>> generatorModelMixin = null;

        @Builder.Default
        Supplier<Class<?>> generatorTemplateMixin = null;

    }

    public static ModelGeneratorContext createGeneratorContext(CreateGeneratorContextArgument args) throws IOException {

        URLTemplateLoader urlTemplateLoader = null;
        URLResolver urlResolver = null;

        if (args.urlTemplateLoaderFactory != null) {
            urlTemplateLoader = args.urlTemplateLoaderFactory.apply(args.uris.values());
            if (args.urlResolverFactory != null) {
                urlResolver = args.urlResolverFactory.apply(args.uris.values());
            } else {
                throw new IllegalStateException("Could not determinate URLResolver");
            }
        } else {
            urlTemplateLoader = ChainedURLTemplateLoader.createFromURIs(args.uris.values());
            if (args.urlResolverFactory != null) {
                urlResolver = args.urlResolverFactory.apply(args.uris.values());
            } else {
                urlResolver = (URLResolver) urlTemplateLoader;
            }
        }

        if (args.uris.isEmpty()) {
            throw new IllegalArgumentException("Minimum one URI is mandatory for templates");
        }


        GeneratorModel generatorModel = null;

        Map.Entry<String, URI> root = args.uris.entrySet().stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No template URI is defined"));

        for (Map.Entry<String, URI> entry : args.uris.entrySet()) {
            GeneratorModel model = GeneratorModel.loadYamlURL(entry.getKey(),
                    UriHelper.calculateRelativeURI(entry.getValue(), args.descriptorName + YAML).normalize().toURL(),
                    args);
            if (entry == root) {
                generatorModel = model;
            } else {
                if (model != null && generatorModel != null) {
                    generatorModel.overrideTemplates(model.getTemplates());
                }
            }
        }

        List<ValueResolver> valueResolversPar = new ArrayList<>();
        for (Class helper : args.helpers) {
            if (ValueResolver.class.isAssignableFrom(helper) &&
                    valueResolversPar.stream().map(v -> v.getClass()).filter(v -> v == helper).findAny().isEmpty()) {
                try {
                    Object o = helper.getDeclaredConstructor().newInstance();
                    if (o instanceof ValueResolver) {
                        valueResolversPar.add((ValueResolver) o);
                    } else {
                        throw new IllegalArgumentException("Could not instantiate value resolver class: " + helper.getName());
                    }
                } catch (Exception e) {
                    throw new IllegalArgumentException("Could not load value resolver class: " + helper.getName());
                }
            }
        }

        Collection<Class> helpersPar = new ArrayList<>();
        if (args.helpers != null) {
            helpersPar.addAll(args.helpers);
        }

        ModelGeneratorContext modelGeneratorContext = ModelGeneratorContext.builder()
                .templateLoader(urlTemplateLoader)
                .urlResolver(urlResolver)
                .generatorModel(generatorModel)
                .helpers(helpersPar)
                .valueResolvers(valueResolversPar)
                .contextAccessor(args.contextAccessor)
                .build();

        return modelGeneratorContext;
    }


    @SneakyThrows
    public static void callBindContextForTypeIfCan(ModelGeneratorContext generatorContext, Class type, Object value) {
        if (generatorContext.getContextAccessor() != null) {
            Optional<Method> callMethod = Arrays.stream(generatorContext.getContextAccessor().getMethods()).filter(m ->
                    m.getName().equals("bindContext") &&
                            Modifier.isPublic(m.getModifiers()) &&
                            Modifier.isStatic(m.getModifiers()) &&
                            m.getParameters().length == 1 &&
                            type.isAssignableFrom(m.getParameters()[0].getType())
            ).findFirst();
            if (callMethod.isPresent()) {
                callMethod.get().invoke(null, value);
            }
        }
    }
}
