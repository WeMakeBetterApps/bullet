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

import com.google.common.collect.ImmutableList;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Test;

import java.lang.annotation.Annotation;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourcesSubjectFactory.javaSources;

public abstract class AbstractComponentProcessorTest {

  protected abstract Class<? extends Annotation> getComponentType();

  @Test public void simpleComponent() {
    JavaFileObject injectableTypeFile = JavaFileObjects.forSourceLines("test.SomeInjectableType",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class SomeInjectableType {",
        "  @Inject SomeInjectableType() {}",
        "}");
    JavaFileObject otherInjectableTypeFile = JavaFileObjects.forSourceLines("test.OtherInjectableType",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class OtherInjectableType {",
        "  @Inject OtherInjectableType() {}",
        "}");
    JavaFileObject subcomponentFile = JavaFileObjects.forSourceLines("test.SimpleSubcomponent",
        "package test;",
        "",
        "import dagger.Subcomponent;",
        "",
        "@Subcomponent",
        "interface SimpleSubcomponent {",
        "  OtherInjectableType otherInjectableType();",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.SimpleComponent",
        "package test;",
        "",
        "import " + getComponentType().getCanonicalName() + ";",
        "",
        "@" + getComponentType().getSimpleName(),
        "interface SimpleComponent {",
        "  SomeInjectableType someInjectableType();",
        "  SimpleSubcomponent simpleSubcomponent();",
        "}");
    JavaFileObject generatedBullet = JavaFileObjects.forSourceLines("test.BulletSimpleComponent",
        "package test;\n" +
            "\n" +
            "import bullet.ObjectGraph;\n" +
            "import java.lang.Class;\n" +
            "import java.lang.IllegalArgumentException;\n" +
            "import java.lang.Override;\n" +
            "import javax.annotation.Generated;\n" +
            "\n" +
            "@Generated(\"bullet.impl.ComponentProcessor\")\n" +
            "public final class BulletSimpleComponent implements ObjectGraph {\n" +
            "  private final SimpleComponent component;\n" +
            "\n" +
            "  public BulletSimpleComponent(final SimpleComponent component) {\n" +
            "    this.component = component;\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T get(final Class<T> type) {\n" +
            "    if (type == SomeInjectableType.class) {\n" +
            "      return type.cast(this.component.someInjectableType());\n" +
            "    }\n" +
            "    throw new IllegalArgumentException(\"No get or Provides method found for \" + type.getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T inject(final T instance) {\n" +
            "    throw new IllegalArgumentException(\"No inject or MembersInject method found for \" + instance.getClass().getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "}");
    assert_().about(javaSources()).that(ImmutableList.of(injectableTypeFile, otherInjectableTypeFile, subcomponentFile, componentFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(generatedBullet);
  }

  @Test public void simpleComponentWithNesting() {
    JavaFileObject nestedTypesFile = JavaFileObjects.forSourceLines("test.OuterType",
        "package test;",
        "",
        "import " + getComponentType().getCanonicalName() + ";",
        "import javax.inject.Inject;",
        "",
        "final class OuterType {",
        "  final static class A {",
        "    @Inject A() {}",
        "  }",
        "  final static class B {",
        "    @Inject A a;",
        "  }",
        "  @" + getComponentType().getSimpleName() + " interface SimpleComponent {",
        "    A a();",
        "    void inject(B b);",
        "  }",
        "}");
    JavaFileObject generatedBullet = JavaFileObjects.forSourceLines("test.BulletOuterType_SimpleComponent",
        "package test;\n" +
            "\n" +
            "import bullet.ObjectGraph;\n" +
            "import bullet.impl.ClassIndexHashTable;\n" +
            "import java.lang.Class;\n" +
            "import java.lang.IllegalArgumentException;\n" +
            "import java.lang.Override;\n" +
            "import javax.annotation.Generated;\n" +
            "\n" +
            "@Generated(\"bullet.impl.ComponentProcessor\")\n" +
            "public final class BulletOuterType_SimpleComponent implements ObjectGraph {\n" +
            "  private static final ClassIndexHashTable classIndexHashTable;\n" +
            "\n" +
            "  static {\n" +
            "    classIndexHashTable = new ClassIndexHashTable(3);\n" +
            "    classIndexHashTable.put(OuterType.B.class, (char) 0);\n" +
            "  }\n" +
            "\n" +
            "  private final OuterType.SimpleComponent component;\n" +
            "\n" +
            "  public BulletOuterType_SimpleComponent(final OuterType.SimpleComponent component) {\n" +
            "    this.component = component;\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T get(final Class<T> type) {\n" +
            "    if (type == OuterType.A.class) {\n" +
            "      return type.cast(this.component.a());\n" +
            "    }\n" +
            "    throw new IllegalArgumentException(\"No get or Provides method found for \" + type.getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T inject(final T instance) {\n" +
            "    Class<?> c = instance.getClass();\n" +
            "    while (c != Object.class) {\n" +
            "      switch (classIndexHashTable.get(c)) {\n" +
            "        case 0:\n" +
            "          this.component.inject((OuterType.B) instance);\n" +
            "          return instance;\n" +
            "      }\n" +
            "      c = c.getSuperclass();\n" +
            "    }\n" +
            "    throw new IllegalArgumentException(\"No inject or MembersInject method found for \" + instance.getClass().getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "}");
    assert_().about(javaSources()).that(ImmutableList.of(nestedTypesFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(generatedBullet);
  }

  @Test public void membersInjectionTypePrecedence() {
    // XXX: Dagger‡ doesn't support @Inject on abstract methods (e.g. interfaces), but we support it here anyway.
    JavaFileObject iFile = JavaFileObjects.forSourceLines("test.I",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "interface I {",
        "  @Inject void setE(E e);",
        "}");
    JavaFileObject i2File = JavaFileObjects.forSourceLines("test.I2",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "interface I2 extends I {",
        "  @Inject void setC(C c);",
        "}");
    JavaFileObject aFile = JavaFileObjects.forSourceLines("test.A",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class A implements I {",
        "  @Inject public void setE(E e) {};",
        "}");
    JavaFileObject bFile = JavaFileObjects.forSourceLines("test.B",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class B {",
        "  @Inject A a;",
        "}");
    JavaFileObject cFile = JavaFileObjects.forSourceLines("test.C",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "import javax.inject.Provider;",
        "",
        "final class C extends B {",
        "  @Inject Provider<I> iProvider;",
        "}");
    JavaFileObject dFile = JavaFileObjects.forSourceLines("test.D",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class D implements I2 {",
        "  @Inject public void setC(C c) {}",
        "  @Inject public void setE(E e) {}",
        "}");
    JavaFileObject eFile = JavaFileObjects.forSourceLines("test.E",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class E {",
        " @Inject E() {}",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.SimpleComponent",
        "package test;",
        "",
        "import " + getComponentType().getCanonicalName() + ";",
        "",
        "@" + getComponentType().getSimpleName(),
        "interface SimpleComponent {",
        "  void inject(I i);",
        "  void inject(I2 i1);",
        "  void inject(A a);",
        "  void inject(B b);",
        "  void inject(C c);",
        "  void inject(D d);",
        "}");
    JavaFileObject generatedBullet = JavaFileObjects.forSourceLines("test.BulletSimpleComponent",
        "package test;\n" +
            "\n" +
            "import bullet.ObjectGraph;\n" +
            "import bullet.impl.ClassIndexHashTable;\n" +
            "import java.lang.Class;\n" +
            "import java.lang.IllegalArgumentException;\n" +
            "import java.lang.Override;\n" +
            "import javax.annotation.Generated;\n" +
            "\n" +
            "@Generated(\"bullet.impl.ComponentProcessor\")\n" +
            "public final class BulletSimpleComponent implements ObjectGraph {\n" +
            "  private static final ClassIndexHashTable classIndexHashTable;\n" +
            "\n" +
            "  static {\n" +
            "    classIndexHashTable = new ClassIndexHashTable(11);\n" +
            "    classIndexHashTable.put(A.class, (char) 0);\n" +
            "    classIndexHashTable.put(C.class, (char) 1);\n" +
            "    classIndexHashTable.put(B.class, (char) 2);\n" +
            "    classIndexHashTable.put(D.class, (char) 3);\n" +
            "    classIndexHashTable.put(I2.class, (char) 4);\n" +
            "    classIndexHashTable.put(I.class, (char) 5);\n" +
            "  }\n" +
            "\n" +
            "  private final SimpleComponent component;\n" +
            "\n" +
            "  public BulletSimpleComponent(final SimpleComponent component) {\n" +
            "    this.component = component;\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T get(final Class<T> type) {\n" +
            "    throw new IllegalArgumentException(\"No get or Provides method found for \" + type.getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T inject(final T instance) {\n" +
            "    Class<?> c = instance.getClass();\n" +
            "    while (c != Object.class) {\n" +
            "      switch (classIndexHashTable.get(c)) {\n" +
            "        case 0:\n" +
            "          this.component.inject((A) instance);\n" +
            "          return instance;\n" +
            "        case 1:\n" +
            "          this.component.inject((C) instance);\n" +
            "          return instance;\n" +
            "        case 2:\n" +
            "          this.component.inject((B) instance);\n" +
            "          return instance;\n" +
            "        case 3:\n" +
            "          this.component.inject((D) instance);\n" +
            "          return instance;\n" +
            "        case 4:\n" +
            "          this.component.inject((I2) instance);\n" +
            "          return instance;\n" +
            "        case 5:\n" +
            "          this.component.inject((I) instance);\n" +
            "          return instance;\n" +
            "      }\n" +
            "      c = c.getSuperclass();\n" +
            "    }\n" +
            "    throw new IllegalArgumentException(\"No inject or MembersInject method found for \" + instance.getClass().getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "}");
    assert_().about(javaSources()).that(ImmutableList.of(iFile, i2File, aFile, bFile, cFile, dFile, eFile, componentFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(generatedBullet);
  }

  @Test public void membersInjectionTypePrecedence2() {
    // See https://code.google.com/p/google-web-toolkit/issues/detail?id=8036
    JavaFileObject dFile = JavaFileObjects.forSourceLines("test.D",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class D {",
        "  @Inject D() {}",
        "}");
    JavaFileObject aFile = JavaFileObjects.forSourceLines("test.A",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class A {",
        "  @Inject D d;",
        "}");
    JavaFileObject bFile = JavaFileObjects.forSourceLines("test.B",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "class B {",
        "  @Inject D d;",
        "}");
    JavaFileObject cFile = JavaFileObjects.forSourceLines("test.C",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "import javax.inject.Provider;",
        "",
        "final class C extends A {",
        "  @Inject B b;",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.SimpleComponent",
        "package test;",
        "",
        "import " + getComponentType().getCanonicalName() + ";",
        "",
        "@" + getComponentType().getSimpleName(),
        "interface SimpleComponent {",
        "  void inject(B b);",
        "  void inject(A a);",
        "  void inject(C c);",
        "}");
    JavaFileObject generatedBullet = JavaFileObjects.forSourceLines("test.BulletSimpleComponent",
        "package test;\n" +
            "\n" +
            "import bullet.ObjectGraph;\n" +
            "import bullet.impl.ClassIndexHashTable;\n" +
            "import java.lang.Class;\n" +
            "import java.lang.IllegalArgumentException;\n" +
            "import java.lang.Override;\n" +
            "import javax.annotation.Generated;\n" +
            "\n" +
            "@Generated(\"bullet.impl.ComponentProcessor\")\n" +
            "public final class BulletSimpleComponent implements ObjectGraph {\n" +
            "  private static final ClassIndexHashTable classIndexHashTable;\n" +
            "\n" +
            "  static {\n" +
            "    classIndexHashTable = new ClassIndexHashTable(5);\n" +
            "    classIndexHashTable.put(C.class, (char) 0);\n" +
            "    classIndexHashTable.put(A.class, (char) 1);\n" +
            "    classIndexHashTable.put(B.class, (char) 2);\n" +
            "  }\n" +
            "\n" +
            "  private final SimpleComponent component;\n" +
            "\n" +
            "  public BulletSimpleComponent(final SimpleComponent component) {\n" +
            "    this.component = component;\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T get(final Class<T> type) {\n" +
            "    throw new IllegalArgumentException(\"No get or Provides method found for \" + type.getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T inject(final T instance) {\n" +
            "    Class<?> c = instance.getClass();\n" +
            "    while (c != Object.class) {\n" +
            "      switch (classIndexHashTable.get(c)) {\n" +
            "        case 0:\n" +
            "          this.component.inject((C) instance);\n" +
            "          return instance;\n" +
            "        case 1:\n" +
            "          this.component.inject((A) instance);\n" +
            "          return instance;\n" +
            "        case 2:\n" +
            "          this.component.inject((B) instance);\n" +
            "          return instance;\n" +
            "      }\n" +
            "      c = c.getSuperclass();\n" +
            "    }\n" +
            "    throw new IllegalArgumentException(\"No inject or MembersInject method found for \" + instance.getClass().getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "}");
    assert_().about(javaSources()).that(ImmutableList.of(dFile, aFile, bFile, cFile, componentFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(generatedBullet);
  }

  @Test public void nonVisibleMethods() {
    JavaFileObject aFile = JavaFileObjects.forSourceLines("other.A",
        "package other;",
        "",
        "import javax.inject.Inject;",
        "",
        "public final class A {",
        "  @Inject A() {}",
        "}");
    JavaFileObject bFile = JavaFileObjects.forSourceLines("other.B",
        "package other;",
        "",
        "import javax.inject.Inject;",
        "",
        "public final class B {",
        "  @Inject B() {}",
        "}");
    JavaFileObject cFile = JavaFileObjects.forSourceLines("other.C",
        "package other;",
        "",
        "import javax.inject.Inject;",
        "",
        "public final class C {",
        "  @Inject C() {}",
        "}");
    JavaFileObject dFile = JavaFileObjects.forSourceLines("other.D",
        "package other;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class D {",
        "  @Inject D() {}",
        "}");
    JavaFileObject superComponentInOtherPackageFile = JavaFileObjects.forSourceLines("other.SuperComponentInOtherPackage",
        "package other;",
        "",
        "public class SuperComponentInOtherPackage {",
        "  public A publicMethodInOtherPackage() { return null; }",
        "  protected B protectedMethodInOtherPackage() { return null; }",
        "  private C privateMethod() { return null; }",
        "  public D nonVisibleReturnType() { return null; }",
        "}");
    JavaFileObject eFile = JavaFileObjects.forSourceLines("test.E",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class E {",
        "  @Inject E() {}",
        "}");
    JavaFileObject fFile = JavaFileObjects.forSourceLines("test.F",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class F {",
        "  @Inject F() {}",
        "}");
    JavaFileObject superComponentFile = JavaFileObjects.forSourceLines("test.SuperComponent",
        "package test;",
        "",
        "import other.SuperComponentInOtherPackage;",
        "",
        "class SuperComponent extends SuperComponentInOtherPackage {",
        "  protected E protectedMethodInSamePackage() { return null; }",
        "  private F privateMethod() { return null; }",
        "}");
    JavaFileObject gFile = JavaFileObjects.forSourceLines("test.G",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class G {",
        "  @Inject G() {}",
        "}");
    JavaFileObject hFile = JavaFileObjects.forSourceLines("test.H",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class H {",
        "  @Inject H() {}",
        "}");
    JavaFileObject iFile = JavaFileObjects.forSourceLines("test.I",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class I {",
        "  @Inject I() {}",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.SimpleComponent",
        "package test;",
        "",
        "import " + getComponentType().getCanonicalName() + ";",
        "",
        "@" + getComponentType().getSimpleName(),
        "class SimpleComponent extends SuperComponent {",
        "  protected G protectedMethod() { return null; }",
        "  private H privateMethod() { return null; }",
        "}");
    JavaFileObject generatedBullet = JavaFileObjects.forSourceLines("test.BulletSimpleComponent",
        "package test;\n" +
            "\n" +
            "import bullet.ObjectGraph;\n" +
            "import java.lang.Class;\n" +
            "import java.lang.IllegalArgumentException;\n" +
            "import java.lang.Override;\n" +
            "import javax.annotation.Generated;\n" +
            "import other.A;\n" +
            "\n" +
            "@Generated(\"bullet.impl.ComponentProcessor\")\n" +
            "public final class BulletSimpleComponent implements ObjectGraph {\n" +
            "  private final SimpleComponent component;\n" +
            "\n" +
            "  public BulletSimpleComponent(final SimpleComponent component) {\n" +
            "    this.component = component;\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T get(final Class<T> type) {\n" +
            "    if (type == A.class) {\n" +
            "      return type.cast(this.component.publicMethodInOtherPackage());\n" +
            "    }\n" +
            "    if (type == E.class) {\n" +
            "      return type.cast(this.component.protectedMethodInSamePackage());\n" +
            "    }\n" +
            "    if (type == G.class) {\n" +
            "      return type.cast(this.component.protectedMethod());\n" +
            "    }\n" +
            "    throw new IllegalArgumentException(\"No get or Provides method found for \" + type.getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T inject(final T instance) {\n" +
            "    throw new IllegalArgumentException(\"No inject or MembersInject method found for \" + instance.getClass().getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "}");
    assert_().about(javaSources()).that(ImmutableList.of(
        aFile, bFile, cFile, dFile, eFile, fFile, gFile, hFile, iFile,
        superComponentInOtherPackageFile, superComponentFile, componentFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(generatedBullet);
  }

  @Test public void provider() {
    JavaFileObject injectableTypeFile = JavaFileObjects.forSourceLines("test.SomeInjectableType",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class SomeInjectableType {",
        "  @Inject SomeInjectableType() {}",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.SimpleComponent",
        "package test;",
        "",
        "import javax.inject.Provider;",
        "import " + getComponentType().getCanonicalName() + ";",
        "",
        "@" + getComponentType().getSimpleName(),
        "interface SimpleComponent {",
        "  Provider<SomeInjectableType> someInjectableType();",
        "}");
    JavaFileObject generatedBullet = JavaFileObjects.forSourceLines("test.BulletSimpleComponent",
        "package test;\n" +
            "\n" +
            "import bullet.ObjectGraph;\n" +
            "import java.lang.Class;\n" +
            "import java.lang.IllegalArgumentException;\n" +
            "import java.lang.Override;\n" +
            "import javax.annotation.Generated;\n" +
            "\n" +
            "@Generated(\"bullet.impl.ComponentProcessor\")\n" +
            "public final class BulletSimpleComponent implements ObjectGraph {\n" +
            "  private final SimpleComponent component;\n" +
            "\n" +
            "  public BulletSimpleComponent(final SimpleComponent component) {\n" +
            "    this.component = component;\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T get(final Class<T> type) {\n" +
            "    if (type == SomeInjectableType.class) {\n" +
            "      return type.cast(this.component.someInjectableType().get());\n" +
            "    }\n" +
            "    throw new IllegalArgumentException(\"No get or Provides method found for \" + type.getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T inject(final T instance) {\n" +
            "    throw new IllegalArgumentException(\"No inject or MembersInject method found for \" + instance.getClass().getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "}");
    assert_().about(javaSources()).that(ImmutableList.of(injectableTypeFile, componentFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(generatedBullet);
  }

  @Test public void lazy() {
    JavaFileObject injectableTypeFile = JavaFileObjects.forSourceLines("test.SomeInjectableType",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class SomeInjectableType {",
        "  @Inject SomeInjectableType() {}",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.SimpleComponent",
        "package test;",
        "",
        "import dagger.Lazy;",
        "import " + getComponentType().getCanonicalName() + ";",
        "",
        "@" + getComponentType().getSimpleName(),
        "interface SimpleComponent {",
        "  Lazy<SomeInjectableType> someInjectableType();",
        "}");

    JavaFileObject generatedBullet = JavaFileObjects.forSourceLines("test.BulletSimpleComponent",
        "package test;\n" +
            "\n" +
            "import bullet.ObjectGraph;\n" +
            "import java.lang.Class;\n" +
            "import java.lang.IllegalArgumentException;\n" +
            "import java.lang.Override;\n" +
            "import javax.annotation.Generated;\n" +
            "\n" +
            "@Generated(\"bullet.impl.ComponentProcessor\")\n" +
            "public final class BulletSimpleComponent implements ObjectGraph {\n" +
            "  private final SimpleComponent component;\n" +
            "\n" +
            "  public BulletSimpleComponent(final SimpleComponent component) {\n" +
            "    this.component = component;\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T get(final Class<T> type) {\n" +
            "    if (type == SomeInjectableType.class) {\n" +
            "      return type.cast(this.component.someInjectableType().get());\n" +
            "    }\n" +
            "    throw new IllegalArgumentException(\"No get or Provides method found for \" + type.getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T inject(final T instance) {\n" +
            "    throw new IllegalArgumentException(\"No inject or MembersInject method found for \" + instance.getClass().getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "}");
    assert_().about(javaSources()).that(ImmutableList.of(injectableTypeFile, componentFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(generatedBullet);
  }

  @Test public void membersInjector() {
    JavaFileObject aFile = JavaFileObjects.forSourceLines("test.A",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class A {",
        "  @Inject A() {}",
        "}");
    JavaFileObject bFile = JavaFileObjects.forSourceLines("test.B",
        "package test;",
        "",
        "import javax.inject.Inject;",
        "",
        "final class B {",
        "  @Inject A a;",
        "}");
    JavaFileObject componentFile = JavaFileObjects.forSourceLines("test.SimpleComponent",
        "package test;",
        "",
        "import dagger.MembersInjector;",
        "import " + getComponentType().getCanonicalName() + ";",
        "",
        "@" + getComponentType().getSimpleName(),
        "interface SimpleComponent {",
        "  MembersInjector<B> b();",
        "}");
    JavaFileObject generatedBullet = JavaFileObjects.forSourceLines("test.BulletSimpleComponent",
        "package test;\n" +
            "\n" +
            "import bullet.ObjectGraph;\n" +
            "import bullet.impl.ClassIndexHashTable;\n" +
            "import java.lang.Class;\n" +
            "import java.lang.IllegalArgumentException;\n" +
            "import java.lang.Override;\n" +
            "import javax.annotation.Generated;\n" +
            "\n" +
            "@Generated(\"bullet.impl.ComponentProcessor\")\n" +
            "public final class BulletSimpleComponent implements ObjectGraph {\n" +
            "  private static final ClassIndexHashTable classIndexHashTable;\n" +
            "\n" +
            "  static {\n" +
            "    classIndexHashTable = new ClassIndexHashTable(3);\n" +
            "    classIndexHashTable.put(B.class, (char) 0);\n" +
            "  }\n" +
            "\n" +
            "  private final SimpleComponent component;\n" +
            "\n" +
            "  public BulletSimpleComponent(final SimpleComponent component) {\n" +
            "    this.component = component;\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T get(final Class<T> type) {\n" +
            "    throw new IllegalArgumentException(\"No get or Provides method found for \" + type.getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "\n" +
            "  @Override\n" +
            "  public <T> T inject(final T instance) {\n" +
            "    Class<?> c = instance.getClass();\n" +
            "    while (c != Object.class) {\n" +
            "      switch (classIndexHashTable.get(c)) {\n" +
            "        case 0:\n" +
            "          this.component.b().injectMembers((B) instance);\n" +
            "          return instance;\n" +
            "      }\n" +
            "      c = c.getSuperclass();\n" +
            "    }\n" +
            "    throw new IllegalArgumentException(\"No inject or MembersInject method found for \" + instance.getClass().getName() + \" in SimpleComponent\");\n" +
            "  }\n" +
            "}");
    assert_().about(javaSources()).that(ImmutableList.of(aFile, bFile, componentFile))
        .processedWith(new ComponentProcessor())
        .compilesWithoutError()
        .and().generatesSources(generatedBullet);
  }
}
