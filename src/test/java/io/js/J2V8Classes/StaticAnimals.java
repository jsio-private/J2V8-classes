package io.js.J2V8Classes;

import java.util.ArrayList;

/**
 * Created by Brown on 4/27/16.
 */
public class StaticAnimals {
    public static int SomeNumber = 123;

    public static String SomeFunc(String s) {
        return s + "!";
    }

    public static Animal cat = new Animal("cat");
    public static Animal dog = new Animal("dog");

    public static ArrayList<Animal> animals = new ArrayList<>();
    public static void registerAnimal(Animal animal) {
        animals.add(animal);
    }
}
