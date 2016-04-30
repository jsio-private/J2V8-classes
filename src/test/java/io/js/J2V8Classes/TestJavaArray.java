package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * Created by emir on 4/28/16.
 */
public class TestJavaArray {
	@Test
	public void testJavaArrays() {
		V8 runtime = Runtime.getRuntime();
		runtime.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testJavaArrays.js"));

		String[] res1 = (String[]) Utils.v8arrayToObjectArray(runtime.executeArrayScript("StaticAnimals.SomeFuncArray(['val1', 'val2'])"));
		String[] a1 = new String[]{"val1", "val2", "newVal"};
		Assert.assertEquals(Arrays.toString(a1), Arrays.toString(res1));

		String[] names2 = (String[]) Utils.v8arrayToObjectArray(runtime.executeArrayScript("StaticAnimals.SomeFuncVarargs([myBear, StaticAnimals.cat])"));
		String[] a2 = new String[]{"bear", "cat"};
		Assert.assertEquals(Arrays.toString(a2), Arrays.toString(names2));

		String[] names3 = (String[]) Utils.v8arrayToObjectArray(runtime.executeArrayScript("StaticAnimals.SomeFuncVarargs([myBear, myBear2])"));
		String[] a3 = new String[]{"bear", "bear"};
		Assert.assertEquals(Arrays.toString(a3), Arrays.toString(names3));

		Object[] res2 = (Object[]) Utils.v8arrayToObjectArray(runtime.executeArrayScript("StaticAnimals.SomeFuncArray([123, 456])"));
		int[] a4 = new int[]{123, 456, 9};
		Assert.assertEquals(Arrays.toString(a4), Arrays.toString(res2));

//		ArrayList listExample = new ArrayList();
//		listExample.add("val1");
//		listExample.add("val2");
//		listExample.add("val3");
//		Assert.assertEquals(listExample.toString(), runtime.executeStringScript("StaticAnimals.SomeFuncWithArrayList(['val1', 'val2', 'val3']);"));

		Runtime.release(runtime);
	}
}
