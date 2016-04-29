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
  var _INTERNAL_EXTEND = '__INTERNAL_EXTEND';
  var _CLASS_INITIALIZING = '__CLASS_INITIALIZING';

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

    // Check for arrays
    // if (res.__javaClass.lastIndexOf('[]') == res.__javaClass.length - 2) {
    // }
    if (Array.isArray(res)) {
      print('handleIncommingGet: array, ensuring values are backed by classes if needed ', res.length);
      for (var i = 0, j = res.length; i < j; i++) {
        res[i] = handleIncommingGet(res[i]);
      }
    }
    else if (res.__javaInstance !== undefined) {
      print('handleIncommingGet: class instance: (', res.__javaClass, ':', res.__javaInstance, ')');
      var existingInstance = instanceMap[res.__javaInstance];
      if (existingInstance) {
        print('handleIncommingGet: already exists');
        return existingInstance;
      }
      if (isInitializing(res.__javaClass)) {
        return null;
      }
      // make the instance
      var clz = getClass(res.__javaClass);
      var inst = new clz(_FROM_JAVA, res);
      return inst;
    }

    return res;
  };


  var addMethod = function(inst, name, method) {
    print('adding method: ', name, typeof method, method);
    inst[name] = function() {
      print('running method: ', name);
      // print('> keys: ', Object.keys(this));
      var javaRes = method.apply(this, arguments);
      return handleIncommingGet(javaRes);
    };
  };

  var addField = function(inst, name, get, set) {
    print('adding field: ', name);
    if (!get || !set) {
      print('WARNING: fields must have both a getter and a setter');
      return;
    }

    Object.defineProperty(inst, name, {
      enumerable: true,
      // configurable: true,
      get: function() {
        print('getting(op): ', name, ' (', this.__javaInstance || this.__javaClass, ')');
        return handleIncommingGet(get.call(this));
      },
      // get: get,
      set: function(v) {
        print('setting(op): ', name, ' (', this.__javaInstance || this.__javaClass, ')');
        set.call(this, v);
      }
    });
  };


  var addProxies = function(instance, javaData, opts) {
    opts = opts || {};

    if (!javaData) {
      return instance;
    }

    // Add all the js -> java methods
    var methods = javaData.methods;
    if (methods && opts.methods !== false) {
      for (var k in methods) {
        addMethod(instance, k, methods[k]);
      }
    }

    // Add getters and setters for the fields
    var fields = javaData.fields;
    var _GET_PREFIX = '__get_';
    var _SET_PREFIX = '__set_';
    if (fields && opts.fields !== false) {
      for (var k in fields) {
        if (k.indexOf(_GET_PREFIX) !== 0) {
          continue;
        }

        var strippedK = k.substring(_GET_PREFIX.length);
        addField(
          instance,
          strippedK,
          fields[k],
          fields[_SET_PREFIX + strippedK]
        );
      }
    }

    return instance;
  };


  var _classMixins = {};

  var registerMixins = function(className, mixins) {
    if (_classMixins[className]) {
      print('WARNING: Mixin collision for ', className);
    }
    _classMixins[className] = mixins;
  };

  var getMixins = function(className) {
    return _classMixins[className];
  };


  var funcSafeMerge = function(origObj, newObj) {
    if (!origObj) {
      return newObj;
    }
    if (!newObj) {
      return origObj;
    }

    var origT = typeof(origObj);
    var newT = typeof(newObj);
    if (origT !== newT) {
      print('WARNING: Cannot merge, mismatch types: ', origT, ' !== ', newT);
      return origT;
    }
    if (origT === 'function') {
      return function() {
        print('running merged function: ', this.$class.$name, ' ', Object.keys(this));
        origObj.apply(this, arguments);
        return newObj.apply(this, arguments);
      };
    }
    if (Array.isArray(origObj)) {
      return origObj.concat(newObj);
    }
    if (origT === 'object') {
      var res = {};
      for (var k in newObj) {
        if (origObj[k]) {
          res[k] = funcSafeMerge(origObj[k], newObj[k]);
        } else {
          res[k] = newObj[k];
        }
      }
      return res;
    }
    // if (origT === 'string') {
    //   return newObj;
    // }
    // if (origT === 'number') {
    //   return newObj;
    // }
    // print('WARNING: Cannot merge, unknown type ', origT);
    return newObj;
  };


  var addJavaFields = function(fromClass) {
    if (!this.__javaInstance) {
      print('WARNING: Trying to addJavaFields from ', fromClass, ' to an instance not backed by java');
      return;
    }

    print('addJavaFields: from ', fromClass, ' to ', this.__javaInstance);
    var classInfo = getClassInfo(fromClass);
    var superClass = classInfo.__javaSuperclass;
    if (superClass) {
      addJavaFields.call(this, superClass);
    }

    print('addJavaFields: Adding proxies for ', fromClass);
    // print('TEST: ', Object.keys(classInfo));
    addProxies(this, classInfo.publics, { methods: false });
  };


  var isInitializing = function(className) {
    return classMap[className] === _CLASS_INITIALIZING;
  };
  var isDynamicClass = function(className) {
    return className.indexOf(_DYNAMIC_PACKAGE) === 0;
  };


  var _classInfoCache = {};

  var getClassInfo = function(className) {
    var existing = _classInfoCache[className];
    if (existing) {
      return existing;
    }

    // Make the classInfo js side, so that it isnt released
    var classInfo = getBlankClassInfo();
    JavaGetClass(className, classInfo);
    _classInfoCache[className] = classInfo;
    return classInfo;
  };


  var getClass = function(className) {
    if (!className) {
      throw new Error('Must provide className to getClass');
    }

    var existing = classMap[className];
    if (existing) {
      if (isInitializing(className)) {
        print('WARNING: Calling getClass() from inside getClass() stack (class is already initializing)');
        return null;
      }
      return existing;
    }
    classMap[className] = _CLASS_INITIALIZING;
    print('getting class data for: ', className);
    var classInfo = getClassInfo(className);

    if (!classInfo.__javaClass) {
      print('WARNING: Class not found: ', className);
      return null;
    }

    var internalClassInit = function() {
      print('Running internalClassInit for ', className);
      var instChildClass = this.$class.__javaClass;

      if (isDynamicClass(className)) {
        // Wait to call createInstance until we have js args for js -> java super
        print('WARNING: internalClassInit called with dynamic class ', className);
        return;
      }

      var instData;
      var isSuper = arguments[0] === _IS_SUPER;
      if (isSuper || arguments[0] === _FROM_JAVA) {
        print('adopting java instance: ', className);
        instData = arguments[1];
      } else {
        print('creating new instance: ', instChildClass, ' (non dynamic super ', className, ')');
        var args = Array.prototype.slice.call(arguments);
        args.unshift(instChildClass);
        print('> Instance args: ', JSON.stringify(args));
        instData = JavaCreateInstance.apply(this, args);

        if (!instData) {
          print('WARNING: instData not valid... JavaCreateInstance must have failed');
          throw new Error('JavaCreateInstance failed');
        }
      }


      if (this.__internalClassInitCalled) {
        return;
      }
      this.__internalClassInitCalled = true;


      if (!instData.__javaInstance) {
        print('WARNING: No instData.__javaInstance');
      } else {
        this.__javaInstance = instData.__javaInstance;
        print('(inst: ' + instChildClass + ' : ' + this.__javaInstance + ')');
      }

      var existing = instanceMap[this.__javaInstance];
      if (!isSuper) {
        if (instanceMap[this.__javaInstance]) {
          // TODO: this is fired for super classes
          print('WARNING: instanceMap collision. Instance already in map: ' + existing.$class.__javaClass + ':' + existing.__javaInstance);
        } else {
          instanceMap[this.__javaInstance] = this;
        }
      }

      // Manually back in the field properties (abitbol handles the functions...?)
      addJavaFields.call(this, instChildClass);
    };

    var classConstructor = {
      __name__: classInfo.__javaClass.substring(classInfo.__javaClass.lastIndexOf('.') + 1)
    };

    if (!isDynamicClass(className)) {
      classConstructor.__init__ = function() {
        internalClassInit.apply(this, arguments);
      };
    }

    var superClz = null;
    var superClzName = classInfo.__javaSuperclass;
    if (superClzName && superClzName !== 'java.lang.Object') {
      print('Getting super (', className, ' extends ', superClzName, ')');
      superClz = getClass(superClzName);
    }

    if (classInfo.publics) {
      print('Adding publics: ', JSON.stringify(classInfo.publics));
      addProxies(classConstructor, classInfo.publics);
    }

    if (classInfo.statics) {
      print('Adding statics: ', JSON.stringify(classInfo.statics));
      classConstructor.__classvars__ = addProxies(
        {
          __javaClass: classInfo.__javaClass
        },
        classInfo.statics
      );
    }

    // Add any original js mixins if they exist
    var jsMixins = getMixins(className);
    if (jsMixins) {
      print('Adding JS mixins: ', Object.keys(jsMixins));
      Object.keys(jsMixins).forEach(function(k) {
        var v = jsMixins[k];
        var existing = classConstructor[k];
        classConstructor[k] = funcSafeMerge(existing, v);
      });
    }

    // Ensure that there is an __init__
    if (!classConstructor.__init__) {
      print('No user defined init, using internalClassInit');
      classConstructor.__init__ = internalClassInit;
    }

    print('Generating abitbol class: ', classConstructor.__name__, ' classConstructor: ', Object.keys(classConstructor));
    var clz;
    if (superClz) {
      print('> Using super.$extend for: "', superClz.$name, '" (', superClz.__javaClass, ')');
      clz = superClz.$extend(_INTERNAL_EXTEND, classConstructor);
    } else {
      print('> No super, running Class.$extend');
      clz = Class.$extend(classConstructor);
    }

    clz.__javaClass = classInfo.__javaClass;
    clz.__javaSuperclass = classInfo.__javaSuperclass;

    Object.defineProperty(clz, '$extend', {
        enumerable: false,
        configurable: true,
        value: function() {
          print('Custom $extend');

          if (arguments[0] === _INTERNAL_EXTEND) {
            print('> Internal extend, running Class.$extend');
            var args = Array.prototype.slice.call(arguments);
            args.shift();
            return Class.$extend.apply(this, args);
          }

          var superClass = this.$class.__javaClass;
          if (!superClass) {
            print('> No superClass, running Class.$extend');
            return Class.$extend.apply(this, arguments);
          }

          if (_classUid >= _MAX_CUSTOM_CLASS_COUNT) {
            print('WARNING: max custom class count hit, falling back to js class extend');
            return Class.$extend.apply(this, arguments);
          }

          var newClassConstructor = arguments[0];
          var className = newClassConstructor.__name__;
          if (!className) {
            className = 'Dynamic_' + (_classUid++);
          }

          var javaClass = _DYNAMIC_PACKAGE + '.Dynamic.' + className;
          print('> parent.__javaClass= ', superClass);
          print('> new javaClass: ', javaClass);

          registerMixins(javaClass, newClassConstructor);

          var methods = [];
          var fields = [];

          // var keys = Object.keys(resultClz);
          // for (var i in keys) {
          //   var k = keys[i];
          //   if (k.charAt(0) === '_') {
          //     continue;
          //   }

          //   var v = resultClz[k];
          //   var t = typeof(v);
          //   if (t === 'function') {
          //     methods.push({
          //       type: t,
          //       name: k
          //     });
          //   }
          // }

          print('> Generating Java class');
          JavaGenerateClass(
            javaClass,
            superClass,
            // fields,
            methods
          );

          print('> Creating JS class');
          return getClass(javaClass);
        }
    });

    print('Class info load complete: ', className);
    classMap[className] = clz;

    return clz;
  };

  var _DYNAMIC_PACKAGE = 'io.js.enderScript';
  var _MAX_CUSTOM_CLASS_COUNT = 1000;
  var _classUid = 0;


  var getBlankClassInfo = function() {
    return {
      publics: {
        fields: {},
        methods: {}
      },
      statics: {
        fields: {},
        methods: {}
      }
    };
  };


  return {
    getClass: getClass,
    getBlankClassInfo: getBlankClassInfo
  };

}));
