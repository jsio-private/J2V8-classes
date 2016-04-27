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
        V8 runtime = Runtime.getRuntime();
        runtime.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testJavaInheritance.js"));

        // Check original functionality
        Assert.assertEquals("fish", runtime.executeStringScript("myAnimal.type"));
        Assert.assertEquals("fish", runtime.executeStringScript("myAnimal.getType()"));

        // Check extended functionality
        Assert.assertEquals("fishy mcgee", runtime.executeStringScript("myAnimal.name"));
        Assert.assertEquals("fishy mcgee", runtime.executeStringScript("myAnimal.getName()"));

        runtime.release();

    }
}
