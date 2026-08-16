package gg.topchdlc.api.scripts.api.bindings;

import gg.topchdlc.vse.utils.mapper.FabricMapper;
import org.graalvm.polyglot.HostAccess;

import java.lang.reflect.*;

public class ReflectProvider {

    @HostAccess.Export
    public Class<?> findClass(String obfName) throws ClassNotFoundException {
        String real = FabricMapper.remapClassName(obfName);
        if (!real.toLowerCase().startsWith("net.") && !real.toLowerCase().startsWith("org."))
            throw new ClassNotFoundException(String.format("Class access exception: %s", obfName));
        try {
            return Class.forName(real);
        } catch (ClassNotFoundException e) {
            throw new ClassNotFoundException(String.format("Class not found: %s", obfName), e);
        }
    }

    @HostAccess.Export
    public Object newInstance(String classObf, Object... args) throws ReflectiveOperationException {
        Class<?> cls = findClass(classObf);
        Constructor<?> ctor = findBestConstructor(cls, args);
        makeAccessible(ctor);
        Object[] conv = convertArgsFor(ctor.getParameterTypes(), args, ctor.isVarArgs());
        return ctor.newInstance(conv);
    }

    @HostAccess.Export
    public Object callStatic(String classObf, String methodObf, Object... args) throws ReflectiveOperationException {
        Class<?> cls = findClass(classObf);
        String methodName = FabricMapper.remapMethodName(cls.getName(), methodObf);
        Method m = findBestMethod(cls, methodName, true, args);
        makeAccessible(m);
        Object[] conv = convertArgsFor(m.getParameterTypes(), args, m.isVarArgs());
        return m.invoke(null, conv);
    }

    @HostAccess.Export
    public Object callInstance(Object instance, String methodObf, Object... args) throws ReflectiveOperationException {
        if (instance == null) throw new IllegalArgumentException("instance == null");
        Class<?> cls = instance.getClass();
        String methodName = FabricMapper.remapMethodName(cls.getName(), methodObf);
        Method m = findBestMethod(cls, methodName, false, args);
        makeAccessible(m);
        Object[] conv = convertArgsFor(m.getParameterTypes(), args, m.isVarArgs());
        return m.invoke(instance, conv);
    }

    @HostAccess.Export
    public Object call(String classOrInstance, Object targetOrNull, String methodObf, Object... args) throws ReflectiveOperationException {
        if (targetOrNull == null) {
            return callStatic(classOrInstance, methodObf, args);
        } else {
            return callInstance(targetOrNull, methodObf, args);
        }
    }

    @HostAccess.Export
    public Object getFieldValue(String classOrInstanceObf, Object instanceIfAny, String fieldObf) throws ReflectiveOperationException {
        Class<?> cls = findClass(classOrInstanceObf);
        String fieldName = FabricMapper.remapFieldName(cls.getName(), fieldObf);
        Field f = findFieldInHierarchy(cls, fieldName);
        makeAccessible(f);
        return f.get(instanceIfAny);
    }

    @HostAccess.Export
    public void setFieldValue(String classOrInstanceObf, Object instanceIfAny, String fieldObf, Object value) throws ReflectiveOperationException {
        Class<?> cls = findClass(classOrInstanceObf);
        String fieldName = FabricMapper.remapFieldName(cls.getName(), fieldObf);
        Field f = findFieldInHierarchy(cls, fieldName);
        makeAccessible(f);
        Object converted = convertSingleArgFor(f.getType(), value);
        f.set(instanceIfAny, converted);
    }

