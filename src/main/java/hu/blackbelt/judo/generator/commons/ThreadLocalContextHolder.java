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

import java.util.Map;

/**
 * The handlebars context inaccessible in helpers / value resolvers
 * because there is no state for them. The ThreadLocal is used
 * to init variable values from template execution.
 */
public abstract class ThreadLocalContextHolder {
    private static ThreadLocal<Map<String, ?>> contextLocal = new ThreadLocal<>();

    public static ThreadLocal<Map<String, ?>>getContextLocal() {
        return contextLocal;
    }

    public static void bindContext(Map<String, ?> context) {
        contextLocal.set(context);
    }

    public static synchronized Object getVariable(String key) {
        if (contextLocal.get() == null) {
            System.out.println("Could not retrieve contextLocal");
            throw new RuntimeException("Could not retrieve contextLocal");
        } else {
            return contextLocal.get().get(key);
        }
    }

}
