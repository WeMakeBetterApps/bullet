/*
 * Copyright (C) 2014 Thomas Broyer
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
package bullet.impl;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.auto.common.MoreTypes;
import com.google.auto.common.Visibility;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

import bullet.impl.ComponentMethodDescriptor.ComponentMethodKind;
import dagger.Component;
import dagger.Subcomponent;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

class ComponentProcessingStep implements BasicAnnotationProcessor.ProcessingStep {

  private final ProcessingEnvironment processingEnv;

  ComponentProcessingStep(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return ImmutableSet.of(Component.class, Subcomponent.class);
  }

  @Override
  public void process(SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    Set<Element> componentElements = Sets.union(
        elementsByAnnotation.get(Component.class),
        elementsByAnnotation.get(Subcomponent.class));

    for (Element element : componentElements) {
      TypeElement componentElement = MoreElements.asType(element);
      generateObjectGraph(componentElement);
    }
  }

  private void generateObjectGraph(TypeElement element) {
    DeclaredType component = MoreTypes.asDeclared(element.asType());
    ArrayList<ComponentMethodDescriptor> provisionMethods = new ArrayList<>();
    ArrayList<ComponentMethodDescriptor> membersInjectionMethods = new ArrayList<>();

    PackageElement packageElement = processingEnv.getElementUtils().getPackageOf(element);
    TypeElement objectElement = processingEnv.getElementUtils().getTypeElement(Object.class.getCanonicalName());
    for (ExecutableElement method : ElementFilter.methodsIn(processingEnv.getElementUtils().getAllMembers(element))) {
      if (method.getEnclosingElement().equals(objectElement)) {
        continue;
      }
      if (!isVisibleFrom(method, packageElement)) {
        continue;
      }
      Optional<ComponentMethodDescriptor> optMethodDescriptor =
          ComponentMethodDescriptor.forComponentMethod(processingEnv.getTypeUtils(), component, method);
      if (!optMethodDescriptor.isPresent()) {
        continue;
      }
      ComponentMethodDescriptor methodDescriptor = optMethodDescriptor.get();
      if (!isVisibleFrom(processingEnv.getTypeUtils().asElement(methodDescriptor.type()), packageElement)) {
        continue;
      }
      switch (methodDescriptor.kind()) {
        case SIMPLE_PROVISION:
        case PROVIDER_OR_LAZY:
          provisionMethods.add(methodDescriptor);
          break;
        case SIMPLE_MEMBERS_INJECTION:
        case MEMBERS_INJECTOR:
          membersInjectionMethods.add(methodDescriptor);
          break;
        default:
          throw new AssertionError();
      }
    }

    // Order members-injection methods from most-specific to least-specific types, for cascading ifs of instanceof.
    Collections.sort(membersInjectionMethods, new Comparator<ComponentMethodDescriptor>() {
      final javax.lang.model.util.Types typeUtils = processingEnv.getTypeUtils();

      @Override
      public int compare(ComponentMethodDescriptor o1, ComponentMethodDescriptor o2) {
        TypeMirror t1 = o1.type(), t2 = o2.type();
        if (typeUtils.isSameType(t1, t2)) {
          return 0;
        } else if (typeUtils.isSubtype(t1, t2)) {
          return -1;
        } else if (typeUtils.isSubtype(t2, t1)) {
          return 1;
        }
        return getName(t1).compareTo(getName(t2));
      }

      private String getName(TypeMirror type) {
        return MoreElements.asType(typeUtils.asElement(type)).getQualifiedName().toString();
      }
    });

    final ClassName elementName = ClassName.get(element);

    // Filter out duplicate injections
    Map<TypeMirror, ComponentMethodDescriptor> membersInjectionMethodsMap = new LinkedHashMap<>(membersInjectionMethods.size());
    for (ComponentMethodDescriptor componentMethod : membersInjectionMethods) {
      ComponentMethodDescriptor storedComponentMethod = membersInjectionMethodsMap.get(componentMethod.type());
      if (storedComponentMethod == null || storedComponentMethod.kind() != ComponentMethodKind.MEMBERS_INJECTOR) {
        membersInjectionMethodsMap.put(componentMethod.type(), componentMethod);
      }
    }

    final TypeSpec.Builder classBuilder = TypeSpec.classBuilder("Bullet" + Joiner.on("_").join(elementName.simpleNames()))
        .addOriginatingElement(element)
        .addAnnotation(AnnotationSpec.builder(Generated.class)
            .addMember("value", "$S", ComponentProcessor.class.getCanonicalName())
            .build())
        .addModifiers(PUBLIC, FINAL)
        .addSuperinterface(ClassName.get("bullet", "ObjectGraph"))
        .addField(elementName, "component", PRIVATE, FINAL)
        .addMethod(MethodSpec.constructorBuilder()
            .addModifiers(PUBLIC)
            .addParameter(elementName, "component", FINAL)
            .addCode("this.component = component;\n")
            .build());

    // Generate the ClassIndexHashTable if there are classes to inject.
    if (membersInjectionMethodsMap.size() > 0) {
      classBuilder.addField(ClassName.get("bullet.impl", "ClassIndexHashTable"), "classIndexHashTable", PRIVATE, STATIC, FINAL);

      // ClassIndexHashTable size should be at least 30% larger and a prime number.
      int classIndexHashTableSize = getNextPrime((int) Math.ceil(membersInjectionMethodsMap.size() * 1.3));

      CodeBlock.Builder classIndexHashTableCodeBlockBuilder = CodeBlock.builder()
          .add("classIndexHashTable = new ClassIndexHashTable(" + classIndexHashTableSize + ");\n");

      int i = 0;
      for (Map.Entry<TypeMirror, ComponentMethodDescriptor> entry : membersInjectionMethodsMap.entrySet()) {
        classIndexHashTableCodeBlockBuilder.add("classIndexHashTable.put($T.class, (char) " + i++ + ");\n", entry.getValue().type());
      }
      classBuilder.addStaticBlock(classIndexHashTableCodeBlockBuilder.build());
    }

    final TypeVariableName t = TypeVariableName.get("T");
    final MethodSpec.Builder getBuilder = MethodSpec.methodBuilder("get")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addTypeVariable(t)
        .returns(t)
        .addParameter(ParameterizedTypeName.get(ClassName.get(Class.class), t), "type", FINAL);
    for (ComponentMethodDescriptor method : provisionMethods) {
      getBuilder.addCode(
          "if (type == $T.class) {\n$>" +
          "return type.cast(this.component.$N()$L);\n" +
          "$<}\n",
          method.type(), method.name(), method.kind() == ComponentMethodKind.PROVIDER_OR_LAZY ? ".get()" : "");
    }
    getBuilder.addCode("throw new $T(\"No get or Provides method found for \" + type.getName() + \" in " + elementName.simpleName() + "\");\n", IllegalArgumentException.class);
    classBuilder.addMethod(getBuilder.build());

    final MethodSpec.Builder injectWriter = MethodSpec.methodBuilder("inject")
        .addAnnotation(Override.class)
        .addModifiers(PUBLIC)
        .addTypeVariable(t)
        .returns(t)
        .addParameter(t, "instance", FINAL);

    // Generate injection code if there are injections
    if (membersInjectionMethodsMap.size() > 0) {
      injectWriter.addCode(
          "Class<?> c = instance.getClass();\n" +
              "while (c != Object.class) {\n$>" +
              "switch (classIndexHashTable.get(c)) {\n$>");

      {
        int i = 0;
        for (Map.Entry<TypeMirror, ComponentMethodDescriptor> entry : membersInjectionMethodsMap.entrySet()) {
          ComponentMethodDescriptor method = entry.getValue();
          injectWriter.addCode(
              "case $L:\n$>" +
                  "this.component.$N$L(($T) instance);\n" +
                  "return instance;\n$<",
              i++, method.name(),
              method.kind() == ComponentMethodKind.MEMBERS_INJECTOR ? "().injectMembers" : "", method.type()
          );
        }
      }

      injectWriter.addCode(
          "$<}\n" +
              "c = c.getSuperclass();\n" +
              "$<}\n");
    }

    injectWriter.addCode("throw new $T(\"No inject or MembersInject method found for \" + instance.getClass().getName() + \" in " + elementName.simpleName() + "\");\n", IllegalArgumentException.class);
    classBuilder.addMethod(injectWriter.build());

    try {
      JavaFile.builder(elementName.packageName(), classBuilder.build())
          .build()
          .writeTo(processingEnv.getFiler());
    } catch (IOException ioe) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      pw.println("Error generating source file for type " + classBuilder.build().name);
      ioe.printStackTrace(pw);
      pw.close();
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, sw.toString());
    }
  }

  private boolean isVisibleFrom(Element target, PackageElement from) {
    switch (Visibility.effectiveVisibilityOfElement(target)) {
      case PUBLIC:
        return true;
      case PROTECTED:
      case DEFAULT:
        return MoreElements.getPackage(target).equals(from);
      case PRIVATE:
        return false;
      default:
        throw new AssertionError();
    }
  }

  private static int getNextPrime(int value) {
    while (true) {
      value++;
      if (isPrime(value)) {
        return value;
      }
    }
  }

  private static boolean isPrime(int value) {
    for (int i = 2; i < value; i++) {
      if (value % i == 0) {
        return false;
      }
    }
    return true;
  }
}
