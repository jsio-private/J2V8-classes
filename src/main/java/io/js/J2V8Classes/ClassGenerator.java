package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import javassist.*;

/**
 * Created by Brown on 4/27/16.
 */
public class ClassGenerator {

    public static Class createClass(V8 runtime, String canonicalName, String superClzCanonicalName, V8Array methods) {
        ClassPool cp = ClassPool.getDefault();

        try {
            CtClass superClz = cp.getCtClass(superClzCanonicalName);
            CtClass clz = cp.makeClass(canonicalName, superClz);

            // Add matching constructors if the super class is not dynamic
            CtConstructor[] c = superClz.getConstructors();
            for (int i = 0; i < c.length; i++) {
                CtConstructor proxyConstructor = CtNewConstructor.make(c[i].getParameterTypes(), c[i].getExceptionTypes(), clz);
                clz.addConstructor(proxyConstructor);
            }


//              CtMethod m = CtNewMethod.make("public int xmove(int dx) { return dx + 1; }", clz);
//              clz.addMethod(m);

//            for (int i = 0, j = methods.length(); i < j; i++) {
//                String name = methods.getObject(i).getString("name");
//                CtMethod m = CtNewMethod.make("public Object " + name + "(int dx) { return dx + 1; }", clz);
//                clz.addMethod(m);
//            }


            return clz.toClass();
        } catch (CannotCompileException e) {
            e.printStackTrace();
        }
        catch (NotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
