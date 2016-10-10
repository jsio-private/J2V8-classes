package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Emir
 */
public class TestJavaSetting {

    public static String customString = "no";
    public static Animal customAnimal = null;
    public static RunnableWithArg customRunnable = null;
    public static V8Object v8obj = null;

    @Test
    public void testJavaInstance() {
        V8 v8 = V8JavaClasses.injectClassHelper(V8.createV8Runtime(), "testJavaSetting");
        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testJsSetJava.js"));

        Assert.assertEquals("yes", customString);
        Assert.assertEquals("babel_fish", customAnimal.getType());
        Assert.assertEquals("not_babel_fish", v8obj.getString("type"));

        Assert.assertNotNull(customRunnable);
        customRunnable.run(new Animal("table"));

        Assert.assertEquals("table", customAnimal.getType());

        V8JavaClasses.release("testJavaSetting");
    }

    public static void setV8Object(V8Object obj) {
        TestJavaSetting.v8obj = obj;
    }
}
