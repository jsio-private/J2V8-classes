package io.js.J2V8Classes;

import com.eclipsesource.v8.*;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Created by Brown on 4/26/16.
 */
public class Runtime {
    private static Logger logger = Logger.getLogger("Runtime");

    public static V8 getRuntime() {
        V8 runtime = V8.createV8Runtime();

        runtime.executeVoidScript(
                Utils.getScriptSource(
                        Runtime.class.getClassLoader(),
                        "js.class/dist/js.class.js"
                )
        );

        runtime.executeVoidScript(
                Utils.getScriptSource(
                        Runtime.class.getClassLoader(),
                        "jsClassHelper.js"
                )
        );


        JavaVoidCallback print = new JavaVoidCallback() {
            public void invoke(final V8Array parameters) {
                StringBuilder sb = new StringBuilder();
                sb.append("JS: ");
                for (int i = 0; i < parameters.length(); i++) {
                    Object obj = parameters.get(i);
                    sb.append(obj);
                    if (obj instanceof V8Value) {
                        ((V8Value) obj).release();
                    }
                }
                System.out.println(sb.toString());
            }
        };
        runtime.registerJavaMethod(print, "print");


        JavaCallback getClass = new JavaCallback() {
            public Object invoke(final V8Array parameters) {
                String className = (String) parameters.get(0);
                logger.info("Getting class: " + className);
                V8Object res = new V8Object(runtime);
                try {
                    V8Object statics = getClassStatics(runtime, className);
                    res.add("statics", statics);
                    statics.release();
                    res.add("found", true);
                } catch (ClassNotFoundException e) {
                    logger.warning("> Class not found");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
//                catch (InstantiationException e) {
//                    e.printStackTrace();
//                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
//                }

                return res;
            }
        };
        runtime.registerJavaMethod(getClass, "JavaGetClass");


        JavaCallback createInstance = new JavaCallback() {
            public V8Object invoke(final V8Array parameters) {
                String className = (String) parameters.get(0);
                try {
                    return createInstance(runtime, className, Utils.v8arrayToObjectArray(parameters, 1));
                } catch (ClassNotFoundException e) {
                    logger.warning("> Class not found");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        runtime.registerJavaMethod(createInstance, "JavaCreateInstance");

        return runtime;
    }


    private static V8Object getClassStatics(V8 runtime, String className) throws ClassNotFoundException, IllegalAccessException {
        logger.info("Getting class statics: " + className);
        Class clz = Class.forName(className);

        V8Object res = generateAllGetSet(runtime, clz, clz, true);

        return res;
    }


    private static V8Object createInstance(V8 runtime, String className, Object[] parameters) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        logger.info("Getting class instance: " + className);
        Class clz = Class.forName(className);
        Class[] parameterTypes = new Class[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getClass();
        }
        Constructor c = clz.getConstructor(parameterTypes);
//        Object[] passed = {parameters};
        Object instance = c.newInstance(parameters);

        return getV8ObjectForObject(runtime, instance);
    }

    private static V8Object generateAllGetSet(V8 runtime, Class clz, Object instance, boolean statics) {
        V8Object res = new V8Object(runtime);

        logger.info("Generating getters and setters for: " + clz.getName());

        logger.info("> Getting fields");
        Field[] f = clz.getDeclaredFields();
        V8Object jsF = new V8Object(runtime);
        for (int i = 0; i < f.length; i++) {
            if (Modifier.isStatic(f[i].getModifiers()) == statics) {
                generateGetSet(runtime, jsF, instance, f[i]);
            }
        }
        res.add("fields", jsF);
        jsF.release();

        logger.info("> Getting methods");
        Method[] m = clz.getDeclaredMethods();
        V8Object jsM = new V8Object(runtime);
        for (int i = 0; i < m.length; i++) {
            if (Modifier.isStatic(m[i].getModifiers()) == statics) {
                generateMethod(runtime, jsM, instance, m[i]);
            }
        }
        res.add("methods", jsM);
        jsM.release();

        return res;
    }

    private static void generateMethod(V8 runtime, V8Object parent, Object instance, Method m) {
        String mName = m.getName();
        logger.info(">> M: " + mName);
        if (Modifier.isStatic(m.getModifiers())) {
            JavaCallback staticMethod = new JavaCallback() {
                public V8Object invoke(final V8Array parameters) {
                    try {
                        Object v = m.invoke(instance, Utils.v8arrayToObjectArray(parameters));
                        return getReturnValue(runtime, v);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    return new V8Object(runtime);
                }
            };
            parent.registerJavaMethod(staticMethod, mName);
        } else {
            parent.registerJavaMethod(instance, mName, mName, m.getParameterTypes());
        }
    }

    private static V8Object getReturnValue(V8 runtime, Object v) {
        V8Object res = new V8Object(runtime);
        Class vClass = v.getClass();
        if (vClass == Boolean.class) {
            res.add("v", (boolean) v);
        } else if (vClass == Double.class) {
            res.add("v", (double) v);
        } else if (vClass == Integer.class) {
            res.add("v", (int) v);
        } else if (vClass == String.class) {
            res.add("v", (String) v);
        } else if (vClass instanceof Object) {
            logger.warning("> Class! " + vClass);
            V8Object jsInst = getV8ObjectForObject(runtime, v);
            res.add("v", jsInst);
            jsInst.release();
        } else {
            logger.warning("> Unknown type! " + vClass);
        }
        return res;
    }

    private static void generateGetSet(V8 runtime, V8Object parent, Object instance, Field f) {
        String fName = f.getName();
        logger.info(">> F: " + fName);

        V8Object fCallbacks = new V8Object(runtime);
//        final V8Value undef = runtime.getUndefined();

        JavaCallback getter = new JavaCallback() {
            public V8Object invoke(final V8Array parameters) {
                try {
                    Object v = f.get(instance);
                    return getReturnValue(runtime, v);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return new V8Object(runtime);
            }
        };
        fCallbacks.registerJavaMethod(getter, "get");

        JavaVoidCallback setter = new JavaVoidCallback() {
            public void invoke(final V8Array parameters) {
                Object v = (Object) parameters.get(0);
                if (v.getClass() == V8Object.class) {
                    return;
                }
                try {
                    f.set(instance, v);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        };
        fCallbacks.registerJavaMethod(setter, "set");

        parent.add(fName, fCallbacks);
        fCallbacks.release();
    }


    private static V8Object getV8ObjectForObject(V8 runtime, Object o) {
        int hash = o.hashCode();
        if (jsInstanceMap.containsKey(hash)) {
            return (V8Object) jsInstanceMap.get(hash);
        }

        V8Object res = generateAllGetSet(runtime, o.getClass(), o, false);
        res.add("__javaClass", o.getClass().getCanonicalName());
        res.add("__javaInstance", registerInstance(o));
        return res;
    }



    private static HashMap javaInstanceMap = new HashMap<Integer, Object>();
    private static HashMap jsInstanceMap = new HashMap<Integer, V8Object>();

    private static Object getInstance(int hash) {
        if (!javaInstanceMap.containsKey(hash)) {
            logger.warning("Hash missing: " + hash);
            return null;
        }
        return javaInstanceMap.get(hash);
    }

    private static int registerInstance(Object o) {
        int hash = o.hashCode();
        if (javaInstanceMap.containsKey(hash)) {
            logger.warning("Hash collision: " + hash);
        }
        javaInstanceMap.put(hash, o);
        return hash;
    }

}
