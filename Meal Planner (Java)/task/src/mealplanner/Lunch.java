package mealplanner;

import java.util.LinkedHashSet;

public class Lunch extends Meal {
    public Lunch(String name, LinkedHashSet<String> ingredients) {
        super("lunch", name, ingredients);
    }

}
