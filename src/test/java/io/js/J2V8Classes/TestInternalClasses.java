package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by Brown on 4/28/16.
 */
public class TestInternalClasses {
    @Test
    public void testClasses() {
        Runtime runtime = new Runtime("testClasses");
        V8 v8 = runtime.getRuntime();
        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testInternalClasses.js"));

        Assert.assertEquals(
                NestedClasses.c1Inst.getClass().getName(),
                v8.executeStringScript("c1Inst.$class.__javaClass")
        );

        runtime.release();
    }
}
