Bullet
======

Provides Dagger1-like ObjectGraph API around Dagger2 Components, without using reflection.

Download
--------

Releases are deployed to [the Central Repository][releases]
 [releases]: https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.wemakebetterapps%22
 
```groovy
compile 'com.wemakebetterapps:bullet:0.21'
provided 'com.wemakebetterapps:bullet-compiler:0.21'
```

If using Gradle and Android, you could use [android-apt](https://bitbucket.org/hvisser/android-apt):
```groovy
compile 'com.wemakebetterapps:bullet:0.21'
apt 'com.wemakebetterapps:bullet-compiler:0.21'
```
 
```xml
<dependencies>
  <dependency>
    <groupId>com.wemakebetterapps</groupId>
    <artifactId>bullet</artifactId>
    <version>${bullet.version}</version>
  </dependency>
  <dependency>
    <groupId>com.wemakebetterapps</groupId>
    <artifactId>bullet-compiler</artifactId>
    <version>${bullet.version}</version>
    <optional>true</optional>
  </dependency>
</dependencies>
```

Example
-------

```java
@Singleton
@Component(modules = ModuleA.class)
public interface ComponentA {
    void inject(ClassToInject obj);
    ClassA getClassA();
}
```

```java
@Module
public class ModuleA {
    @Provides @Singleton ClassA providesClassA() {
         return new ClassA();
    }
}
```

```java
public class ClassA {
}
```

```java
public class ClassToInject {
    @Inject ClassA classA;
}
```

```java
// 1. Create Dagger 2 Component
ComponentA componentA = DaggerComponentA.builder().build();

// 2. Create Bullet Component using the Dagger 2 Component
ObjectGraph objectGraph = new BulletComponentA(componentA);

// 3. The Bullet Component implements the ObjectGraph interface.

// Inject any Class that has an 'inject', or 'MembersInject' method on the component.
ClassToInject classToInject = new ClassToInject();
objectGraph.inject(classToInject);

// Get any object by Class that has a `get`, `Lazy`, or `Provider` method on the component.
ClassA classASingleton = objectGraph.get(ClassA.class);
```

Proguard
--------

Just like Dagger 2, Bullet doesn't use any reflection, and thus is completely compatible with Proguard.

How does it work?
-----------------

Bullet is an annotation processor triggered by Dagger 2's `@Component` and `@Subcomponent` annotations.
All you need to do is put Bullet in your processor path. It should work with all component configurations.

License
-------

    Copyright 2014 Thomas Broyer

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


