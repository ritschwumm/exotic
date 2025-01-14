# exotic [![Continuous Integration](https://github.com/forax/exotic/actions/workflows/main.yml/badge.svg)](https://github.com/forax/exotic/actions/workflows/main.yml)
A bestiary of classes implementing exotic semantics in Java

In Java, a static final field is considered as a constant by the virtual machine,
but a final field of an object which is a constant is not itself considered as a constant.
Exotic allows to see a constant's field as a constant, a result of a calculation as a constant,
to change at runtime the value of a constant, etc. 

This library run on Java 8+ and is fully compatible with Java 9 modules.

This library needs Java 11+ to be built.

### MostlyConstant - [javadoc](https://jitpack.io/com/github/forax/exotic/master/javadoc/com/github/forax/exotic/MostlyConstant.html)

A constant for the VM that can be changed by de-optimizing all the codes that contain the previous value of the constant.

```java
private static final MostlyConstant<Integer> FOO = new MostlyConstant<>(42, int.class);
private static final IntSupplier FOO_GETTER = FOO.intGetter();

public static int getFoo() {
  return FOO_GETTER.getAsInt();
}
public static void setFoo(int value) {
   FOO.setAndDeoptimize(value);
}
```

### StableField - [javadoc](https://jitpack.io/com/github/forax/exotic/master/javadoc/com/github/forax/exotic/StableField.html)

A field that becomes a constant if the object itsef is constant and the field is initialized

```java
enum Option {
  a, b;
    
  private static final Function<Option, String> UPPERCASE =
      StableField.getter(lookup(), Option.class, "uppercase", String.class);
    
  private String uppercase;  // stable

  public String upperCase() {
    String uppercase = UPPERCASE.apply(this);
    if (uppercase != null) {
      return uppercase;
    }
    return this.uppercase = name().toUpperCase();
  }
}
...
Option.a.upperCase()  // constant "A"
```

### ConstantMemoizer - [javadoc](https://jitpack.io/com/github/forax/exotic/master/javadoc/com/github/forax/exotic/ConstantMemoizer.html) 

A function that returns a constant value if its parameter is a constant.

```java
private static final ToIntFunction<Level> MEMOIZER =
    ConstantMemoizer.intMemoizer(Level::ordinal, Level.class);
...
MEMOIZER.applyAsInt("foo") // constant 3
```

### ObjectSupport - [javadoc](https://jitpack.io/com/github/forax/exotic/master/javadoc/com/github/forax/exotic/ObjectSupport.html) 

Provide a fast implementation for equals() and hashCode().

```java
class Person {
  private static final ObjectSupport<Person> SUPPORT =
      ObjectSupport.of(lookup(), Person.class, p -> p.name);
    
  private String name;
  ...
  
  public boolean equals(Object other) {
    return SUPPORT.equals(this, other);
  }
    
  public int hashCode() {
    return SUPPORT.hashCode(this);
  }
}
```

### StructuralCall - [javadoc](https://jitpack.io/com/github/forax/exotic/master/javadoc/com/github/forax/exotic/StructuralCall.html)

A method call that can call different method implementations if they share the same name and same parameter types.

```java
private static final StructuralCall IS_EMPTY =
    StructuralCall.create(lookup(), "isEmpty", methodType(boolean.class));

static boolean isEmpty(Object o) {  // can be called with a Map, a Collection or a String
  return IS_EMPTY.invoke(o);
}
```

### Visitor - [javadoc](https://jitpack.io/com/github/forax/exotic/master/javadoc/com/github/forax/exotic/Visitor.html)

An open visitor, a visitor that does allow new types and new operations, can be implemented using a Map
that associates a class to a lambda, but this implementation loose inlining thus perform badly compared to the Gof Visitor.
This class implements an open visitor that's used inlining caches.

```java
private static final Visitor<Void, Integer> VISITOR =
    Visitor.create(Void.class, int.class, opt -> opt
      .register(Value.class, (v, value, __) -> value.value)
      .register(Add.class,   (v, add, __)   -> v.visit(add.left, null) + v.visit(add.right, null))
    );
...
Expr expr = new Add(new Add(new Value(7), new Value(10)), new Value(4));
int value = VISITOR.visit(expr, null);  // 21
```

### TypeSwitch - [javadoc](https://jitpack.io/com/github/forax/exotic/master/javadoc/com/github/forax/exotic/TypeSwitch.html)

Express a switch on type as function from an object to an index + a classical switch on the possible indexes.
The TypeSwitch should be more efficient than a cascade of if instanceof.  

```java
private static final TypeSwitch TYPE_SWITCH = TypeSwitch.create(true, Integer.class, String.class);
  
public static String asString(Object o) {
  switch(TYPE_SWITCH.typeSwitch(o)) {
  case TypeSwitch.NULL_MATCH:
    return "null";
  case 0:
    return "Integer";
  case 1:
    return "String";
  default: // TypeSwitch.NO_MATCH
    return "unknown";
  }
}
```


## Build Tool Integration [![](https://jitpack.io/v/forax/exotic.svg)](https://jitpack.io/#forax/exotic)

Get latest binary distribution via [JitPack](https://jitpack.io/#forax/exotic) 


### Maven

    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>
    <dependency>
        <groupId>com.github.forax</groupId>
        <artifactId>exotic</artifactId>
        <version>master-SNAPSHOT</version>
    </dependency>


### Gradle

    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        compile 'com.github.forax:exotic:master-SNAPSHOT'
    }
