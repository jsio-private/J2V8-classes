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
        Runtime runtime = new Runtime("testJavaStatics");
        V8 v8 = runtime.getRuntime();
        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testStatics.js"));

        // Check field
        Assert.assertEquals(123, v8.executeIntegerScript("StaticAnimals.SomeNumber"));
        // Check method
        Assert.assertEquals("asdf!", v8.executeStringScript("StaticAnimals.SomeFunc('asdf')"));
        // Check instances
        Assert.assertEquals("cat", v8.executeStringScript("StaticAnimals.cat.getType()"));
        Assert.assertEquals("dog", v8.executeStringScript("StaticAnimals.dog.type"));

        // Test setting a static value
        int n = 789;
        v8.executeVoidScript("StaticAnimals.SomeNumber = " + n + ";");
        Assert.assertEquals(n, v8.executeIntegerScript("StaticAnimals.SomeNumber"));
        Assert.assertEquals(n, StaticAnimals.SomeNumber);

        runtime.release();
    }
}
