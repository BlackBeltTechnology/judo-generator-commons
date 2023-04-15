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

@Builder(builderMethodName = "generatorFileEntry")
@Getter
@Setter
public final class GeneratorFileEntry implements Comparable {

    @NonNull
    String path;

    @NonNull
    String checksum;

    public static GeneratorFileEntry fromString(String str) {
        String[] parts = str.split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Could not parse file entry: " + str);
        }
        return GeneratorFileEntry.generatorFileEntry()
                .path(parts[0])
                .checksum(parts[1])
                .build();
    }

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
        return getPath() + "," + getChecksum();
    }

    @Override
    public int hashCode() {
        return getPath().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GeneratorFileEntry) {
            GeneratorFileEntry fileEntry = (GeneratorFileEntry) o;
            return this.getPath().equals(fileEntry.getPath()) && this.getChecksum().equals(fileEntry.getChecksum());
        }
        return false;
    }
}
