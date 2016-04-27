package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

public class TestMain {
//    @Test
    public void testPrintMessage() {
        V8 runtime = V8.createV8Runtime();
        int result = runtime.executeIntScript(""
                + "var hello = 'hello, ';\n"
                + "var world = 'world!';\n"
                + "hello.concat(world).length;\n");
        runtime.release();
        Assert.assertEquals(result, 13);
    }

    @Test
    public void testCustomJs() {
        V8 runtime = Runtime.getRuntime();
        runtime.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testScript.js"));
        System.out.println("result: " + runtime.executeStringScript("person.name"));
        System.out.println("result: " + runtime.executeStringScript("jackie.name"));
        System.out.println("result: " + runtime.executeStringScript("jackie.isAwesome()"));
        runtime.release();
//        Assert.assertEquals(result, 13);
    }
}