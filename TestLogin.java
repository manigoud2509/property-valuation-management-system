
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class TestLogin {
    public static void main(String[] args) throws Exception {
        Scanner input = new Scanner(System.in);

        System.out.print("Enter your exact username: ");
        String username = input.nextLine();

        System.out.print("Enter the password you use to log in: ");
        String password = input.nextLine();

        try (Connection con = DBConnect.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT password_hash FROM users WHERE username = ?")) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    System.out.println(
                        "Password verification: " +
                        (PasswordUtil.verify(password, storedHash)
                            ? "MATCH" : "NO MATCH")
                    );
                } else {
                    System.out.println("Username not found.");
                }
            }
        }
    }
}
