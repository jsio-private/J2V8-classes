package io.js.J2V8Classes;

/**
 * Created by Brown on 4/27/16.
 */
public class CustomJavaAnimal extends Animal {
    public String name;

    public CustomJavaAnimal(String type, String name) {
        super(type);
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
