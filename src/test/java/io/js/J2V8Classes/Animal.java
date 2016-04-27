package io.js.J2V8Classes;

/**
 * Created by Brown on 4/26/16.
 */
public class Animal {
    public String type;

    public Animal() {
        this("unknown");
    }

    public Animal(String type) {
        this.type = type;
    }

    public String getType() {
        return this.type;
    }
}
