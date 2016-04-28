package io.js.J2V8Classes;

import com.eclipsesource.v8.*;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
                for (int i = 0, j = parameters.length(); i < j; i++) {
                    Object obj = parameters.get(i);
                    sb.append(obj);
//                    if (i < j - 1) {
//                        sb.append(' ');
//                    }
                    if (obj instanceof V8Value) {
                        ((V8Value) obj).release();
                    }
                }
                System.out.println(sb.toString());
            }
        };
        runtime.registerJavaMethod(print, "print");


        JavaVoidCallback getClass = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                String className = parameters.getString(0);
                logger.info("Getting class: " + className);
                try {
                    getClassInfo(className, parameters.getObject(1));
                } catch (ClassNotFoundException e) {
                    logger.warning("> Class not found");
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        };
        runtime.registerJavaMethod(getClass, "JavaGetClass");


        JavaCallback createInstance = new JavaCallback() {
            public V8Object invoke(final V8Object receiver, final V8Array parameters) {
                String className = parameters.getString(0);
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


        JavaCallback generateClass = new JavaCallback() {
            public V8Object invoke(final V8Object receiver, final V8Array parameters) {
                String className = parameters.getString(0);
                String superName = parameters.getString(1);
                V8Array methods = parameters.getArray(2);
                logger.info("Generating class: " + className + " extending " + superName + " (method count " + methods.length() + ")");

                ClassGenerator.createClass(runtime, className, superName, methods);

                methods.release();

//                try {
//                    logger.info("GENERATE CLASS PARAMS " + parameters.length());
//                    return createInstance(runtime, className, Utils.v8arrayToObjectArray(methods));
                    return new V8Object(runtime);
//                }
//                catch (ClassNotFoundException e) {
//                    logger.warning("> Class not found");
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                } catch (InstantiationException e) {
//                    e.printStackTrace();
//                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
//                } catch (NoSuchMethodException e) {
//                    e.printStackTrace();
//                }
//                return null;
            }
        };
        runtime.registerJavaMethod(generateClass, "JavaGenerateClass");



        return runtime;
    }


    private static void getClassInfo(String className, V8Object classInfo) throws ClassNotFoundException, IllegalAccessException {
        logger.info("Getting class info: " + className);
        Class clz = Class.forName(className);

        generateAllGetSet(classInfo.getObject("statics"), clz, clz, true);
        generateAllGetSet(classInfo.getObject("publics"), clz, clz, false);
        String clzName = Utils.getClassName(clz);
        classInfo.add("__javaClass", clzName);

        Class superClz = clz.getSuperclass();
        if (superClz != Object.class) {
            classInfo.add("__javaSuperclass", Utils.getClassName(clz.getSuperclass()));
        }
    }


    private static V8Object createInstance(V8 runtime, String className, Object[] parameters) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        logger.info("Getting class instance: " + className);
        Class clz = Class.forName(className);
        Class[] parameterTypes = Utils.getArrayClasses(parameters);

        // TODO: support for nested classes? http://stackoverflow.com/a/17485341
        logger.info("> Getting constructor for: " + Arrays.toString(parameterTypes));
        Constructor c = clz.getConstructor(parameterTypes);

        Object instance = c.newInstance(parameters);

        return Utils.getV8ObjectForObject(runtime, instance);
    }

    private static void generateAllGetSet(V8Object parent, Class clz, Object instance, boolean statics) {
        V8 runtime = parent.getRuntime();

        logger.info("Generating getters and setters for: " + clz.getName() + "(" + instance.hashCode() + ", " + statics + ")");

        logger.info("> Getting fields");
        Field[] f = clz.getDeclaredFields();
        V8Object jsF = parent.getObject("fields");
        for (int i = 0; i < f.length; i++) {
            if (Modifier.isStatic(f[i].getModifiers()) == statics) {
                generateGetSet(jsF, f[i]);
            }
        }

        logger.info("> Getting methods");
        Method[] m = clz.getDeclaredMethods();
        V8Object jsM = parent.getObject("methods");
        for (int i = 0; i < m.length; i++) {
            if (Modifier.isStatic(m[i].getModifiers()) == statics) {
                generateMethod(jsM, m[i]);
            }
        }

        if (!statics) {
            Class superClz = clz.getSuperclass();
            if (superClz != Object.class) {
                logger.info("> Adding super object for: " + superClz.getName());
                V8Object superData = runtime.executeObjectScript("ClassHelpers.getBlankClassInfo()");
                superData.add("__javaClass", superClz.getCanonicalName());
                generateAllGetSet(superData.getObject("publics"), superClz, instance, false);
                parent.add("superData", superData);
            }
        }
    }

    private static void generateMethod(V8Object parent, Method m) {
        V8 runtime = parent.getRuntime();

        String mName = m.getName();
        logger.info(">> M: " + mName);

        int mods = m.getModifiers();
        if (Modifier.isPrivate(mods)) {
            logger.info(">>> Skipping private");
            return;
        }

        JavaCallback staticMethod = new JavaCallback() {
            public V8Object invoke(final V8Object receiver, final V8Array parameters) {
                try {
                    Object fromRecv = getReceiverFromCallback(receiver);
                    if (fromRecv == null) {
                        logger.warning("Callback with no bound java receiver!");
                        return new V8Object(runtime);
                    }
                    Object[] args = Utils.v8arrayToObjectArray(parameters);
                    logger.info("Method: " + m.getName());
//                    logger.info("Args: " + Arrays.toString(args));

                    Class[] argTypes = Utils.getArrayClasses(args);
                    logger.info("Arg types: " + Arrays.toString(argTypes));

                    Method inferredMethod = null;
//                    Method inferredMethod = fromRecv.getClass().getDeclaredMethod(mName, argTypes);
                    Class fromRecvClz = fromRecv instanceof Class ? (Class) fromRecv : fromRecv.getClass();
                    Method[] ms = fromRecvClz.getMethods();
                    logger.info("Finding method... " + Utils.getClassName(fromRecvClz) + " " + mName + " (total " + ms.length + ")");
                    for (int i = 0; i < ms.length; i++) {
                        if (ms[i].getName() != mName) {
                            continue;
                        }

                        Class[] paramTypes = ms[i].getParameterTypes();
                        logger.info("Testing against paramTypes: " + Arrays.toString(paramTypes));
                        if (paramTypes.length != argTypes.length) {
                            continue;
                        }

                        boolean match = true;
                        for (int j = 0; j < paramTypes.length; j++) {
                            if (paramTypes[j].isArray() && argTypes[j].isArray()) {
                                Object[] arr = (Object[]) args[j];
                                args[j] = Arrays.copyOf(arr, arr.length, paramTypes[j]); // cast the Object[] array to whatever the Java method wants
                                match = true;
                                break;
                            } else if (paramTypes[j].isAssignableFrom(ArrayList.class) && argTypes[j].isArray()) {
                                ArrayList list = new ArrayList();
                                Object[] arr = (Object[]) args[j];
                                for(int k = 0; k < arr.length; k++){
                                    list.add(arr[k]);
                                }
                                args[j] = list;
                                match = true;
                                break;
                            } else if (!paramTypes[j].isAssignableFrom(argTypes[j])) {
                                match = false;
                                break;
                            }
                        }
                        if (match) {
                            inferredMethod = ms[i];
                            break;
                        }
                    }

                    if (inferredMethod == null) {
                        logger.warning("Could not infer method, argument class signature not found");
                        return new V8Object(runtime);
                    }

//                    Object v = m.invoke(fromRecv, args);
                    Object v = inferredMethod.invoke(fromRecv, args);
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
        } else if (v instanceof Object[]) {
            logger.info("> Class Array! " + vClass);
            Object[] oarr = (Object[]) v;
            V8Array arr = new V8Array(runtime);
            for (int i = 0; i < oarr.length; i++) {
                arr.push(getReturnValue(runtime, oarr[i]));
            }
            res.add("v", arr);
            arr.release();
        } else if (v instanceof Object) {
            logger.info("> Class! " + vClass);
            V8Object jsInst = Utils.getV8ObjectForObject(runtime, v);
            res.add("v", jsInst);
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
        return Utils.getInstance(receiver.getInteger("__javaInstance"));
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

    private static void generateGetSet(V8Object parent, Field f) {
        V8 runtime = parent.getRuntime();

        String fName = f.getName();
        logger.info(">> F: " + fName);

        int mods = f.getModifiers();
        if (Modifier.isPrivate(mods)) {
            logger.info(">>> Skipping private");
            return;
        }

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
        parent.registerJavaMethod(getter, "__get_" + fName);

        JavaVoidCallback setter = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                try {
                    Object fromRecv = getReceiverFromCallback(receiver);

                    if (fromRecv == null) {
                        logger.warning("Could not find receiving Object for callback!");
                        return;
                    }

                    Object v = parameters.get(0);
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
        parent.registerJavaMethod(setter, "__set_" + fName);
    }

    public static void release(V8 runtime) {
        Utils.releaseAllFor(runtime);
        // TODO: better release logic... maybe add some cleanup stuff to jsClassHelper
        runtime.release(false);
    }

}
