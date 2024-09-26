package mealplanner;

import java.util.LinkedHashSet;

public class Breakfast extends Meal {
    public Breakfast(String name, LinkedHashSet<String> ingredients) {
        super("breakfast", name, ingredients);
    }
}
