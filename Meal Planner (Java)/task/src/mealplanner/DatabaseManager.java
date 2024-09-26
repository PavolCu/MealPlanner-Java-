
package mealplanner;

import java.sql.*;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:postgresql:meals_db";
    private static final String USER = "postgres";
    private static final String PASS = "1111";
    private final Connection connection;
    private int nextMealId;

    public DatabaseManager() throws SQLException {
        connection = DriverManager.getConnection(DB_URL, USER, PASS);
        connection.setAutoCommit(true);
        initializeDatabase();
        nextMealId = getMaxMealId() + 1;
    }

    public Connection getConnection() {
        return connection;
    }

    public void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            //dropTables();
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS meals (" +
                    "meal_id SERIAL PRIMARY KEY, " +
                    "category VARCHAR(255), " +
                    "meal VARCHAR(255))");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ingredients (" +
                    "ingredient_id SERIAL PRIMARY KEY, " +
                    "ingredient VARCHAR(255), " +
                    "meal_id INT)");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS meal_planner (" +
                    "day VARCHAR(255), " +
                    "meal_category VARCHAR(255), " +
                    "meal_id INT, " +
                    "FOREIGN KEY (meal_id) REFERENCES meals(meal_id))");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS plan (" +
                    "meal_option VARCHAR(255), " +
                    "meal_category VARCHAR(255), " +
                    "meal_id INT, " +
                    "FOREIGN KEY (meal_id) REFERENCES meals(meal_id))");


        } catch (SQLException e) {
            e.printStackTrace();
            throw new SQLException("An exception was thrown while trying to create tables - " + e.getMessage(), e);
        }
    }

    private int getMaxMealId() throws SQLException {
        String query = "SELECT COALESCE(MAX(meal_id), 0) AS max_meal_id FROM meals";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("max_meal_id");
            }
        }
        return 0;
    }

    public int getNextMealId() {
        return nextMealId++;
    }
    // Method to drop tables
public void dropTables() throws SQLException {
    try (Statement stmt = connection.createStatement()) {
        stmt.executeUpdate("DROP TABLE IF EXISTS meal_planner CASCADE");
        stmt.executeUpdate("DROP TABLE IF EXISTS ingredients CASCADE");
        stmt.executeUpdate("DROP TABLE IF EXISTS meals CASCADE");
        stmt.executeUpdate("DROP TABLE IF EXISTS plan CASCADE");
    } catch (SQLException e) {
        e.printStackTrace();
        throw new SQLException("An exception was thrown while trying to drop tables - " + e.getMessage(), e);
    }
}
}