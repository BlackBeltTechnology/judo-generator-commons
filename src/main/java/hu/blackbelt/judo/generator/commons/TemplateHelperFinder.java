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

import hu.blackbelt.judo.generator.commons.annotations.ContextAccessor;
import hu.blackbelt.judo.generator.commons.annotations.TemplateHelper;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ClassInfoList;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class TemplateHelperFinder {


    private static ClassInfoList getAnnotatedTypes(Class annotation, Collection<String> acceptedPackages, ClassLoader...classLoaders) {
        ClassGraph classGraph =  new ClassGraph()
                .enableAnnotationInfo()
                .overrideClassLoaders(getClassLooaders(classLoaders));

        if (acceptedPackages != null && acceptedPackages.size() > 0) {
            classGraph.acceptPackages(acceptedPackages.stream().map(s -> s.trim()).collect(Collectors.toList()).toArray(new String[acceptedPackages.size()]));
        }
        return   classGraph.scan()
                .getClassesWithAnnotation(annotation.getName());
    }

    private static ClassLoader[] getClassLooaders(ClassLoader...classLoaders) {
        ClassLoader classLoadersEff[] = classLoaders;
        if (classLoaders.length == 0) {
            classLoadersEff = new ClassLoader[]{Thread.currentThread().getContextClassLoader()};
        }
        return classLoadersEff;
    }
    public static Collection<String> collectHelpers(Collection<String> acceptedPackages, ClassLoader...classLoaders) throws IOException {
        return getAnnotatedTypes(TemplateHelper.class, acceptedPackages, classLoaders).stream().map(c -> c.getName()).collect(Collectors.toSet());
    }

    public static Collection<String> collectHelpers(ClassLoader...classLoaders) throws IOException {
        return getAnnotatedTypes(TemplateHelper.class, null, classLoaders).stream().map(c -> c.getName()).collect(Collectors.toSet());
    }

    public static Collection<Class> collectHelpersAsClass(Collection<String> acceptedPackages, ClassLoader...classLoaders) throws IOException {
        return getAnnotatedTypes(TemplateHelper.class, acceptedPackages, classLoaders).stream().map(c -> c.loadClass()).collect(Collectors.toSet());
    }

    public static Collection<Class> collectHelpersAsClass(ClassLoader...classLoaders) throws IOException {
        return getAnnotatedTypes(TemplateHelper.class, null, classLoaders).stream().map(c -> c.loadClass()).collect(Collectors.toSet());
    }

    private static Optional<ClassInfo> findContextAccessorClassInfo(Collection<String> acceptedPackages, ClassLoader...classLoaders) throws IOException {
        ClassInfoList classInfos = getAnnotatedTypes(ContextAccessor.class, acceptedPackages, classLoaders);
        if (classInfos.size() > 1) {
            throw new IllegalArgumentException("Multiple instance of class annotated with @ContextAccessor found: " +
                    classInfos.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
        }
        if (classInfos.size() == 1) {
            return Optional.of(classInfos.get(0));
        }
        return Optional.empty();
    }

    public static Optional<Class> findContextAccessorAsClass(Collection<String> acceptedPackages, ClassLoader...classLoaders) throws IOException {
        return findContextAccessorClassInfo(acceptedPackages, classLoaders).map(o -> o.loadClass());
    }

    public static Optional<Class> findContextAccessorAsClass(ClassLoader...classLoaders) throws IOException {
        return findContextAccessorClassInfo(null, classLoaders).map(o -> o.loadClass());
    }

    public static Optional<String> findContextAccessor(Collection<String> acceptedPackages, ClassLoader...classLoaders) throws IOException {
        return findContextAccessorClassInfo(acceptedPackages, classLoaders).map(o -> o.getName());
    }

    public static Optional<String> findContextAccessor(ClassLoader...classLoaders) throws IOException {
        return findContextAccessorClassInfo(null, classLoaders).map(o -> o.getName());
    }

}
