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

import com.github.jknack.handlebars.io.TemplateSource;
import com.github.jknack.handlebars.io.URLTemplateLoader;
import lombok.extern.slf4j.Slf4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A spacial URLTemplateLoader for Handlebars.
 * It supports parent URLTemoplateLoader, means the template loaders can be chained and the loading mechanism will walk
 * while the template is not found.
 * There is an extra override mechanism which helps the template decorator pattern. When a template placed with `override.hbs`
 * instead of `.hbs` extension, the `.override.hbs` version will be used and the original .hbs can be included inside of it.
 */
@Slf4j
public class ChainedURLTemplateLoader extends URLTemplateLoader implements URLResolver {
    final URI root;
    final ChainedURLTemplateLoader parent;
    final String contextPath;
    final Map<String, Stack<String>> pathOrder;


    public static ChainedURLTemplateLoader createFromURIs(Collection<URI> uris) {
        ChainedURLTemplateLoader clientGeneratorTemplateLoader = null;
        for (URI uri: uris) {
            clientGeneratorTemplateLoader = new ChainedURLTemplateLoader(clientGeneratorTemplateLoader, uri);
            clientGeneratorTemplateLoader.setSuffix("");
        }
        return clientGeneratorTemplateLoader;
    }

    /**
     * Creates a new {@link ChainedURLTemplateLoader}.
     *
     * @param parent Parent template loader. When parent is defined, it is used when resource not found.
     * @param root The base URI used for loading.. Required.
     * @param prefix The view prefix. Required.
     * @param suffix The view suffix. Required.
     */
    public ChainedURLTemplateLoader(final ChainedURLTemplateLoader parent, final URI root, final String contextPath, final String prefix, final String suffix) {
        this.root = root;
        this.parent = parent;
        this.contextPath = contextPath;
        if (parent == null) {
            pathOrder = new ConcurrentHashMap<>();
        } else {
            pathOrder = null;
        }
        setPrefix(prefix);
        setSuffix(suffix);
    }

    /**
     * Creates a new {@link ChainedURLTemplateLoader}.
     *
     * @param root The base URI used for loading.. Required.
     * @param prefix The view prefix. Required.
     */
    public ChainedURLTemplateLoader(final URI root, final String prefix) {
        this(null, root, UriHelper.lastPart(root.toString()), prefix, DEFAULT_SUFFIX);
    }

    /**
     * Creates a new {@link ChainedURLTemplateLoader}.
     *
     * @param parent Parent template loader. When parent is defined, it is used when resource not found.
     * @param root The base URI used for loading.. Required.
     * @param prefix The view prefix. Required.
     */
    public ChainedURLTemplateLoader(final ChainedURLTemplateLoader parent, final URI root, final String prefix) {
        this(parent, root, UriHelper.lastPart(root.toString()), prefix, DEFAULT_SUFFIX);
    }

    /**
     * Creates a new {@link ChainedURLTemplateLoader}. It looks for templates
     * stored in the root of the given path
     *
     * @param root The base URI used for loading.. Required.
     */
    public ChainedURLTemplateLoader(final URI root) {
        this(null, root, "/");
    }

    /**
     * Creates a new {@link ChainedURLTemplateLoader}. It looks for templates
     * stored in the root of the given path
     *
     * @param root The base URI used for loading.. Required.
     */
    public ChainedURLTemplateLoader(final ChainedURLTemplateLoader parent, final URI root) {
        this(parent, root, UriHelper.lastPart(root.toString()), "/", DEFAULT_SUFFIX);
    }

    private String getOverriddenLocationRelativePath(String loc) {
        return loc.replace(getSuffix(), ".override" + getSuffix());
    }

    @Override
    public String getSuffix() {
        String original = super.getSuffix();
        return !Objects.equals(original, "") ? original : DEFAULT_SUFFIX;
    }

    public Map<String, Stack<String>> getPathOrder() {
        if (parent != null) {
            return parent.getPathOrder();
        } else {
            return pathOrder;
        }
    }

    @Override
    public TemplateSource sourceAt(final String location) throws IOException {
        log.debug("sourceAt: " + root + " - " + location);
        String loc = location;
        if (location.startsWith(contextPath + "/")) {
            loc = location.substring(contextPath.length() + 1);
        }

        if (getPathOrder().get(loc) == null) {
            getPathOrder().put(loc, new Stack<>());
        }
        Stack<String> stack = getPathOrder().get(loc);

        String overrideRelativePath = getOverriddenLocationRelativePath(loc);
        String overrideFullPath = root.toString() + "/" + overrideRelativePath;

        // Need to make sure:
        // - prevent infinite loops for normal flows where override falls back internally to original template
        // - devs can explicitly load overrides, e.g. recurse with override
        if (!loc.equals(overrideRelativePath) && loc.endsWith(getSuffix()) && getPathOrder().size() > 0 &&
                !stack.contains(overrideFullPath)) {
            try {
                URL overrideFullPathUrl = new URI(overrideFullPath).normalize().toURL();
                stack.push(overrideFullPath);
                try (InputStream is = overrideFullPathUrl.openStream()) {
                    if (is != null && is.available() > 0) {
                        return sourceAtInternal(overrideRelativePath, location);
                    }
                    return sourceAtInternal(loc, location);
                } catch (Exception e) {
                    return sourceAtInternal(loc, location);
                }
            } catch (URISyntaxException e) {
                return sourceAtInternal(loc, location);
            }
        }
        stack.push(loc);

        return sourceAtInternal(loc, location);
    }

    public TemplateSource sourceAtInternal(final String loc, final String originalLoc) throws IOException {
        try {
            return super.sourceAt(loc);
        } catch (IOException ex) {
            // try next loader in the chain.
            log.trace("Unable to resolve: {}, trying next loader in the chain.", originalLoc);
        }
        if (parent != null) {
            return parent.sourceAt(loc);
        } else {
            throw new FileNotFoundException(originalLoc);
        }
    }

    @Override
    public String resolve(final String location) {
        log.debug("resolve: " + root + " - " + location);
        try {
            return super.resolve(location);
        } catch (Exception ex) {
            // try next loader in the chain.
            log.trace("Unable to resolve: {}, trying next loader in the chain.", location);
        }
        if (parent != null) {
            return parent.resolve(location);
        } else {
            throw new IllegalStateException("Can't resolve: '" + location + "'");
        }
    }

    @Override
    public URL getResource(String location) throws IOException {
        log.debug("getResource: " + root + " - " + location);
        try {
            String location_rel = location;
            if (root.toString().endsWith("/") && location.startsWith("/")) {
                location_rel = location.substring(1);
            }
            URL scriptUrl = new URI((root + location_rel)).normalize().toURL(); //root.resolve(location).toURL();
                try (InputStream is = scriptUrl.openStream()) {
                    if (is != null && is.available() > 0) {
                        return scriptUrl;
                    }
                } catch (Exception e) {
                }
        } catch (Exception e) {
        }
        URL url = null;

        if (parent != null && url == null) {
            url = parent.getResource(location);
        }

        // Use this bundle
        return url;

    }
}
