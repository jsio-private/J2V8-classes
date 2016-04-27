package io.js.J2V8Classes;

import com.eclipsesource.v8.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.logging.Logger;

import static com.sun.xml.internal.fastinfoset.stax.events.EmptyIterator.instance;

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
//                try {
//                    Class clz = Class.forName(className);

//                    res.add("statics", getClassStatics(runtime, className));
                    res.add("found", true);


//                } catch (ClassNotFoundException e) {
//                    logger.warning("> Class not found");
//                    res.add("found", false);
//                }

                return res;
            }
        };
        runtime.registerJavaMethod(getClass, "JavaGetClass");


        JavaCallback getInstance = new JavaCallback() {
            public V8Object invoke(final V8Array parameters) {
                String className = (String) parameters.get(0);
                try {
                    return createInstance(runtime, className);
                } catch (ClassNotFoundException e) {
                    logger.warning("> Class not found");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        runtime.registerJavaMethod(getInstance, "JavaCreateInstance");

        return runtime;
    }


    private static V8Object getClassStatics(V8 runtime, String className) throws ClassNotFoundException {
        logger.info("Getting class statics: " + className);
        Class clz = Class.forName(className);

        V8Object res = new V8Object(runtime);

        logger.info("> Getting fields");
        Field[] f = clz.getDeclaredFields();
        V8Object jsF = new V8Object(runtime);
        for (int i = 0; i < f.length; i++) {
            generateGetSet(runtime, jsF, instance, f[i]);
        }
        res.add("fields", jsF);
        jsF.release();

        logger.info("> Getting methods");
        Method[] m = instance.getClass().getDeclaredMethods();
        V8Object jsM = new V8Object(runtime);
        for (int i = 0; i < m.length; i++) {
            String mName = m[i].getName();
            logger.info(">> M: " + mName);
            jsM.registerJavaMethod(instance, mName, mName, m[i].getParameterTypes());
        }
        res.add("methods", jsM);
        jsM.release();

//        res.add("__javaInstance", registerInstance(instance));

        return res;
    }


    private static V8Object createInstance(V8 runtime, String className) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException {
        logger.info("Getting class instance: " + className);
        Class clz = Class.forName(className);
        Constructor c = clz.getConstructors()[0];
        Object instance = c.newInstance();

        V8Object res = new V8Object(runtime);

        logger.info("> Getting fields");
        Field[] f = clz.getDeclaredFields();
        V8Object jsF = new V8Object(runtime);
        for (int i = 0; i < f.length; i++) {
            generateGetSet(runtime, jsF, instance, f[i]);
        }
        res.add("fields", jsF);
        jsF.release();

        logger.info("> Getting methods");
        Method[] m = clz.getDeclaredMethods();
        V8Object jsM = new V8Object(runtime);
        for (int i = 0; i < m.length; i++) {
            String mName = m[i].getName();
            logger.info(">> M: " + mName);
            jsM.registerJavaMethod(instance, mName, mName, m[i].getParameterTypes());
        }
        res.add("methods", jsM);
        jsM.release();

        res.add("__javaInstance", registerInstance(instance));

        return res;
    }

    private static void generateGetSet(V8 runtime, V8Object parent, Object instance, Field f) {
        String fName = f.getName();
        logger.info(">> F: " + fName);

        V8Object fCallbacks = new V8Object(runtime);
//        final V8Value undef = runtime.getUndefined();

        JavaCallback getter = new JavaCallback() {
            public V8Object invoke(final V8Array parameters) {
                V8Object res = new V8Object(runtime);
                try {
                    Object v = f.get(instance);
                    Class vClass = v.getClass();
                    if (vClass == boolean.class) {
                        res.add("v", (boolean) v);
                    } else if (vClass == double.class) {
                        res.add("v", (double) v);
                    } else if (vClass == int.class) {
                        res.add("v", (int) v);
                    } else if (vClass == String.class) {
                        res.add("v", (String) v);
                    } else {
                        logger.warning("> Unknown type! " + vClass);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                return res;
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

    private static HashMap instanceMap = new HashMap<Integer, Object>();

    private static Object getInstance(int hash) {
        if (!instanceMap.containsKey(hash)) {
            logger.warning("Hash missing: " + hash);
            return null;
        }
        return instanceMap.get(hash);
    }

    private static int registerInstance(Object o) {
        int hash = o.hashCode();
        if (instanceMap.containsKey(hash)) {
            logger.warning("Hash collision: " + hash);
        }
        instanceMap.put(hash, o);
        return hash;
    }

}
