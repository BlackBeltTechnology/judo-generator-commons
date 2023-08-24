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

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import java.io.File;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Builder(builderMethodName = "generatorParameter")
@Getter
public final class GeneratorParameter<T> {
    ModelGeneratorContext generatorContext;

    @Builder.Default
    Predicate<T> discriminatorPredicate = a -> true;

    @NonNull
    Function<T, File> discriminatorTargetDirectoryResolver;

    @NonNull
    Function<T, String> discriminatorTargetNameResolver;

    @NonNull
    Supplier<File> targetDirectoryResolver;

    Logger log;

    @Builder.Default
    Supplier<Map<String, ?>> extraContextVariables = () -> ImmutableMap.of();

    @NonNull
    Function<GeneratorParameter<T>, GeneratorResult<T>> performExecutor;

    @Builder.Default
    boolean validateChecksum = true;
}
