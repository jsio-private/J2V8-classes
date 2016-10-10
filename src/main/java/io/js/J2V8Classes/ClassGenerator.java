package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import javassist.*;

import java.util.logging.Logger;

/**
 * Created by Brown on 4/27/16.
 */
public class ClassGenerator {
    private static Logger logger = Logger.getLogger("ClassGenerator");

    public static Class createClass(String runtimeName, String canonicalName, String superClzName, V8Array methods) {
        logger.info("Generating class: " + canonicalName + " extends " + superClzName);
        ClassPool cp = ClassPool.getDefault();

        try {
            CtClass superClz = cp.getCtClass(superClzName);
            CtClass clz = cp.makeClass(canonicalName, superClz);

            if (superClz.isInterface()) {
                clz = cp.makeClass(canonicalName);
                clz.addInterface(superClz);
            }

            // Add matching constructors if the super class is not dynamic
            CtConstructor[] c = superClz.getConstructors();
            for (int i = 0; i < c.length; i++) {
                CtConstructor proxyConstructor = CtNewConstructor.make(c[i].getParameterTypes(), c[i].getExceptionTypes(), clz);
                clz.addConstructor(proxyConstructor);
            }

            CtField runtimeNameField = new CtField(cp.get("java.lang.String"), "runtimeName", clz);
            runtimeNameField.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
            clz.addField(runtimeNameField, CtField.Initializer.constant(runtimeName));

            ClassPool.getDefault().insertClassPath(new ClassClassPath(V8.class));
            cp.importPackage("com.eclipsesource.v8");
            cp.importPackage("io.js.J2V8Classes");

            CtMethod runJsFunc = CtNewMethod.make(
                    "private Object runJsFunc(String name, Object[] args) { " +
                        "com.eclipsesource.v8.V8 v8 = io.js.J2V8Classes.V8JavaClasses.getRuntime(runtimeName);" +
                        "com.eclipsesource.v8.V8Array v8Args = new com.eclipsesource.v8.V8Array(v8);" +
                        "v8Args.push(hashCode());" +
                        "v8Args.push(name);" +
                        "v8Args.push(io.js.J2V8Classes.Utils.toV8Object(v8, args));" +
                        "Object res = v8.executeFunction(\"executeInstanceMethod\", v8Args);" +
                        "v8Args.release();" +
                        "return res;" +
                    "}",
                    clz
            );
            clz.addMethod(runJsFunc);

            String defaultReturn = "Object";
            String defaultArgs = "Object[] args";

            String methodNames = "";
            for (int i = 0, j = methods.length(); i < j; i++) {
                V8Object v8o = methods.getObject(i);
                String name = v8o.getString("name");
                logger.info("> Adding method: " + name);
                methodNames += "\"" + name + "\"";

                V8Object annotations = v8o.getObject("annotations");
                if (annotations.contains("Override") && annotations.getBoolean("Override")) {
                    logger.info(">> is override");
                    CtMethod superMethod = findSuperMethod(superClz, name);

//                    logger.info(">> >> " + Arrays.toString(superMethod.getParameterTypes()));
//                    logger.info(">> >> " + superMethod.getReturnType().getName());

                    String superReturnType = superMethod.getReturnType().getName();
                    CtClass[] superArgs = superMethod.getParameterTypes();
                    String argsString = "";
                    String jsFuncArgString = "";
                    for (int k = 0; k < superArgs.length; k++) {
                        argsString += superArgs[k].getName() + " a" + k;
                        jsFuncArgString += "a" + k;
                        if (k < superArgs.length - 1) {
                            argsString += ",";
                            jsFuncArgString += ",";
                        }
                    }

                    String meth = "public " + superReturnType + " " + name + "(" + argsString + ") { ";
                    if (jsFuncArgString.length() > 0) {
                        meth += "Object[] args = new Object[]{" + jsFuncArgString + "};";
                    } else {
                        meth += "Object[] args = null;";
                    }
                    if (superReturnType.equals("void")) {
                        meth += "this.runJsFunc(\"" + name + "\", args);" + "}";
                    } else {
                        meth += "return (" + superReturnType + ") this.runJsFunc(\"" + name + "\", args);" + "}";
                    }
                    CtMethod proxyMethod = CtNewMethod.make(
                            meth,
                            clz
                    );
                    if (!superClz.isInterface()) {
                        proxyMethod.setModifiers(superMethod.getModifiers());
                    }
                    clz.addMethod(proxyMethod);
                } else {
                    CtMethod proxyMethod = CtNewMethod.make(
                            "public " + defaultReturn + " " + name + "(" + defaultArgs + ") { " +
                                    "return runJsFunc(\"" + name + "\", args);" +
                                    "}",
                            clz
                    );
                    proxyMethod.setModifiers(proxyMethod.getModifiers() | Modifier.VARARGS);
                    clz.addMethod(proxyMethod);
                }
            }

            CtField jsMethods = new CtField(cp.get("[Ljava.lang.String;"), "__jsMethods", clz);
            jsMethods.setModifiers(Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL);
            if (methodNames.length() > 0) {
                clz.addField(jsMethods, CtField.Initializer.byExpr("new String[]{" + methodNames + "}"));
            } else {
                clz.addField(jsMethods, CtField.Initializer.byExpr("null"));
            }

            return clz.toClass();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
        catch (NotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    private static CtMethod findSuperMethod(CtClass clz, String name) {
        logger.info("Finding method: " + name + " on " + clz.getName());
        if (clz == null) {
            return null;
        }

        CtMethod[] methods = clz.getDeclaredMethods();
        for (int i = 0; i < methods.length; i++) {
            CtMethod m = methods[i];
            if (m.getName().equals(name)) {
                return m;
            }
        }

        try {
            return findSuperMethod(clz.getSuperclass(), name);
        } catch (NotFoundException e) {
            return null;
        }
    }
}
