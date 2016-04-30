package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Brown on 4/27/16.
 */
public class TestJavaInheritance {
    @Test
    public void testJavaInheritance() {
        Runtime runtime = new Runtime("testJavaInheritance");
        V8 v8 = runtime.getRuntime();
        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testJavaInheritance.js"));

        // Check original functionality
        Assert.assertEquals("fish", v8.executeStringScript("myAnimal.type"));
        Assert.assertEquals("fish", v8.executeStringScript("myAnimal.getType()"));

        // Check extended functionality
        Assert.assertEquals("fishy mcgee", v8.executeStringScript("myAnimal.name"));
        Assert.assertEquals("fishy mcgee", v8.executeStringScript("myAnimal.getName()"));

        runtime.release();
    }
}
