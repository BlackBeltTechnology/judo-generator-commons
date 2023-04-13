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

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Builder(builderMethodName = "generatorFileEntry")
@Getter
@Setter
public final class GeneratorFileEntry implements Comparable {

    @NonNull
    String path;

    @NonNull
    String md5;

    @Override
    public int compareTo(Object o) {
        if (o instanceof GeneratorFileEntry) {
            GeneratorFileEntry fileEntry = (GeneratorFileEntry) o;
            return this.getPath().compareTo(fileEntry.getPath());
        }
        return -1;
    }

    @Override
    public String toString() {
        return path + "," + md5;
    }
}
