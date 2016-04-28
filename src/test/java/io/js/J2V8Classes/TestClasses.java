package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Brown on 4/27/16.
 */
public class TestClasses {
    @Test
    public void testClasses() {
        V8 runtime = Runtime.getRuntime();
        runtime.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testClasses.js"));

        Assert.assertEquals(runtime.executeStringScript("person.name"), "joe");
        Assert.assertEquals(runtime.executeStringScript("jackie.name"), "jackie");
        Assert.assertEquals(runtime.executeBooleanScript("jackie.isAwesome()"), true);

        Runtime.release(runtime);
    }
}
