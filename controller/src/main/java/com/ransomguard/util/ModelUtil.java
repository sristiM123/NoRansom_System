package com.ransomguard.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ModelUtil {
    private ModelUtil() {}

    // Try setters first, then direct fields. Never crashes compile-time.
    public static void set(Object obj, String[] names, Object value) {
        if (obj == null) return;
        Class<?> c = obj.getClass();

        // 1) Try setter methods: setXxx(...)
        for (String name : names) {
            String m = "set" + capitalize(name);
            Method[] methods = c.getMethods();
            for (Method method : methods) {
                if (!method.getName().equals(m)) continue;
                if (method.getParameterCount() != 1) continue;
                try {
                    Object v = coerce(value, method.getParameterTypes()[0]);
                    method.invoke(obj, v);
                    return;
                } catch (Exception ignored) {}
            }
        }

        // 2) Try direct field
        for (String name : names) {
            try {
                Field f = findField(c, name);
                if (f == null) continue;
                f.setAccessible(true);
                Object v = coerce(value, f.getType());
                f.set(obj, v);
                return;
            } catch (Exception ignored) {}
        }
    }

    public static Object get(Object obj, String[] names) {
        if (obj == null) return null;
        Class<?> c = obj.getClass();

        // 1) Try getters: getXxx() / isXxx()
        for (String name : names) {
            String g1 = "get" + capitalize(name);
            String g2 = "is" + capitalize(name);

            try {
                Method m = c.getMethod(g1);
                return m.invoke(obj);
            } catch (Exception ignored) {}

            try {
                Method m = c.getMethod(g2);
                return m.invoke(obj);
            } catch (Exception ignored) {}
        }

        // 2) Try direct field
        for (String name : names) {
            try {
                Field f = findField(c, name);
                if (f == null) continue;
                f.setAccessible(true);
                return f.get(obj);
            } catch (Exception ignored) {}
        }

        return null;
    }

    private static Field findField(Class<?> c, String name) {
        Class<?> cur = c;
        while (cur != null) {
            try { return cur.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { cur = cur.getSuperclass(); }
        }
        return null;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static Object coerce(Object value, Class<?> target) {
        if (value == null) {
            if (target == boolean.class) return false;
            if (target == long.class) return 0L;
            if (target == int.class) return 0;
            return null;
        }
        if (target.isInstance(value)) return value;

        if (target == String.class) return String.valueOf(value);

        if (target == long.class || target == Long.class) {
            if (value instanceof Number n) return n.longValue();
            return Long.parseLong(String.valueOf(value));
        }

        if (target == int.class || target == Integer.class) {
            if (value instanceof Number n) return n.intValue();
            return Integer.parseInt(String.valueOf(value));
        }

        if (target == boolean.class || target == Boolean.class) {
            if (value instanceof Boolean b) return b;
            return Boolean.parseBoolean(String.valueOf(value));
        }

        return value; // best effort
    }
}
