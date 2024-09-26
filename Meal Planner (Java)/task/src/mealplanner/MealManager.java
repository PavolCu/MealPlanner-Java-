package mealplanner;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MealManager {
    private final LinkedHashMap<String, Meal> meals = new LinkedHashMap<>();
    private final DatabaseManager dbManager;
    private final Map<String, Map<String, Meal>> weeklyPlan = new LinkedHashMap<>();

    public MealManager(DatabaseManager dbManager) throws SQLException {
        this.dbManager = dbManager;
        loadMealsFromDatabase();
    }

    // Load meals from the database into the 'meals' map
    private void loadMealsFromDatabase() throws SQLException {
        String queryMeals = "SELECT * FROM meals";
        try (Statement statement = dbManager.getConnection().createStatement();
             ResultSet rsMeals = statement.executeQuery(queryMeals)) {

            while (rsMeals.next()) {
                String category = rsMeals.getString("category");
                String mealName = rsMeals.getString("meal");
                int mealID = rsMeals.getInt("meal_id");

                LinkedHashSet<String> ingredients = new LinkedHashSet<>();
                String queryIngredients = "SELECT ingredient FROM ingredients WHERE meal_id = ?";
                try (PreparedStatement ps = dbManager.getConnection().prepareStatement(queryIngredients)) {
                    ps.setInt(1, mealID);
                    ResultSet rsIngredients = ps.executeQuery();
                    while (rsIngredients.next()) {
                        ingredients.add(rsIngredients.getString("ingredient"));
                    }
                }

                Meal meal = createMeal(category, mealName, ingredients);
                meal.setId(mealID);
                meals.put(mealName.toLowerCase(), meal);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("Error loading meals from database", e);
        }
    }

    // Method for adding a meal interactively
    public void addMealInteractive(Scanner scanner) throws SQLException {
        String category = promptForCategory(scanner);
        String name = promptForName(scanner);
        LinkedHashSet<String> ingredients = promptForIngredients(scanner);

        Meal meal = createMeal(category, name, ingredients);
        addMeal(meal);
    }

    private Meal createMeal(String category, String name, LinkedHashSet<String> ingredients) {
        return switch (category) {
            case "breakfast" -> new Breakfast(name, ingredients);
            case "lunch" -> new Lunch(name, ingredients);
            case "dinner" -> new Dinner(name, ingredients);
            default -> throw new IllegalArgumentException("Invalid category: " + category);
        };
    }

    private String promptForName(Scanner scanner) {
        System.out.println("Input the meal's name:");
        String name = scanner.nextLine().trim().toLowerCase();

        while (isValidNameOrIngredient(name)) {
            System.out.println("Wrong format. Use letters only!");
            name = scanner.nextLine().trim().toLowerCase();
        }
        return name;
    }

    public void showMealsInteractive(Scanner scanner) {
        System.out.println("Which category do you want to print (breakfast, lunch, dinner)?");
        String category = scanner.nextLine().trim().toLowerCase();

        while (isValidCategory(category)) {
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            category = scanner.nextLine().trim().toLowerCase();
        }

        List<Meal> categoryMeals = getMealsByCategory(category);

        if (categoryMeals.isEmpty()) {
            System.out.println("No meals found.");
        } else {
            categoryMeals.sort(Comparator.comparing(Meal::getName));
            System.out.println("Category: " + category);
            for (Meal meal : categoryMeals) {
                meal.display();
                System.out.println();
            }
        }
    }

    private LinkedHashSet<String> promptForIngredients(Scanner scanner) {
        System.out.println("Input the ingredients:");
        String ingredientsInput = scanner.nextLine().trim().toLowerCase();

        while (!isValidIngredientsInput(ingredientsInput)) {
            System.out.println("Wrong format. Use letters only!");
            ingredientsInput = scanner.nextLine().trim().toLowerCase();
        }
        return new LinkedHashSet<>(Arrays.asList(ingredientsInput.split(",\\s*")));
    }

    private String promptForCategory(Scanner scanner) {
        System.out.println("Which meal do you want to add (breakfast, lunch, dinner)?");
        String category = scanner.nextLine().trim().toLowerCase();

        while (isValidCategory(category)) {
            System.out.println("Wrong meal category! Choose from: breakfast, lunch, dinner.");
            category = scanner.nextLine().trim().toLowerCase();
        }
        return category;
    }

    // Method to add a meal to the database and the 'meals' map
    public void addMeal(Meal meal) throws SQLException {
        int mealId = dbManager.getNextMealId();
        meal.setId(mealId);

        String insertMealSQL = "INSERT INTO meals (meal_id, category, meal) VALUES (?, ?, ?)";
        try (PreparedStatement psMeal = dbManager.getConnection().prepareStatement(insertMealSQL)) {
            psMeal.setInt(1, mealId);
            psMeal.setString(2, meal.getCategory());
            psMeal.setString(3, meal.getName());
            psMeal.executeUpdate();

            String insertIngredientSQL = "INSERT INTO ingredients (ingredient, meal_id) VALUES (?, ?)";
            try (PreparedStatement psIngredients = dbManager.getConnection().prepareStatement(insertIngredientSQL)) {
                for (String ingredient : meal.getIngredients()) {
                    psIngredients.setString(1, ingredient);
                    psIngredients.setInt(2, mealId);
                    psIngredients.addBatch();
                }
                psIngredients.executeBatch();
            }
        }
        meals.put(meal.getName().toLowerCase(), meal);
        System.out.println("The meal has been added!");
    }


    // Method to plan meals for the week
    public void planMeals(Scanner scanner) throws SQLException {
        // Delete existing plan
        try (Statement stmt = dbManager.getConnection().createStatement()) {
            stmt.executeUpdate("DELETE FROM meal_planner WHERE day = ('Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday')");
        }

        String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        String[] mealCategories = {"breakfast", "lunch", "dinner"};

        weeklyPlan.clear(); // Clear the previous plan

        for (String day : daysOfWeek) {
            System.out.println(day);
            Map<String, Meal> mealsForDay = new LinkedHashMap<>();

            for (String category : mealCategories) {
                List<Meal> categoryMeals = getMealsByCategory(category);

                if (categoryMeals.isEmpty()) {
                    System.out.println("No meals available for category: " + category);
                    continue;
                }

                // Print meals in alphabetical order
                categoryMeals.sort(Comparator.comparing(Meal::getName));
                for (Meal meal : categoryMeals) {
                    System.out.println(meal.getName());
                }

                System.out.printf("Choose the %s for %s from the list above:%n", category, day);
                String chosenMealName = scanner.nextLine().trim().toLowerCase();

                Meal chosenMeal = null;
                while (true) {
                    for (Meal meal : categoryMeals) {
                        if (meal.getName().equalsIgnoreCase(chosenMealName)) {
                            chosenMeal = meal;
                            break;
                        }
                    }

                    if (chosenMeal != null) {
                        mealsForDay.put(category, chosenMeal);
                        break;
                    } else {
                        System.out.println("This meal doesn’t exist. Choose a meal from the list above.");
                        chosenMealName = scanner.nextLine().trim().toLowerCase();
                    }
                }
            }

            weeklyPlan.put(day, mealsForDay);
            System.out.printf("Yeah! We planned the meals for %s.%n%n", day);
        }

        // Save the plan to the database
        savePlanToDatabase(weeklyPlan);

        // Print the plan
        printWeeklyPlan(weeklyPlan);
    }

    // Method to get meals by category
    private List<Meal> getMealsByCategory(String category) {
        List<Meal> categoryMeals = new ArrayList<>();
        for (Meal meal : meals.values()) {
            if (meal.getCategory().equals(category)) {
                categoryMeals.add(meal);
            }
        }
        return categoryMeals;
    }

    // Method to save the weekly plan to the database
    private void savePlanToDatabase(Map<String, Map<String, Meal>> weeklyPlan) throws SQLException {
        String insertSQL = "INSERT INTO meal_planner (day, meal_category, meal_id) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(insertSQL)) {
            for (Map.Entry<String, Map<String, Meal>> dayEntry : weeklyPlan.entrySet()) {
                String day = dayEntry.getKey();
                Map<String, Meal> mealsForDay = dayEntry.getValue();

                for (Map.Entry<String, Meal> mealEntry : mealsForDay.entrySet()) {
                    String category = mealEntry.getKey();
                    Meal meal = mealEntry.getValue();

                    pstmt.setString(1, day);
                    pstmt.setString(2, category);
                    pstmt.setInt(3, meal.getId());
                    pstmt.addBatch();
                }
            }
            pstmt.executeBatch();
        }
    }

    // Method to print the weekly plan
    private void printWeeklyPlan(Map<String, Map<String, Meal>> weeklyPlan) {
        for (Map.Entry<String, Map<String, Meal>> dayEntry : weeklyPlan.entrySet()) {
            String day = dayEntry.getKey();
            Map<String, Meal> mealsForDay = dayEntry.getValue();

            System.out.println(day);
            for (String category : Arrays.asList("breakfast", "lunch", "dinner")) {
                Meal meal = mealsForDay.get(category);
                if (meal != null) {
                    System.out.printf("%s: %s%n", capitalize(category), meal.getName());
                }
            }
            System.out.println();
        }
    }

    // Method to list the stored plan
    public void listPlan() throws SQLException {
        String query = "SELECT * FROM meal_planner ORDER BY day, meal_category";
        Map<String, Map<String, Meal>> weeklyPlan = new LinkedHashMap<>();

        try (Statement stmt = dbManager.getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            if (!rs.isBeforeFirst()) {
                System.out.println("Database does not contain any meal plans.");
                return;
            }

            while (rs.next()) {
                String day = rs.getString("day");
                String category = rs.getString("meal_category");
                int mealId = rs.getInt("meal_id");

                Meal meal = getMealById(mealId);
                if (meal == null) {
                    continue;
                }

                weeklyPlan.computeIfAbsent(day, k -> new LinkedHashMap<>()).put(category, meal);
            }
        }

        // Print the plan
        printWeeklyPlan(weeklyPlan);
    }

    // Helper method to get a meal by its ID
    private Meal getMealById(int mealId) throws SQLException {
        String query = "SELECT * FROM meals WHERE meal_id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(query)) {
            pstmt.setInt(1, mealId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String category = rs.getString("category");
                    String name = rs.getString("meal");
                    int id = rs.getInt("meal_id");

                    LinkedHashSet<String> ingredients = new LinkedHashSet<>();
                    String ingredientsQuery = "SELECT ingredient FROM ingredients WHERE meal_id = ?";
                    try (PreparedStatement psIngredients = dbManager.getConnection().prepareStatement(ingredientsQuery)) {
                        psIngredients.setInt(1, id);
                        try (ResultSet rsIngredients = psIngredients.executeQuery()) {
                            while (rsIngredients.next()) {
                                ingredients.add(rsIngredients.getString("ingredient"));
                            }
                        }
                    }

                    Meal meal;
                    switch (category) {
                        case "breakfast":
                            meal = new Breakfast(name, ingredients);
                            break;
                        case "lunch":
                            meal = new Lunch(name, ingredients);
                            break;
                        case "dinner":
                            meal = new Dinner(name, ingredients);
                            break;
                        default:
                            return null;
                    }
                    meal.setId(id);
                    return meal;
                }
            }
        }
        return null;
    }

    // Helper method to capitalize the first letter of a string
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }


    // Method to validate meal categories
    public boolean isValidCategory(String category) {
        return category.equals("breakfast") || category.equals("lunch") || category.equals("dinner");
    }

    // Method to validate meal names and ingredients
    public boolean isValidNameOrIngredient(String input) {
        Pattern pattern = Pattern.compile("^[a-zA-Z ]+$");
        Matcher matcher = pattern.matcher(input);
        return !matcher.matches();
    }

    // Method to validate ingredients input
    public boolean isValidIngredientsInput(String input) {
        String[] ingredients = input.split(",\\s*");
        for (String ingredient : ingredients) {
            if (isValidNameOrIngredient(ingredient)) {
                return false;
            }
        }
        return true;
    }

    public void saveShoppingList(Scanner scanner) {
        if (weeklyPlan.isEmpty()) {
            System.out.println("Unable to save. Plan your meals first.");
            return;
        }

        Map<String, Integer> shoppingList = new HashMap<>();
        for (Map<String, Meal> dayMeals : weeklyPlan.values()) {
            for (Meal meal : dayMeals.values()) {
                for (String ingredient : meal.getIngredients()) {
                    shoppingList.put(ingredient, shoppingList.getOrDefault(ingredient, 0) + 1);
                }
            }
        }

        System.out.println("Input a filename:");
        String filename = scanner.nextLine().trim();

        try (FileWriter writer = new FileWriter(filename)) {
            for (Map.Entry<String, Integer> entry : shoppingList.entrySet()) {
                if (entry.getValue() > 1) {
                    writer.write(entry.getKey() + " x" + entry.getValue() + "\n");
                } else {
                    writer.write(entry.getKey() + "\n");
                }
            }
            System.out.println("Saved!");
        } catch (IOException e) {
            System.out.println("An error occurred while saving the file.");
            e.printStackTrace();
        }
    }
}