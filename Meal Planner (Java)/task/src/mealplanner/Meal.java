package mealplanner;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public abstract class Meal {
    protected int id;
    protected String category;
    protected String name;
    protected LinkedHashSet<String> ingredients;
    private final Map<String, Map<String, Meal>> weeklyPlan = new LinkedHashMap<>();

    public Meal(String category, String name, LinkedHashSet<String> ingredients) {
        this.category = category;
        this.name = name;
        this.ingredients = ingredients;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public LinkedHashSet<String> getIngredients() {
        return ingredients;
    }

    public void display() {
        System.out.println("Category: " + category);
        System.out.println("Name: " + name);
        System.out.println("Ingredients:");
        for (String ingredient : ingredients) {
            System.out.println(ingredient);
        }
    }
}