import java.util.Scanner;

class User {

    private String login;
    private String firstName;
    private String lastName;

    public User(String login, String firstName, String lastName) {
        this.login = login;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return "login=" + login + ",firstName=" + firstName + ",lastName=" + lastName;
    }
}

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);


        String input = scanner.nextLine();
        String[] parts = input.split(" ");

        if (parts.length == 3) {
            User user = new User(parts[0], parts[1], parts[2]);
            System.out.println(user);
        } else {
            System.out.println("Invalid input. Please enter exactly three values separated by spaces.");
        }
    }
}