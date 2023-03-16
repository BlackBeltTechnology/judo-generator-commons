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

import lombok.SneakyThrows;

import java.net.URI;
import java.net.URISyntaxException;

public class UriHelper {

    @SneakyThrows(URISyntaxException.class)
    public static URI calculateRelativeURI(URI uri, String path) {
        URI uriRoot = uri;
        if (uriRoot.toString().endsWith(".jar")) {
            uriRoot = new URI(concatUri("jar:" + uriRoot.toString() + "!/", path));
        } else if (uriRoot.toString().startsWith("jar:bundle:")) {
            uriRoot = new URI(uriRoot.toString().substring(4, uriRoot.toString().indexOf("!")) + path);
        } else {
            uriRoot = new URI(concatUri(uriRoot.toString(), path));
        }
        return uriRoot;
    }

     public static String concatUri(String root, String path) {
        String base = root;
        String pathRel = path;
        if (root.endsWith("/")) {
            base = root.substring(0, root.length() - 1);
        }
         if (path.startsWith("/")) {
             pathRel = path.substring(1, root.length());
         }
        return base + "/" + pathRel;
     }

    public static String lastPart(String url) {
        String base = url;
        if (url.endsWith("/")) {
            base = url.substring(0, url.length() - 1);
        }
        String[] urlParts = base.split("/");
        if (urlParts != null && urlParts.length > 0) {
            return urlParts[urlParts.length - 1];
        }
        return null;
    }


}
