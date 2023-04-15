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

import lombok.*;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

/**
 * It represents a generated file. It is used to write to directory or create a ZipStream.
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GeneratedFile {

    private String path;
    private byte[] content;

    private Set<PosixFilePermission> permissions;

}
