package mealplanner;

import java.util.LinkedHashSet;

public class Dinner extends Meal{
    public Dinner(String name, LinkedHashSet<String> ingredients) {
        super("dinner", name, ingredients);
    }

}
