package io.js.J2V8Classes;

import com.eclipsesource.v8.*;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Created by Brown on 4/26/16.
 */
public class Runtime {

    private static HashMap<String, Runtime> runtimes = new HashMap<>();

    private Logger logger;
    private String name;

    private V8 runtime;

    public Runtime(String name) {
        this.name = name;
        logger = Logger.getLogger("Runtime-" + name);
    }

    public String getName() {
        return name;
    }

    public V8 getRuntime() {
        if (runtime != null) {
            return runtime;
        }

        runtime = V8.createV8Runtime();
        runtimes.put(name, this);

        runtime.executeVoidScript("__runtimeName='" + name + "';");

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

        JavaVoidCallback log = new JavaVoidCallback() {
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
                logger.info("JS: " + sb.toString());
            }
        };
        runtime.registerJavaMethod(log, "log");


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

        final Runtime thiz = this;
        JavaCallback generateClass = new JavaCallback() {
            public V8Object invoke(final V8Object receiver, final V8Array parameters) {
                String className = parameters.getString(0);
                String superName = parameters.getString(1);
                V8Array methods = parameters.getArray(2);
                logger.info("Generating class: " + className + " extending " + superName + " (method count " + methods.length() + ")");

                ClassGenerator.createClass(thiz, className, superName, methods);

                methods.release();
                return new V8Object(runtime);
            }
        };
        runtime.registerJavaMethod(generateClass, "JavaGenerateClass");



        return runtime;
    }


    private void getClassInfo(String className, V8Object classInfo) throws ClassNotFoundException, IllegalAccessException {
        logger.info("Getting class info: " + className);
        Class clz = Class.forName(className);

        generateAllGetSet(classInfo.getObject("statics"), clz, clz, true);
        generateAllGetSet(classInfo.getObject("publics"), clz, clz, false);
        String clzName = Utils.getClassName(clz);
        classInfo.add("__javaClass", clzName);

        Class superClz = clz.getSuperclass();
        if (superClz != Object.class && superClz != null) {
            classInfo.add("__javaSuperclass", Utils.getClassName(clz.getSuperclass()));
        }
    }


    private V8Object createInstance(V8 runtime, String className, Object[] parameters) throws ClassNotFoundException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        logger.info("Getting class instance: " + className);
        Class clz = Class.forName(className);

        // TODO: support for nested classes? http://stackoverflow.com/a/17485341
        logger.info("> Getting constructor");
        Executable inferredMethod = Utils.findMatchingExecutable(
                clz.getConstructors(),
                parameters,
                null
        );

        if (inferredMethod == null) {
            logger.warning("> Could not find constructor for args " + Arrays.toString(parameters));
            return null;
        }

        Object instance = ((Constructor) inferredMethod).newInstance(parameters);
        return Utils.getV8ObjectForObject(runtime, instance);
    }

    private void generateAllGetSet(V8Object parent, Class clz, Object instance, boolean statics) {
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
        // Dont send in js methods??
        String[] jsMethods;
        try {
            Field __jsMethods = clz.getField("__jsMethods");
            jsMethods = (String[]) __jsMethods.get(clz);
        } catch(NoSuchFieldException e) {
            jsMethods = new String[]{};
        } catch(IllegalAccessException e) {
            jsMethods = new String[]{};
        }
        logger.info(">> jsMethods= " + Arrays.toString(jsMethods));

        Method[] methods = clz.getDeclaredMethods();
        V8Object jsM = parent.getObject("methods");
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (Modifier.isStatic(m.getModifiers()) == statics) {
                generateMethod(jsM, m);
            }
        }

        if (!statics) {
            Class superClz = clz.getSuperclass();
            if (superClz != Object.class && superClz != null) {
                logger.info("> Adding super object for: " + superClz.getName());
                V8Object superData = runtime.executeObjectScript("ClassHelpers.getBlankClassInfo()");
                superData.add("__javaClass", superClz.getCanonicalName());
                generateAllGetSet(superData.getObject("publics"), superClz, instance, false);
                parent.add("superData", superData);
            }
        }
    }

    private void generateMethod(V8Object parent, Method m) {
        V8 runtime = parent.getRuntime();

        String mName = m.getName();
        logger.info(">> M: " + mName);

        int mods = m.getModifiers();
        if (Modifier.isPrivate(mods)) {
            logger.info(">>> Skipping private");
            return;
        }
        if (Modifier.isProtected(mods)) {
            logger.info(">>> Skipping protected");
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
                    logger.info("Method: " + mName);
                    logger.info("Args: " + Arrays.toString(args));

                    Class fromRecvClz = fromRecv instanceof Class ? (Class) fromRecv : fromRecv.getClass();

                    logger.info("fromRecvClz: " + Utils.getClassName(fromRecvClz));

                    Executable inferredMethod = Utils.findMatchingExecutable(
                            fromRecvClz.getMethods(),
                            args,
                            mName
                    );

                    if (inferredMethod == null) {
                        return new V8Object(runtime);
                    }

                    inferredMethod.setAccessible(true);
                    Object v = ((Method) inferredMethod).invoke(fromRecv, Utils.matchExecutableParams(inferredMethod, args));
                    return Utils.toV8Object(runtime, v);
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

    public Object getReceiverFromCallback(V8Object receiver) throws ClassNotFoundException {
        if (!receiver.contains("__javaInstance")) {
            if (!receiver.contains("__javaClass")) {
                logger.warning("Callback with no bound java receiver!");
                return null;
            }
            return Class.forName(receiver.getString("__javaClass"));
        }
        return Utils.getInstance(receiver.getInteger("__javaInstance"));
    }

    private V8Object getFromField(V8 runtime, V8Object receiver, Field f) throws IllegalAccessException, ClassNotFoundException {
        Object fromRecv = getReceiverFromCallback(receiver);
        if (fromRecv == null) {
            logger.warning("Could not find receiving Object for callback!");
            return new V8Object(runtime);
        }
        f.setAccessible(true);
        Object v = f.get(fromRecv);
        return Utils.toV8Object(runtime, v);
    }

    private void generateGetSet(V8Object parent, Field f) {
        V8 runtime = parent.getRuntime();

        String fName = f.getName();
        logger.info(">> F: " + fName);

        int mods = f.getModifiers();
        if (Modifier.isPrivate(mods)) {
            logger.info(">>> Skipping private");
            return;
        }
        if (Modifier.isProtected(mods)) {
            logger.info(">>> Skipping protected");
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
                        V8Object jsObj = (V8Object) v;
                        Object javaObj = getReceiverFromCallback(jsObj);
                        if(javaObj == null){
                            return;
                        }
                        v = javaObj;
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

    public void release() {
        runtimes.remove(name);
        Utils.releaseAllFor(runtime);
        // TODO: better release logic... maybe add some cleanup stuff to jsClassHelper
        runtime.release(false);
    }


    public static Runtime getRuntime(String name) {
        if (runtimes.containsKey(name)) {
            return runtimes.get(name);
        }
        return null;
    }
}
