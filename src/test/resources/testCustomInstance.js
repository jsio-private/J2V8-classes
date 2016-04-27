var Animal = ClassHelpers.getClass('io.js.J2V8Classes.Animal');

var myAnimal = new Animal('fish');
print('myAnimal= ', Object.keys(myAnimal));
print('myAnimal.type= ', typeof myAnimal.type);
print('myAnimal.type= ', myAnimal.type);

var originalType = myAnimal.type;
myAnimal.type = 'zebra';
