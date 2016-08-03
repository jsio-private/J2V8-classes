package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Emir
 */
public class TestJavaSetting {

    public static String customString = "no";
    public static Animal customAnimal = null;
    public static Runnable customRunnable = null;

    @Test
    public void testJavaInstance() {
        Runtime runtime = new Runtime("testJavaSetting");
        V8 v8 = runtime.getRuntime();
        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testJsSetJava.js"));

        Assert.assertEquals("yes", customString);
        Assert.assertEquals("babel_fish", customAnimal.getType());

        Assert.assertNotNull(customRunnable);
        customRunnable.run();

        Assert.assertEquals("runnable worked", customString);

        runtime.release();
    }
}
