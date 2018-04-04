package com.github.forax.exotic;

import static java.lang.invoke.MethodHandles.exactInvoker;
import static java.lang.invoke.MethodHandles.foldArguments;
import static java.lang.invoke.MethodHandles.guardWithTest;
import static java.lang.invoke.MethodType.methodType;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.HashMap;
import java.util.Objects;

import com.github.forax.exotic.Visitor.Visitlet;

class VisitorCallSite extends MutableCallSite {
  private static final MethodHandle FALLBACK, TYPECHECK;
  static final MethodHandle VISIT;
  static {
    Lookup lookup = MethodHandles.lookup();
    try {
      FALLBACK = lookup.findVirtual(VisitorCallSite.class, "fallback", methodType(MethodHandle.class, Object.class));
      VISIT = lookup.findVirtual(Visitlet.class, "visit", methodType(Object.class, Visitor.class, Object.class, Object.class));
      TYPECHECK = lookup.findStatic(VisitorCallSite.class, "typecheck", methodType(boolean.class, Class.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  static <P, R> Visitor<P, R> visitor(Class<P> pType, Class<R> rType, HashMap<Class<?>, MethodHandle> map) {
    MethodHandle mh = new VisitorCallSite(MethodType.methodType(rType, Object.class, pType), map)
        .dynamicInvoker()
        .asType(methodType(Object.class, Object.class, Object.class));
    return (expr, parameter) -> {
      Objects.requireNonNull(expr);
      try {
        return (R)mh.invokeExact(expr, parameter);
      } catch(Throwable t) {
        throw Thrower.rethrow(t);
      }
    };
  }

  private final HashMap<Class<?>, MethodHandle> map;

  private VisitorCallSite(MethodType methodType, HashMap<Class<?>, MethodHandle> map) {
    super(methodType);
    this.map = map;
    setTarget(foldArguments(exactInvoker(methodType), FALLBACK.bindTo(this)));
  }

  @SuppressWarnings("unused")
  private MethodHandle fallback(Object o) {
    Class<?> receiverClass = o.getClass();
    MethodHandle target = map.get(receiverClass);
    if (target == null) {
      throw new IllegalStateException("no visitlet register for type " + receiverClass.getName());
    }
    MethodHandle guard = guardWithTest(TYPECHECK.bindTo(receiverClass),
        target,
        new VisitorCallSite(type(), map).dynamicInvoker());
    setTarget(guard);
    return target;
  }

  @SuppressWarnings("unused")
  private static boolean typecheck(Class<?> receiverClass, Object receiver) {
    return receiverClass == receiver.getClass();
  }
}