package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;

/**
 * Created by Brown on 4/28/16.
 */
public class _TestMC {
//    @Test
    public void testMC() {
        V8 v8 = V8JavaClasses.injectClassHelper(V8.createV8Runtime(), "testMC");
        v8.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testMC.js"));

        V8JavaClasses.release("testMC");
    }
}
