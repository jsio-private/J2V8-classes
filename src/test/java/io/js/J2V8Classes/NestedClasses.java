package io.js.J2V8Classes;

/**
 * Created by Brown on 4/28/16.
 */
public class NestedClasses {
    static class Class1 extends Object
    {
        public String coolFunc() {
            return "cool";
        }
    }

    public static class Class2 extends Object
    {
        public String coolFunc2() {
            return "cool2";
        }
    }

    public static Class1 c1Inst = new Class1();
}
