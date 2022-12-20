package hu.blackbelt.judo.generator.commons;

import hu.blackbelt.judo.generator.commons.annotations.ContextAccessor;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class TemplateHelperFinder {


    private static ClassInfoList getAnnotatedTypes(Class annotation, ClassLoader...classLoaders) {
        return new ClassGraph()
                .enableAnnotationInfo()
                .overrideClassLoaders(getClassLooaders(classLoaders))
                .scan()
                .getClassesWithAnnotation(annotation.getName());
    }

    private static ClassLoader[] getClassLooaders(ClassLoader...classLoaders) {
        ClassLoader classLoadersEff[] = classLoaders;
        if (classLoaders.length == 0) {
            classLoadersEff = new ClassLoader[]{Thread.currentThread().getContextClassLoader()};
        }
        return classLoadersEff;
    }
    public static Collection<String> collectHelpers(ClassLoader...classLoaders) throws IOException {
        return getAnnotatedTypes(TemplateHelper.class, classLoaders).stream().map(c -> c.getName()).collect(Collectors.toSet());
    }

    public static Collection<Class> collectHelpersAsClass(ClassLoader...classLoaders) throws IOException {
        return getAnnotatedTypes(TemplateHelper.class, classLoaders).stream().map(c -> c.loadClass()).collect(Collectors.toSet());
    }

    private static Optional<ClassInfo> findContextAccessorClassInfo(ClassLoader...classLoaders) throws IOException {
        ClassInfoList classInfos = getAnnotatedTypes(ContextAccessor.class, classLoaders);
        if (classInfos.size() > 1) {
            throw new IllegalArgumentException("Multiple instance of class annotated with @ContextAccessor found");
        }
        if (classInfos.size() == 1) {
            return Optional.of(classInfos.get(0));
        }
        return Optional.empty();
    }

    public static Optional<Class> findContextAccessorAsClass(ClassLoader...classLoaders) throws IOException {
        return findContextAccessorClassInfo(classLoaders).map(o -> o.loadClass());
    }

    public static Optional<String> findContextAccessor(ClassLoader...classLoaders) throws IOException {
        return findContextAccessorClassInfo(classLoaders).map(o -> o.getName());
    }


}
