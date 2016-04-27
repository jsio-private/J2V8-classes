package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Brown on 4/27/16.
 */
public class TestStatics {
    @Test
    public void testJavaStatics() {
        V8 runtime = Runtime.getRuntime();
        runtime.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testStatics.js"));

        // Check field
        Assert.assertEquals(123, runtime.executeIntegerScript("StaticAnimals.SomeNumber"));
        // Check method
        Assert.assertEquals("asdf!", runtime.executeStringScript("StaticAnimals.SomeFunc('asdf')"));
        // Check instances
        Assert.assertEquals("cat", runtime.executeStringScript("StaticAnimals.cat.getType()"));
        Assert.assertEquals("dog", runtime.executeStringScript("StaticAnimals.dog.getType()"));

        runtime.release();
    }
}
