var Animal = ClassHelpers.getClass('io.js.J2V8Classes.Animal');
var StaticAnimals = ClassHelpers.getClass('io.js.J2V8Classes.StaticAnimals');

var Bear = Animal.$extend({
    __init__: function(subtype) {
        this.$super('bear');
        this.subtype = subtype;
    },

    getSubtype: function() {
        return this.subtype;
    }
});

var myBear = new Bear('grizzly');

//print('myBear.__javaClass= ', myBear.__javaClass);
//print('myBear.__javaSuperclass= ', myBear.__javaSuperclass);

StaticAnimals.registerAnimal(myBear);
