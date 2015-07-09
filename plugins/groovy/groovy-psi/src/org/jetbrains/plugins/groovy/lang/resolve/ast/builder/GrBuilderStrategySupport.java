/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.groovy.lang.resolve.ast.builder;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.resolve.ast.builder.strategy.DefaultBuilderStrategySupport;

import java.util.Collection;

public abstract class GrBuilderStrategySupport {

  public static final ExtensionPointName<GrBuilderStrategySupport> EP = ExtensionPointName.create("org.intellij.groovy.builderStrategySupport");
  public static final String BUILDER_PACKAGE = "groovy.transform.builder";
  public static final String BUILDER_FQN = BUILDER_PACKAGE + ".Builder";
  public static final String ORIGIN_INFO = "by @Builder";
  public static final String STRATEGY_ATTRIBUTE = "builderStrategy";

  public static class Members {
    public static final Members EMPTY = new Members() {
      @Override
      public void addFrom(Members other) {
        // do nothing
      }
    };

    public final Collection<PsiMethod> methods = ContainerUtil.newArrayList();
    public final Collection<GrField> fields = ContainerUtil.newArrayList();
    public final Collection<PsiClass> classes = ContainerUtil.newArrayList();

    public void addFrom(Members other) {
      methods.addAll(other.methods);
      fields.addAll(other.fields);
      classes.addAll(other.classes);
    }
  }

  @NotNull
  public abstract Members process(GrTypeDefinition typeDefinition);

  @Nullable
  @Contract("null,_ -> null")
  public static PsiClass getClassAttributeValue(@Nullable PsiAnnotation annotation, @NotNull String attributeName) {
    if (annotation == null) return null;
    final PsiAnnotationMemberValue value = annotation.findAttributeValue(attributeName);
    if (value instanceof GrReferenceExpression) {
      final PsiElement element = ((GrReferenceExpression)value).resolve();
      return element instanceof PsiClass ? (PsiClass)element : null;
    }
    else if (value instanceof PsiClassObjectAccessExpression) {
      PsiType type = ((PsiClassObjectAccessExpression)value).getOperand().getType();
      if (type instanceof PsiClassType) {
        return ((PsiClassType)type).resolve();
      }
    }
    return null;
  }

  @Contract("null, _ -> false")
  public static boolean isApplicable(@Nullable PsiAnnotation annotation, @NotNull String strategy) {
    if (annotation == null) return false;
    final PsiAnnotationMemberValue attributeValue = annotation.findDeclaredAttributeValue(STRATEGY_ATTRIBUTE);
    if (attributeValue == null) {
      return strategy == DefaultBuilderStrategySupport.DEFAULT_STRATEGY_NAME;
    }
    else {
      final String value = attributeValue.getText();
      return strategy.equals(value) || StringUtil.getQualifiedName(BUILDER_PACKAGE, strategy).equals(value);
    }
  }

  public static PsiType createType(PsiClass clazz) {
    return JavaPsiFacade.getElementFactory(clazz.getProject()).createType(clazz);
  }
}
