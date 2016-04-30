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
        Runtime runtime = new Runtime("testJavaInstance");
        V8 v8 = runtime.getRuntime();
        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testCustomInstance.js"));

        // Check default constructor
        Assert.assertEquals("unknown", v8.executeStringScript("boringAnimal.type"));

        // Check constructor + read
        Assert.assertEquals("fish", v8.executeStringScript("originalType"));
        // Check read
        Assert.assertEquals("zebra", v8.executeStringScript("myAnimal.type"));
        // Check function call
        Assert.assertEquals("zebra", v8.executeStringScript("myAnimal.getType()"));

        runtime.release();
    }
}
