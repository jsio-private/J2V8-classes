 (function (root, factory) {
    if (typeof define === 'function' && define.amd) {
        // AMD. Register as an anonymous module.
        define([], factory);
    } else if (typeof module === 'object' && module.exports) {
        // Node. Does not work with strict CommonJS, but
        // only CommonJS-like environments that support module.exports,
        // like Node.
        module.exports = factory();
    } else {
        // Browser globals (root is window)
        root.ClassHelpers = factory();
   }
}(this, function () {

  var classMap = {};

  var addMethod = function(inst, name, method) {
    inst[name] = method;
  };

  var addField = function(inst, name, get, set) {
    Object.defineProperty(inst, name, {
      enumerable: true,
      get: function() {
        var v = get();
        return v.v;
      },
      // get: get,
      set: set
    });
  };


  var addProxies = function(instance, javaData) {
    if (!javaData) {
      return instance;
    }

    // Add all the js -> java methods
    var methods = javaData.methods;
    for (var k in methods) {
      print('adding method: ', k);
      addMethod(instance, k, methods[k]);
    }

    // Add getters and setters for the fields
    var fields = javaData.fields;
    for (var k in fields) {
      print('adding field: ', k);
      addField(instance, k, fields[k].get, fields[k].set);
    }

    return instance;
  };


  var getClass = function(className) {
    var existing = classMap[className];
    if (existing) {
      return existing;
    }
    var classInfo = JavaGetClass(className);

    if (!classInfo.found) {
      return null;
    }

    var classConstructor = {
      create: function() {
        print('creating instance: ', className);
        var instData = JavaCreateInstance(className);

        this.__javaInstance = instData.__javaInstance;
        print('(inst hash: ' + this.__javaInstance + ')');

        addProxies(this, instData);
      }
    };

    var clz = JSClass(classConstructor);
    clz.static(addProxies({}, classInfo.statics));

    classMap[className] = clz;
    return clz;
  };

  // var JavaClass = JSClass({
  //   create: function() {
  //     var inst = JavaCreateInstance();
  //   }
  // });

  return {
    getClass: getClass,
    // JavaClass: JavaClass
  };

}));
