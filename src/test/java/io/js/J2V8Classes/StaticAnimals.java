package io.js.J2V8Classes;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by Brown on 4/27/16.
 */
public class StaticAnimals {
    public static int SomeNumber = 123;
    public static String[] StringArray = new String[]{"test1", "test2", "test3", "test4"};

    public static String SomeFunc(String s) {
        return s + "!";
    }
    public static String SomeFuncWithStringArray(String[] s) {
        return Arrays.toString(s);
    }
    public static String SomeFuncWithArrayList(ArrayList list){
        return list.toString();
    }

    public static Animal cat = new Animal("cat");
    public static Animal dog = new Animal("dog");
    public static Animal field_very_obfuscated_2394820948 = new Animal("bald_eagle");

    public static ArrayList<Animal> animals = new ArrayList<>();
    public static void registerAnimal(Animal animal) {
        animals.add(animal);
    }
}
