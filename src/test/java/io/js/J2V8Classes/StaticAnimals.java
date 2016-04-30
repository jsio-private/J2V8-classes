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

    public static String[] SomeFuncArray(String[] s) {
        String[] res = Arrays.copyOf(s, s.length + 1);
        res[s.length] = "newVal";
        return res;
    }

    public static int[] SomeFuncArray(int[] ints) {
        int[] res = Arrays.copyOf(ints, ints.length + 1);
        res[ints.length] = 9;
        return res;
    }

    public static String[] SomeFuncVarargs(Animal ... animals) {
        String[] res = new String[animals.length];
        for (int i = 0; i < animals.length; i++) {
            res[i] = animals[i].getType();
        }
        return res;
    }

//    public static String[] SomeFuncArray(ArrayList<String> s) {
//        String[] res = Arrays.copyOf(s, s.length + 1);
//        res[s.length] = "newVal";
//        return res;
//    }

    public static Animal cat = new Animal("cat");
    public static Animal dog = new Animal("dog");

    public static ArrayList<Animal> animals = new ArrayList<>();

    public static Animal findAnimal(String type) {
        for (int i = 0; i < animals.size(); i++) {
            Animal animal = animals.get(i);
            if (animal.getType().equals(type)) {
                return animal;
            }
        }
        return null;
    }

    public static void registerAnimal(Animal animal) {
        animals.add(animal);
    }
}
