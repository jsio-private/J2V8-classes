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
        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testJsExtendJava.js"));

        // Check original class var
        Assert.assertEquals("bear", v8.executeStringScript("myBear.type"));

        // Check extended class method
        Assert.assertEquals("grizzly", v8.executeStringScript("myBear.getSubtype()"));

        // Check extended x2 class method
        Assert.assertEquals(true, v8.executeBooleanScript("myBear.bear2Func()"));

        // Check interactions with java
        Animal bear = StaticAnimals.animals.get(0);
        Assert.assertNotEquals(null, bear);
        Assert.assertEquals(true, bear instanceof Animal);
        Assert.assertEquals("bear", bear.getType());

        runtime.release();
    }
}
