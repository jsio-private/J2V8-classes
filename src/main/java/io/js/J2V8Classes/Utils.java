package io.js.J2V8Classes;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by Brown on 4/26/16.
 */
public class Utils {

    private static Logger logger = Logger.getLogger("Utils");

    public static String getScriptSource(ClassLoader classLoader, String path) {
        InputStream in = classLoader.getResourceAsStream(path);
        try {
            return IOUtils.toString(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static Object[] v8arrayToObjectArray(V8Array v8array) {
        return v8arrayToObjectArray(v8array, 0, v8array.length());
    }
    public static Object[] v8arrayToObjectArray(V8Array v8array, int start) {
        return v8arrayToObjectArray(v8array, start, v8array.length());
    }
    public static Object[] v8arrayToObjectArray(V8Array v8array, int start, int end) {
        Object[] res = new Object[end - start];
        for (int i = start; i < end; i++) {
            Object o = v8array.get(i);
            int idx = i - start;
            res[idx] = o;
            // Replace instances with their java counterparts
            if (o instanceof V8Object) {
                V8Object v8o = (V8Object) o;
                if (v8o.contains("__javaInstance")) {
                    int instHash = v8o.getInteger("__javaInstance");
                    Object inst = getInstance(instHash);
                    if (inst == null) {
                        logger.warning("v8arrayToObjectArray: unknown instance: " + instHash);
                    } else {
                        res[idx] = inst;
                    }
                }
                v8o.release();
            }
        }
        return res;
    }


    public static void releaseAllFor(V8 runtime) {
        logger.info("releaseAllFor: " + runtime);
        Iterator<Map.Entry<Integer, V8Object>> it = jsInstanceMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, V8Object> pair = (Map.Entry)it.next();
            V8Object jso = pair.getValue();
            if (jso.getRuntime() == runtime) {
                int hash = pair.getKey();
                logger.info("> releasing: " + hash);
                jso.release();
                javaInstanceMap.remove(hash);
                it.remove();
            }
        }
        logger.info("> items still left: " + jsInstanceMap.size());
    }

    private static HashMap<Integer, Object> javaInstanceMap = new HashMap<Integer, Object>();
    private static HashMap<Integer, V8Object> jsInstanceMap = new HashMap<Integer, V8Object>();

    public static V8Object getV8ObjectForObject(V8 runtime, Object o) {
        int hash = o.hashCode();
        logger.info("Finding V8Object for: " + hash);
        if (jsInstanceMap.containsKey(hash)) {
            V8Object jsInst = (V8Object) jsInstanceMap.get(hash);
            if (!jsInst.isReleased()) {
                return jsInst.twin();
            }
            logger.warning("Trying to return a released instance!");
        }

        Class clz = o.getClass();
        String clzName = getClassName(clz);
        logger.info("> None found, registering new: " + clzName);

        V8Object res = new V8Object(runtime);
//        V8Object res = generateAllGetSet(runtime, clz, o, false);
        res.add("__javaInstance", hash);
        res.add("__javaClass", clzName);

        registerInstance(o);
        jsInstanceMap.put(hash, res);

        return res.twin();
    }

    public static Object getInstance(int hash) {
        if (!javaInstanceMap.containsKey(hash)) {
            logger.warning("Hash missing: " + hash);
            return null;
        }
        return javaInstanceMap.get(hash);
    }

    public static int registerInstance(Object o) {
        int hash = o.hashCode();
        if (javaInstanceMap.containsKey(hash)) {
            logger.warning("Hash collision: " + hash);
        }
        javaInstanceMap.put(hash, o);
        return hash;
    }

    public static String getClassName(Class clz) {
        String canonicalName = clz.getCanonicalName();
        if (canonicalName != null) {
            return canonicalName;
        }
        return clz.getName();
    }

    public static Class[] getArrayClasses(Object[] arr) {
        Class[] classes = new Class[arr.length];
        for (int i = 0; i < arr.length; i++) {
            classes[i] = arr[i].getClass();
        }
        return classes;
    }
}
