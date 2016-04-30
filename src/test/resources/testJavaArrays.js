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

var myBear = new Bear('pooh');
var myBear2 = new Bear('alfred');


//var a = StaticAnimals.SomeFuncArray([123, 456]);
//print('a= ', a);
//print('a= ', Object.keys(a));
//a.forEach(function(v) {
//    print('TEST>> ', v);
//});

