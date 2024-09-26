package mealplanner;

import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager dbManager = new DatabaseManager();
            dbManager.initializeDatabase();

            MealManager mealManager = new MealManager(dbManager);
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("What would you like to do (add, show, plan, list plan, exit)?");
                String choice = scanner.nextLine().trim().toLowerCase();

                switch (choice) {
                    case "add":
                        mealManager.addMealInteractive(scanner);
                        break;
                    case "show":
                        mealManager.showMealsInteractive(scanner);
                        break;
                    case "plan":
                        mealManager.planMeals(scanner);
                        break;
                    case "list plan":
                        mealManager.listPlan();
                        break;
                    case "save":
                        mealManager.saveShoppingList(scanner);
                    case "exit":
                        System.out.println("Bye!");
                        return;
                    default:
                        System.out.println("Invalid option. Please try again.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}