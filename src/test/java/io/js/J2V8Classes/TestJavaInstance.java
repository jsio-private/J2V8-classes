package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Brown on 4/27/16.
 */
public class TestJavaInstance {
    @Test
    public void testJavaInstance() {
        V8 runtime = Runtime.getRuntime();
        runtime.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testCustomInstance.js"));

        // Check constructor + read
        Assert.assertEquals("fish", runtime.executeStringScript("originalType"));
        // Check read
        Assert.assertEquals("zebra", runtime.executeStringScript("myAnimal.type"));
        // Check function call
        Assert.assertEquals("zebra", runtime.executeStringScript("myAnimal.getType()"));

        runtime.release();
    }
}
