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
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class loads descriptor yaml file and processing it.
 * The yaml file contains a entries describes the generation itself. On entry can
 * be used to generate several entries of files. @see {@link GeneratorTemplate}
 */
@Slf4j
public class ModelGenerator<M> {

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
        return generatedFile;
    }

    public static <D> Consumer<Map.Entry<D, Collection<GeneratedFile>>> getDirectoryWriterForActor(Function<D, File> actorTypeTargetDirectoryResolver, Log log) {
        return e -> {
            File targetDirectory = actorTypeTargetDirectoryResolver.apply(e.getKey());
            GeneratorIgnore generatorIgnore = new GeneratorIgnore(targetDirectory.toPath());
            e.getValue().stream().forEach(f -> {
                File outFile = new File(targetDirectory, f.getPath());
                outFile.getParentFile().mkdirs();
                if (!generatorIgnore.shouldExcludeFile(outFile.toPath())) {
                    try {
                        if (outFile.exists()) {
                            log.debug("File already exists, overwrite: " + outFile.getAbsolutePath());
                            outFile.delete();
                        }
                        ByteStreams.copy(new ByteArrayInputStream(f.getContent()), new FileOutputStream(outFile));
                    } catch (Exception exception) {
                        log.error("Could not write file: " + outFile.getAbsolutePath());
                        throw new RuntimeException(exception);
                    }
                }
            });
        };
    }

    public static Consumer<Collection<GeneratedFile>> getDirectoryWriter(Supplier<File> targetDirectoryResolver, Log log) {
        return e -> {
            File targetDirectory = targetDirectoryResolver.get();
            GeneratorIgnore generatorIgnore = new GeneratorIgnore(targetDirectory.toPath());
            e.stream().forEach(f -> {
                File outFile = new File(targetDirectory, f.getPath());
                outFile.getParentFile().mkdirs();
                if (!generatorIgnore.shouldExcludeFile(outFile.toPath())) {
                    try {
                        if (outFile.exists()) {
                            log.debug("File already exists, overwrite: " + outFile.getAbsolutePath());
                            outFile.delete();
                        }
                        ByteStreams.copy(new ByteArrayInputStream(f.getContent()), new FileOutputStream(outFile));
                    } catch (Exception exception) {
                        log.error("Could not write file: " + outFile.getAbsolutePath());
                        throw new RuntimeException(exception);
                    }
                }
            });

        };
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
                    .filter(e -> parameter.discriminatorPredicate.test(e.getKey()))
                    .forEach(getDirectoryWriterForActor(parameter.discriminatorTargetDirectoryResolver, log));
            getDirectoryWriter(parameter.targetDirectoryResolver, log).accept(result.generated);
        } finally {
            if (loggerToBeClosed.get()) {
                log.close();
            }
        }
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