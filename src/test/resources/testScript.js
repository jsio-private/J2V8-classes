'use strict';


var Person = JSClass({
    create: function(name) {
        this.name = name;
    }
});


var person = new Person('joe');


var Jackie = Person.extend({
    isAwesome: function() {
        return 'very';
    }
});

var jackie = new Jackie('jackie');

//print('hello world!');
//print('print object? ', jackie);




//var Animal = ClassHelpers.getClass('io.js.J2V8Classes.Animal');
//
//var myAnimal = new Animal('dog');
//print('myAnimal= ', Object.keys(myAnimal));
//print('myAnimal.type= ', typeof myAnimal.type);
//print('myAnimal.type= ', myAnimal.type);
//
//myAnimal.type = 'a new type!';
//
//print('myAnimal.getType()= ', myAnimal.getType());




var StaticAnimals = ClassHelpers.getClass('io.js.J2V8Classes.StaticAnimals');
print('StaticAnimals= ', StaticAnimals.SomeNumber);
//print('StaticAnimals= ', StaticAnimals.statF());
