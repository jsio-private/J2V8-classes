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

  var _FROM_JAVA = '__EXISTING_INSTANCE';
  var _IS_SUPER = '__IS_SUPER';

  var classMap = {};
  var instanceMap = {};


  var handleIncommingGet = function(res) {
    // print('handleIncommingGet: ', Object.keys(res));
    if (!res) {
      return undefined;
    }
    if (res.hasOwnProperty('v')) {
      res = res.v;
    }
    if (!res) {
      return null;
    }

    if (res.__javaInstance !== undefined) {
      print('handleIncommingGet: class instance: (', res.__javaClass, ':', res.__javaInstance, ')');
      var existingInstance = instanceMap[res.__javaInstance];
      if (existingInstance) {
        print('handleIncommingGet: already exists');
        return existingInstance;
      }
      // make the instance
      var clz = getClass(res.__javaClass);
      var inst = new clz(_FROM_JAVA, res);
      return inst;
    }

    return res;
  };


  var addMethod = function(inst, name, method) {
    print('adding method: ', name);
    inst[name] = function() {
      // print('running method: ', name, ' ', Object.keys(this));
      return handleIncommingGet(method.apply(this, arguments));
    };
  };

  var addField = function(inst, name, get, set, useObjProp) {
    print('adding field: ', name, ' useObjProp=', useObjProp);
    if (useObjProp) {
      Object.defineProperty(inst, name, {
        enumerable: true,
        get: function() {
          print('getting(op): ', name, ' (', this.__javaInstance, ')');
          return handleIncommingGet(get.call(this));
        },
        // get: get,
        set: function(v) {
          print('setting(op): ', name, ' (', this.__javaInstance, ')');
          set.call(this, v);
        }
      });
    } else {
      inst['get' + name] = function() {
        // print('getting: ', name, ' (', this.__javaInstance, ')');
        return handleIncommingGet(get.call(this));
      };
      inst['set' + name]  = function(v) {
        // print('setting: ', name, ' (', this.__javaInstance, ')');
        set.call(this, v);
      };
    }
  };


  var addProxies = function(instance, javaData, useObjProp) {
    if (!javaData) {
      return instance;
    }

    // Add all the js -> java methods
    var methods = javaData.methods;
    if (methods) {
      for (var k in methods) {
        addMethod(instance, k, methods[k]);
      }
    }

    // Add getters and setters for the fields
    var fields = javaData.fields;
    if (fields) {
      for (var k in fields) {
        addField(instance, k, fields[k].get, fields[k].set, useObjProp);
      }
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

    if (!classInfo) {
      print('WARNING: Class not found: ', className);
      return null;
    }

    var classConstructor = {
      __init__: function() {
        var instData;
        var isSuper = arguments[0] === _IS_SUPER;
        if (isSuper || arguments[0] === _FROM_JAVA) {
          print('adopting java instance: ', className);
          instData = arguments[1];
        } else {
          print('creating new instance: ', className);
          var args = Array.prototype.slice.call(arguments);
          args.unshift(className);
          instData = JavaCreateInstance.apply(this, args);
        }

        if (!instData.__javaInstance) {
          print('WARNING: No instData.__javaInstance');
        } else {
          this.__javaInstance = instData.__javaInstance;
          print('(inst: ' + className + ' : ' + this.__javaInstance + ')');
        }

        // if (instData.superData) {
        //   print('(', className, ' extends ', instData.superData.__javaClass, ')');
        //   print('superData= ', JSON.stringify(instData.superData));
        //   print(instData.superData.__javaClass, ' =?=', this.$class.__javaSuperclass);
        //   if (this.$super) {
        //     print('Running super constructor');
        //     this.$super('EXISTING_INSTANCE', instData);
        //   } else {
        //     print('WARNING: superData available, but no $super');
        //   }
        // }
        if (this.$super) {
          print('Running super constructor');
          this.$super(_IS_SUPER, {__javaInstance: instData.__javaInstance});
        }



        var existing = instanceMap[this.__javaInstance];
        if (instanceMap[this.__javaInstance] && !isSuper) {
          // TODO: this is fired for super classes
          print('WARNING: instanceMap collision. Instance already in map: ' + existing.$class.__javaClass + ':' + existing.__javaInstance);
        } else {
          instanceMap[this.__javaInstance] = this;
        }

        // addProxies(this, instData);
      }
    };

    var superClz = null;
    var superClzName = classInfo.__javaSuperclass;
    if (superClzName && superClzName !== 'java.lang.Object') {
      print('Loading super (', className, ' extends ', superClzName, ')');
      superClz = getClass(superClzName);
    }

    if (classInfo.publics) {
      print('Adding publics: ', JSON.stringify(classInfo.publics));
      addProxies(classConstructor, classInfo.publics);
    }

    if (classInfo.statics) {
      print('Adding statics: ', JSON.stringify(classInfo.statics));
      classConstructor.__classvars__ = addProxies({
        __javaClass: classInfo.__javaClass
      }, classInfo.statics, true);
    }

    var clz;
    if (superClz) {
      clz = superClz.$extend(classConstructor);
    } else {
      clz = Class.$extend(classConstructor);
    }

    clz.__javaClass = classInfo.__javaClass;
    clz.__javaSuperclass = classInfo.__javaSuperclass;

    print('Class info load complete: ', className);
    classMap[className] = clz;
    return clz;
  };

  return {
    getClass: getClass
  };

}));
