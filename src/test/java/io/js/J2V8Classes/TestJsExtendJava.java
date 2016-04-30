package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Brown on 4/27/16.
 */
public class TestJsExtendJava {
    @Test
    public void testJsExtendJava() {
        Runtime runtime = new Runtime("testJsExtendJava");
        V8 v8 = runtime.getRuntime();

//        Object res = v8.executeFunction("executeInstanceMethod", new V8Array(v8));
//        System.out.println("RES: " + res);

        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testJsExtendJava.js"));

        // Check original class var
        Assert.assertEquals("bear", v8.executeStringScript("myBear.type"));

        // Check extended class method
        Assert.assertEquals("grizzly", v8.executeStringScript("myBear.getSubtype()"));

        // Check extended x2 class method
        Assert.assertEquals(true, v8.executeBooleanScript("myBear.bear2Func()"));

        // Check interactions with java
        Animal bear = StaticAnimals.findAnimal("bear");
        Assert.assertNotNull(bear);
        Assert.assertEquals(true, bear instanceof Animal);
        Assert.assertEquals("bear", bear.getType());

        runtime.release();
    }

    @Test
    public void testJsExtendJava_methodOverride() {
        Runtime runtime = new Runtime("testJsExtendJava_methodOverride");
        V8 v8 = runtime.getRuntime();
        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testJsExtendJava.js"));

        // Check js/js override
        Assert.assertEquals("asdf", v8.executeStringScript("myOtherBear.getSubtype()"));

        // Check js/java override
        Assert.assertEquals("qwer", v8.executeStringScript("myOtherBear.getType()"));

        // Check java override

        Animal myOtherBear = StaticAnimals.findAnimal("qwer");
        Assert.assertNotNull(myOtherBear);

        runtime.release();
    }
}
