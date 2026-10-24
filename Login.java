
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.sun.net.httpserver.HttpExchange;

public class Login {

    public static void handle(HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(
                exchange.getRequestMethod())) {

            exchange.getResponseHeaders().set(
                "Allow", "POST");

            exchange.sendResponseHeaders(405, -1);
            return;
        }

        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        String username = "";
        String password = "";

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2) {
                String key = URLDecoder.decode(
                    parts[0],
                    StandardCharsets.UTF_8
                );

                String value = URLDecoder.decode(
                    parts[1],
                    StandardCharsets.UTF_8
                );

                if ("username".equals(key)) {
                    username = value;
                } else if ("password".equals(key)) {
                    password = value;
                }
            }
        }

        boolean valid = false;

        try (
            Connection connection =
                DBConnect.getConnection();

            PreparedStatement statement =
                connection.prepareStatement(
                    "SELECT password_hash FROM users " +
                    "WHERE username = ?"
                )
        ) {
            statement.setString(1, username);

            try (ResultSet result =
                     statement.executeQuery()) {

                if (result.next()) {
                    String storedHash =
                        result.getString("password_hash");

                    valid = PasswordUtil.verify(
                        password,
                        storedHash
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        if (valid) {

            // Create a session after successful login.
            String sessionId =
                SessionManager.createSession();

            // Store the session token in a browser cookie.
            exchange.getResponseHeaders().set(
                "Set-Cookie",
                "PVMS_SESSION=" + sessionId
                    + "; Path=/; HttpOnly; SameSite=Lax"
            );

            // Redirect the user to the dashboard.
            exchange.getResponseHeaders().set(
                "Location",
                "/dashboard"
            );

            exchange.sendResponseHeaders(302, -1);

        } else {

            byte[] response = (
                "<!DOCTYPE html>" +
                "<html><head><title>Login Failed</title></head>" +
                "<body>" +
                "<h2>Login failed</h2>" +
                "<p>Check your username and password.</p>" +
                "<a href='/login.html'>Try again</a>" +
                "</body></html>"
            ).getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set(
                "Content-Type",
                "text/html; charset=UTF-8"
            );

            exchange.sendResponseHeaders(
                401,
                response.length
            );

            try (OutputStream output =
                     exchange.getResponseBody()) {

                output.write(response);
            }
        }
    }
}
