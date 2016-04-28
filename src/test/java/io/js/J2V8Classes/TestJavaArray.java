package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by emir on 4/28/16.
 */
public class TestJavaArray {
	@Test
	public void testJavaArrays() {
		V8 runtime = Runtime.getRuntime();
		runtime.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testStatics.js"));

		Assert.assertEquals(Arrays.toString(new String[]{"val1", "val2", "val3"}), runtime.executeStringScript("StaticAnimals.SomeFuncWithStringArray(['val1', 'val2', 'val3']);"));

		ArrayList listExample = new ArrayList();
		listExample.add("val1");
		listExample.add("val2");
		listExample.add("val3");
		Assert.assertEquals(listExample.toString(), runtime.executeStringScript("StaticAnimals.SomeFuncWithArrayList(['val1', 'val2', 'val3']);"));

		Runtime.release(runtime);
	}
}
