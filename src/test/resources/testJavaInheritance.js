var CustomJavaAnimal = ClassHelpers.getClass('io.js.J2V8Classes.CustomJavaAnimal');

var myAnimal = new CustomJavaAnimal('fish', 'fishy mcgee');
print('myAnimal= ', Object.keys(myAnimal));
print('myAnimal.type= ', typeof myAnimal.type, ' ', myAnimal.type);
//print('myAnimal.type ', Object.keys(myAnimal.type));
print('myAnimal.getType() ', myAnimal.getType());

print('myAnimal.name= ', typeof myAnimal.name, ' ', myAnimal.name);
