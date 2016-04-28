var Animal = ClassHelpers.getClass('io.js.J2V8Classes.Animal');
var StaticAnimals = ClassHelpers.getClass('io.js.J2V8Classes.StaticAnimals');

var Bear = Animal.$extend({
    __name__: 'Bear',
    __init__: function(subtype) {
        this.$super('bear');
        this.subtype = subtype;
    },

    getSubtype: function() {
        return this.subtype;
    }
});

var Bear2 = Bear.$extend({
    __name__: 'Bear2',
    __init__: function(subtype) {
        this.$super(subtype);
    },

    bear2Func: function() {
        return true;
    }
});

var myBear = new Bear2('grizzly');

//print('myBear.$class.$name= ', myBear.$class.$name);
//print('myBear.$class.__javaClass= ', myBear.$class.__javaClass);
//print('myBear.$class.__javaSuperclass= ', myBear.$class.__javaSuperclass);
//
//print('myBear ', Object.keys(myBear));
//
//print('myBear.type= ', myBear.type);
//print('myBear.getSubtype()= ', myBear.getSubtype());
//print('myBear.bear2func()= ', myBear.bear2Func());

StaticAnimals.registerAnimal(myBear);
