package io.js.J2V8Classes;

import com.eclipsesource.v8.V8Array;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Brown on 4/26/16.
 */
public class Utils {

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
            res[i - start] = v8array.get(i);
        }
        return res;
    }
}
