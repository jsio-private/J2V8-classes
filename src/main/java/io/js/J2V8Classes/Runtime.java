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
//        logger.setLevel(Level.WARNING);

        runtime.executeVoidScript(
                Utils.getScriptSource(
                        Runtime.class.getClassLoader(),
                        "abitbol/dist/abitbol.js"
                )
        );

        runtime.executeVoidScript(
                Utils.getScriptSource(
                        Runtime.class.getClassLoader(),
                        "jsClassHelper.js"
                )
        );


        JavaVoidCallback print = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
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
            public Object invoke(final V8Object receiver, final V8Array parameters) {
                String className = (String) parameters.get(0);
                logger.info("Getting class: " + className);
                try {
                    return getClassInfo(runtime, className);
                } catch (ClassNotFoundException e) {
                    logger.warning("> Class not found");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                return null;
            }
        };
        runtime.registerJavaMethod(getClass, "JavaGetClass");


        JavaCallback createInstance = new JavaCallback() {
            public V8Object invoke(final V8Object receiver, final V8Array parameters) {
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


    private static V8Object getClassInfo(V8 runtime, String className) throws ClassNotFoundException, IllegalAccessException {
        logger.info("Getting class info: " + className);
        Class clz = Class.forName(className);

        V8Object res = new V8Object(runtime);
        V8Object statics = generateAllGetSet(runtime, clz, clz, true);
        res.add("statics", statics);
        statics.release();
        V8Object publics = generateAllGetSet(runtime, clz, clz, false);
        res.add("publics", publics);
        publics.release();
        res.add("__javaClass", clz.getCanonicalName());

        Class superClz = clz.getSuperclass();
        if (superClz != Object.class) {
            res.add("__javaSuperclass", clz.getSuperclass().getCanonicalName());
        }

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
        Object instance = c.newInstance(parameters);

        return getV8ObjectForObject(runtime, instance);
    }

    private static V8Object generateAllGetSet(V8 runtime, Class clz, Object instance, boolean statics) {
        V8Object res = new V8Object(runtime);

        logger.info("Generating getters and setters for: " + clz.getName() + "(" + instance.hashCode() + ", " + statics + ")");


        logger.info("> Getting fields");
        Field[] f = clz.getDeclaredFields();
        V8Object jsF = new V8Object(runtime);
        for (int i = 0; i < f.length; i++) {
            if (Modifier.isStatic(f[i].getModifiers()) == statics) {
                generateGetSet(runtime, jsF, f[i]);
            }
        }
        res.add("fields", jsF);
        jsF.release();

        logger.info("> Getting methods");
        Method[] m = clz.getDeclaredMethods();
        V8Object jsM = new V8Object(runtime);
        for (int i = 0; i < m.length; i++) {
            if (Modifier.isStatic(m[i].getModifiers()) == statics) {
                generateMethod(runtime, jsM, m[i]);
            }
        }
        res.add("methods", jsM);
        jsM.release();

        if (!statics) {
            Class superClz = clz.getSuperclass();
            if (superClz != Object.class) {
                logger.info("> Adding super object for: " + superClz.getName());
                V8Object superData = generateAllGetSet(runtime, superClz, instance, false);
                superData.add("__javaClass", superClz.getCanonicalName());
                res.add("superData", superData);
                superData.release();
            }
        }

        return res;
    }

    private static void generateMethod(V8 runtime, V8Object parent, Method m) {
        String mName = m.getName();
        logger.info(">> M: " + mName);
        JavaCallback staticMethod = new JavaCallback() {
            public V8Object invoke(final V8Object receiver, final V8Array parameters) {
                try {
                    Object fromRecv = getReceiverFromCallback(receiver);
                    if (fromRecv == null) {
                        logger.warning("Callback with no bound java receiver!");
                        return new V8Object(runtime);
                    }
                    Object v = m.invoke(fromRecv, Utils.v8arrayToObjectArray(parameters));
                    return getReturnValue(runtime, v);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return new V8Object(runtime);
            }
        };
        parent.registerJavaMethod(staticMethod, mName);
    }

    private static V8Object getReturnValue(V8 runtime, Object v) {
        V8Object res = new V8Object(runtime);
        if (v == null) {
            res.addNull("v");
            return res;
        }

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

    public static Object getReceiverFromCallback(V8Object receiver) throws ClassNotFoundException {
        if (!receiver.contains("__javaInstance")) {
            if (!receiver.contains("__javaClass")) {
                logger.warning("Callback with no bound java receiver!");
                return null;
            }
            return Class.forName(receiver.getString("__javaClass"));
        }
        return getInstance(receiver.getInteger("__javaInstance"));
    }

    private static V8Object getFromField(V8 runtime, V8Object receiver, Field f) throws IllegalAccessException, ClassNotFoundException {
        Object fromRecv = getReceiverFromCallback(receiver);
        if (fromRecv == null) {
            logger.warning("Could not find receiving Object for callback!");
            return new V8Object(runtime);
        }
        Object v = f.get(fromRecv);
        return getReturnValue(runtime, v);
    }

    private static void generateGetSet(V8 runtime, V8Object parent, Field f) {
        String fName = f.getName();
        logger.info(">> F: " + fName);

        V8Object fCallbacks = new V8Object(runtime);
//        final V8Value undef = runtime.getUndefined();

        JavaCallback getter = new JavaCallback() {
            public V8Object invoke(final V8Object receiver, final V8Array parameters) {
                try {
                    return getFromField(runtime, receiver, f);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (V8ResultUndefined e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
                return new V8Object(runtime);
            }
        };
        fCallbacks.registerJavaMethod(getter, "get");

        JavaVoidCallback setter = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                try {
                    Object fromRecv = getReceiverFromCallback(receiver);

                    if (fromRecv == null) {
                        logger.warning("Could not find receiving Object for callback!");
                        return;
                    }

                    Object v = (Object) parameters.get(0);
                    if (v.getClass() == V8Object.class) {
                        return;
                    }

                    f.set(fromRecv, v);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
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

        Class clz = o.getClass();
        V8Object res = new V8Object(runtime);
//        V8Object res = generateAllGetSet(runtime, clz, o, false);
        res.add("__javaInstance", hash);
        res.add("__javaClass", clz.getCanonicalName());

        registerInstance(o);
        jsInstanceMap.put(hash, res);

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
