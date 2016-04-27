var Person = JSClass({
    create: function(name) {
        this.name = name;
    }
});


var person = new Person('joe');


var Jackie = Person.extend({
    isAwesome: function() {
        return true;
    }
});

var jackie = new Jackie('jackie');
