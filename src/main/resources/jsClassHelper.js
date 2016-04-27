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
  var instanceMap = {};

  var handleIncommingGet = function(res) {
    // print('handleIncommingGet', Object.keys(res));
    if (!res) {
      return undefined;
    }
    res = res.v || res;

    if (res.__javaInstance !== undefined) {
      print('handleIncommingGet: class instance');
      var existingInstance = instanceMap[res.__javaInstance];
      if (existingInstance) {
        print('handleIncommingGet: already exists');
        return existingInstance;
      }
      // make the instance
      var clz = getClass(res.__javaClass);
      var inst = new clz('EXISTING_INSTANCE', res);
      return inst;
    }

    return res;
  };


  var addMethod = function(inst, name, method) {
    print('adding method: ', name);
    inst[name] = function() {
      return handleIncommingGet(method.apply(this, arguments));
    };
  };

  var addField = function(inst, name, get, set) {
    print('adding field: ', name);
    Object.defineProperty(inst, name, {
      enumerable: true,
      // writable: false,
      // configurable: true,
      get: function() {
        print('getting: ', name);
        return handleIncommingGet(get());
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
      addMethod(instance, k, methods[k]);
    }

    // Add getters and setters for the fields
    var fields = javaData.fields;
    for (var k in fields) {
      addField(instance, k, fields[k].get, fields[k].set);
    }

    return instance;
  };


  var getClass = function(className) {
    var existing = classMap[className];
    if (existing) {
      return existing;
    }
    print('getting class data for: ', className);
    var classInfo = JavaGetClass(className);

    if (!classInfo.found) {
      return null;
    }

    var classConstructor = {
      create: function() {
        var instData;
        if (arguments[0] === 'EXISTING_INSTANCE') {
          print('adopting java instance: ', className);
          instData = arguments[1];
        } else {
          print('creating new instance: ', className);
          var args = Array.prototype.slice.call(arguments);
          args.unshift(className);
          instData = JavaCreateInstance.apply(this, args);
        }

        this.__javaInstance = instData.__javaInstance;
        this.__javaClass = instData.__javaClass;
        print('(inst: ' + this.__javaClass + ' : ' + this.__javaInstance + ')');

        if (instanceMap[this.__javaInstance]){
          print('WARNING: instanceMap collision');
        }
        instanceMap[this.__javaInstance] = this;

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
