var Animal = ClassHelpers.getClass('io.js.J2V8Classes.Animal');
var TestJavaSetting = ClassHelpers.getClass('io.js.J2V8Classes.TestJavaSetting');
var Runnable = ClassHelpers.getClass('java.lang.Runnable');

TestJavaSetting.customString = "yes";
TestJavaSetting.customAnimal = new Animal('babel_fish');
TestJavaSetting.customRunnable = new (Runnable.$extend({
	__name__: 'CustomRunnable',
	run: function() {
		'@Override';
		TestJavaSetting.customString = "runnable worked";
	}
}));

TestJavaSetting.setV8Object({"name":"custom_object", "type":"not_babel_fish"});

//print("customRunnable : ", Object.keys(customRunnable), "SDFSFSDF : ", SDFSFSDF);

//print('myAnimal.type now?! ', myAnimal.type);
