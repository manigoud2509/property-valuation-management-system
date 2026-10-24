
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Scanner;

public class CreateUser {
    public static void main(String[] args) throws Exception {
        Scanner input = new Scanner(System.in);

        System.out.print("Enter username: ");
        String username = input.nextLine();

        System.out.print("Enter password: ");
        String password = input.nextLine();

        String passwordHash = PasswordUtil.hash(password);

        try (Connection con = DBConnect.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {

            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ps.executeUpdate();

            System.out.println("User account created successfully!");
        }
    }
}
