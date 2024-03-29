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

import com.github.jknack.handlebars.Template;
import lombok.Getter;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@Getter
public class TemplateEvaulator {
    final Expression factoryExpression;
    final Expression pathExpression;
    final Expression conditionExpression;

    final Template template;
    final Map<String, Expression> templateExpressions;
    final ModelGeneratorContext projectGenerator;
    final StandardEvaluationContext standardEvaluationContext;

    public TemplateEvaulator(ModelGeneratorContext projectGenerator, GeneratorTemplate generatorTemplate, StandardEvaluationContext standardEvaluationContext) throws IOException {
        this.projectGenerator = projectGenerator;
        this.standardEvaluationContext = standardEvaluationContext;
        ExpressionParser parser = generatorTemplate.getParser();
        templateExpressions = generatorTemplate.parseExpressions();
        if (generatorTemplate.getFactoryExpression() != null) {
            factoryExpression = parser.parseExpression(generatorTemplate.getFactoryExpression());
        } else {
            factoryExpression = null;
        }
        pathExpression = parser.parseExpression(generatorTemplate.getPathExpression());
        if (generatorTemplate.getConditionExpression() != null) {
            conditionExpression = parser.parseExpression(generatorTemplate.getConditionExpression());
        } else {
            conditionExpression = null;
        }
        if (generatorTemplate.isCopy()) {
            template = null;
        } else if (generatorTemplate.getTemplate() != null && !"".equals(generatorTemplate.getTemplate().trim())) {
            template = projectGenerator.createHandlebars().compileInline(generatorTemplate.getTemplate());
        } else if (generatorTemplate.getTemplateName() != null && !"".equals(generatorTemplate.getTemplateName().trim())) {
            template = projectGenerator.createHandlebars().compile(generatorTemplate.getTemplateName());
        } else {
            template = null;
        }
    }

    public <C> C getFactoryExpressionResultOrValue(GeneratorTemplate template, Object value, Class<C> type) {
        if (getFactoryExpression() == null && type.isAssignableFrom(value.getClass())) {
            return (C) value;
        } else {
            Object ret = null;
            try {
                ret =  getFactoryExpression().getValue(standardEvaluationContext, value, type);
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not evaluate factory expression in " + template.toString(), e);
            }

            if (!type.isAssignableFrom(ret.getClass())) {
                if (Collection.class.isAssignableFrom(ret.getClass())) {
                    return (C) Collections.singletonList(ret);
                } else {
                    throw new IllegalArgumentException("Could not cast return type to expected type and expected type is not collection." + template.toString());
                }
            }
            return (C) ret;
        }
    }
}
