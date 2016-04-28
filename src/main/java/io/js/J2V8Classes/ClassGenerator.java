package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import javassist.*;

/**
 * Created by Brown on 4/27/16.
 */
public class ClassGenerator {

    public static Class createClass(V8 runtime, String canonicalName, String parentClz, V8Array methods) {
        ClassPool cp = ClassPool.getDefault();

        try {
              CtClass clz = cp.makeClass(canonicalName, cp.getCtClass(parentClz));

//              CtMethod m = CtNewMethod.make("public int xmove(int dx) { return dx + 1; }", clz);
//              clz.addMethod(m);

//            CtConstructor constructor = clz.makeClassInitializer();
//            clz.addConstructor(constructor);

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
