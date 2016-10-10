var Animal = ClassHelpers.getClass('io.js.J2V8Classes.Animal');
var TestJavaSetting = ClassHelpers.getClass('io.js.J2V8Classes.TestJavaSetting');
var RunnableWithArg = ClassHelpers.getClass('io.js.J2V8Classes.RunnableWithArg');

TestJavaSetting.customString = "yes";
TestJavaSetting.customAnimal = new Animal('babel_fish');
TestJavaSetting.customRunnable = new (RunnableWithArg.$extend({
	__name__: 'CustomRunnable',
	run: function(animal) {
		'@Override';
		TestJavaSetting.customAnimal = new Animal(animal.getType());
	}
}));

TestJavaSetting.setV8Object({"name":"custom_object", "type":"not_babel_fish"});

//print("customRunnable : ", Object.keys(customRunnable), "SDFSFSDF : ", SDFSFSDF);

//print('myAnimal.type now?! ', myAnimal.type);