    private Field findFieldInHierarchy(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> cur = cls;
        while (cur != null) {
            try {
                return cur.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                cur = cur.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field " + name + " not found in " + cls.getName());
    }

    private Constructor<?> findBestConstructor(Class<?> cls, Object[] args) throws NoSuchMethodException {
        Constructor<?> best = null;
        for (Constructor<?> c : cls.getDeclaredConstructors()) {
            if (isCallableWith(c.getParameterTypes(), args, c.isVarArgs())) {
                if (best == null || betterMatch(best.getParameterTypes(), c.getParameterTypes(), args)) {
                    best = c;
                }
            }
        }
        if (best == null) throw new NoSuchMethodException("No matching constructor for " + cls.getName());
        return best;
    }

    private Method findBestMethod(Class<?> cls, String methodName, boolean wantStatic, Object[] args) throws NoSuchMethodException {
        Method best = null;
        Class<?> cur = cls;
        while (cur != null) {
            for (Method m : cur.getDeclaredMethods()) {
                if (!m.getName().equals(methodName)) continue;
                if (Modifier.isStatic(m.getModifiers()) != wantStatic) continue;
                if (!isCallableWith(m.getParameterTypes(), args, m.isVarArgs())) continue;
                if (best == null || betterMatch(best.getParameterTypes(), m.getParameterTypes(), args)) {
                    best = m;
                }
            }
            cur = cur.getSuperclass();
        }
        if (best == null) throw new NoSuchMethodException("Method " + methodName + " not found in " + cls.getName());
        return best;
    }

    private boolean isCallableWith(Class<?>[] paramTypes, Object[] args, boolean varargs) {
        if (varargs) {
            if (args.length < paramTypes.length - 1) return false;
        } else {
            if (paramTypes.length != args.length) return false;
        }
        for (int i = 0; i < paramTypes.length; i++) {
            if (varargs && i == paramTypes.length - 1) {
                Class<?> comp = paramTypes[i].getComponentType();
                for (int j = i; j < args.length; j++) {
                    if (!isCompatible(comp, args[j])) return false;
                }
                break;
            } else {
                if (i >= args.length) return false;
                if (!isCompatible(paramTypes[i], args[i])) return false;
            }
        }
        return true;
    }

    private boolean isCompatible(Class<?> paramType, Object arg) {
        if (arg == null) return !paramType.isPrimitive();
        Class<?> aClass = unwrapPolyglot(arg).getClass();
        if (paramType.isPrimitive()) {
            return primitiveAccepts(paramType, aClass);
        } else {
            return box(paramType).isAssignableFrom(box(aClass));
        }
    }

    private boolean primitiveAccepts(Class<?> primitive, Class<?> provided) {
        if (primitive == boolean.class) return provided == Boolean.class || provided == boolean.class;
        if (primitive == byte.class) return provided == Byte.class || provided == byte.class;
        if (primitive == char.class) return provided == Character.class || provided == char.class;
        if (primitive == short.class) return provided == Short.class || provided == short.class || provided == Byte.class;
        if (primitive == int.class) return provided == Integer.class || provided == int.class || provided == Short.class || provided == Byte.class || provided == Character.class;
        if (primitive == long.class) return provided == Long.class || provided == long.class || provided == Integer.class || provided == Short.class || provided == Byte.class || provided == Character.class;
        if (primitive == float.class) return provided == Float.class || provided == float.class || provided == Long.class || provided == Integer.class || provided == Short.class || provided == Byte.class;
        if (primitive == double.class) return provided == Double.class || provided == double.class || provided == Float.class || provided == Long.class || provided == Integer.class || provided == Short.class || provided == Byte.class;
        return false;
    }

    private Class<?> box(Class<?> c) {
        if (!c.isPrimitive()) return c;
        if (c == boolean.class) return Boolean.class;
        if (c == byte.class) return Byte.class;
        if (c == char.class) return Character.class;
        if (c == short.class) return Short.class;
        if (c == int.class) return Integer.class;
        if (c == long.class) return Long.class;
        if (c == float.class) return Float.class;
        if (c == double.class) return Double.class;
        return c;
    }
    private boolean betterMatch(Class<?>[] current, Class<?>[] candidate, Object[] args) {
        int scoreCur = 0, scoreCand = 0;
        for (int i = 0; i < candidate.length && i < args.length; i++) {
            Class<?> a = current[Math.min(i, current.length-1)];
            Class<?> b = candidate[Math.min(i, candidate.length-1)];
            Class<?> actual = unwrapPolyglot(args[i]).getClass();
            if (a == actual) scoreCur += 2;
            if (b == actual) scoreCand += 2;
            if (a.isAssignableFrom(actual)) scoreCur++;
            if (b.isAssignableFrom(actual)) scoreCand++;
        }
        return scoreCand > scoreCur;
    }

    private void makeAccessible(AccessibleObject ao) {
        try {
            ao.setAccessible(true);
        } catch (Throwable ignored) {}
    }
    private Object[] convertArgsFor(Class<?>[] paramTypes, Object[] args, boolean varargs) {
        Object[] res = new Object[paramTypes.length];
        if (varargs) {
            int fixed = paramTypes.length - 1;
            for (int i = 0; i < fixed; i++) {
                res[i] = convertSingleArgFor(paramTypes[i], args.length > i ? args[i] : null);
            }
            Class<?> comp = paramTypes[fixed].getComponentType();
            int varCount = Math.max(0, args.length - fixed);
            Object varArray = Array.newInstance(comp, varCount);
            for (int i = 0; i < varCount; i++) {
                Array.set(varArray, i, convertSingleArgFor(comp, args[fixed + i]));
            }
            res[fixed] = varArray;
        } else {
            for (int i = 0; i < paramTypes.length; i++) {
                res[i] = convertSingleArgFor(paramTypes[i], args.length > i ? args[i] : null);
            }
        }
        return res;
    }

    private Object convertSingleArgFor(Class<?> paramType, Object arg) {
        Object un = unwrapPolyglot(arg);
        if (un == null) return null;
        if (paramType.isPrimitive()) {
            if (un instanceof Number) {
                Number n = (Number) un;
                if (paramType == int.class) return n.intValue();
                if (paramType == long.class) return n.longValue();
                if (paramType == short.class) return n.shortValue();
                if (paramType == byte.class) return n.byteValue();
                if (paramType == float.class) return n.floatValue();
                if (paramType == double.class) return n.doubleValue();
                if (paramType == char.class) return (char) n.intValue();
            }
            if (paramType == boolean.class && un instanceof Boolean) return un;
        }
        if (paramType.isInstance(un)) return un;
        if (paramType.isEnum() && un instanceof String) {
            @SuppressWarnings({"rawtypes","unchecked"})
            Enum e = Enum.valueOf((Class<? extends Enum>)paramType, (String)un);
            return e;
        }
        if (Number.class.isAssignableFrom(paramType) && un instanceof Number) {
            Number n = (Number)un;
            if (paramType == Integer.class) return n.intValue();
            if (paramType == Long.class) return n.longValue();
            if (paramType == Short.class) return n.shortValue();
            if (paramType == Byte.class) return n.byteValue();
            if (paramType == Float.class) return n.floatValue();
            if (paramType == Double.class) return n.doubleValue();
        }
        return un;
    }
    private Object unwrapPolyglot(Object maybePoly) {
        if (maybePoly == null) return null;
        try {
            Class<?> valueClass = Class.forName("org.graalvm.polyglot.Value");
            if (valueClass.isInstance(maybePoly)) {
                Method isHostObject = valueClass.getMethod("isHostObject");
                Boolean host = (Boolean)isHostObject.invoke(maybePoly);
                if (host) return valueClass.getMethod("asHostObject").invoke(maybePoly);
                Method fitsLong = valueClass.getMethod("fitsInLong");
                if ((Boolean)fitsLong.invoke(maybePoly)) return valueClass.getMethod("asLong").invoke(maybePoly);
                Method fitsDouble = valueClass.getMethod("fitsInDouble");
                if ((Boolean)fitsDouble.invoke(maybePoly)) return valueClass.getMethod("asDouble").invoke(maybePoly);
                Method isString = valueClass.getMethod("isString");
                if ((Boolean)isString.invoke(maybePoly)) return valueClass.getMethod("asString").invoke(maybePoly);
                Method isBoolean = valueClass.getMethod("isBoolean");
                if ((Boolean)isBoolean.invoke(maybePoly)) return valueClass.getMethod("asBoolean").invoke(maybePoly);
                Method hasArrayElements = valueClass.getMethod("hasArrayElements");
                if ((Boolean)hasArrayElements.invoke(maybePoly)) {
                    long len = (Long)valueClass.getMethod("getArraySize").invoke(maybePoly);
                    Object[] arr = new Object[(int)len];
                    for (int i = 0; i < len; i++) {
                        Object el = valueClass.getMethod("getArrayElement", long.class).invoke(maybePoly, (long)i);
                        arr[i] = unwrapPolyglot(el);
                    }
                    return arr;
                }
                return valueClass.getMethod("as", Class.class).invoke(maybePoly, Object.class);
            }
        } catch (Throwable ignored) {}
        return maybePoly;
    }
}
