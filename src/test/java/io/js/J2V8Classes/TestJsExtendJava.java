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
        V8 runtime = Runtime.getRuntime();
        runtime.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testJsExtendJava.js"));

        Animal bear = StaticAnimals.animals.get(0);
        Assert.assertNotEquals(null, bear);
        Assert.assertEquals(true, bear instanceof Animal);

        Runtime.release(runtime);
    }
}
