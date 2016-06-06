/**
 * Copyright © 2010-2014 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jsonschema2pojo.rules;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JDocComment;
import com.sun.codemodel.JDocCommentable;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JType;

import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.swagger2.Swagger2AnnotationApplier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.validation.constraints.NotNull;

/**
 * Applies the "required" JSON schema rule.
 *
 * @see <a
 * href="http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.4.3">http://tools.ietf.org/html/draft-fge-json-schema-validation-00#section-5.4.3</a>
 */
public class RequiredArrayRule implements Rule<JDefinedClass, JDefinedClass> {

    public static final String REQUIRED_COMMENT_TEXT = "\n(Required)";

    private final RuleFactory ruleFactory;
    private final Swagger2AnnotationApplier swagger2AnnotationApplier;

    protected RequiredArrayRule(RuleFactory ruleFactory) {
        this.ruleFactory = ruleFactory;
        this.swagger2AnnotationApplier = new Swagger2AnnotationApplier(ruleFactory);
    }

    @Override
    public JDefinedClass apply(String nodeName, JsonNode node, JDefinedClass jclass, Schema schema) {
        List<String> requiredFieldMethods = new ArrayList<String>();

        JsonNode properties = schema.getContent().get("properties");

        for (Iterator<JsonNode> iterator = node.elements(); iterator.hasNext(); ) {
            String requiredArrayItem = iterator.next().asText();
            JsonNode propertyNode = null;
            if (properties != null) {
                propertyNode = properties.findValue(requiredArrayItem);
            }

            String fieldName = ruleFactory.getNameHelper().getPropertyName(requiredArrayItem, propertyNode);
            JFieldVar field = jclass.fields().get(fieldName);
            if (field == null) {
                continue;
            }

            addJavaDoc(field);
            addNotNullAnnotation(field);
            swagger2AnnotationApplier.setModelPropertyAnnotationRequired(field, true);

            requiredFieldMethods.add(getGetterName(fieldName, field.type(), node));
            requiredFieldMethods.add(getSetterName(fieldName, node));
        }

        updateGetterSetterJavaDoc(jclass, requiredFieldMethods);

        return jclass;
    }

    private void updateGetterSetterJavaDoc(JDefinedClass jclass, List<String> requiredFieldMethods) {
        for (Iterator methods = jclass.methods().iterator(); methods.hasNext(); ) {
            JMethod method = (JMethod) methods.next();
            if (requiredFieldMethods.contains(method.name())) {
                addJavaDoc(method);
            }
        }
    }

    private void addNotNullAnnotation(JFieldVar field) {
        if (!ruleFactory.getGenerationConfig().isIncludeJsr303Annotations()) {
            return;
        }
        field.annotate(NotNull.class);
    }

    private void addJavaDoc(JDocCommentable docCommentable) {
        JDocComment javadoc = docCommentable.javadoc();
        javadoc.append(REQUIRED_COMMENT_TEXT);
    }

    private String getSetterName(String propertyName, JsonNode node) {
        return ruleFactory.getNameHelper().getSetterName(propertyName, node);
    }

    private String getGetterName(String propertyName, JType type, JsonNode node) {
        return ruleFactory.getNameHelper().getGetterName(propertyName, type, node);
    }
}
