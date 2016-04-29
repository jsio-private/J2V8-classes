package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by emir on 4/29/16.
 */
public class TestJavaObfuscationMappings {
	@Test
	public void testJavaObfuscationMappings() {
		V8 runtime = Runtime.getRuntime();

		Runtime.obfuscatedFieldMap.put("field_very_obfuscated_2394820948", "bald_eagle"); // remap some obfuscated field to something better
		Runtime.obfuscatedMethodMap.put("getType", "giveMeTheType"); // same as above but methods

		runtime.executeVoidScript(Utils.getScriptSource(this.getClass().getClassLoader(), "testStatics.js"));

		Assert.assertEquals("bald_eagle", runtime.executeStringScript("StaticAnimals.bald_eagle.giveMeTheType()"));

		Runtime.release(runtime);
	}
}
