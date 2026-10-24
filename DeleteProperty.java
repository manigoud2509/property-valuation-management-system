
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.sun.net.httpserver.HttpExchange;

public class DeleteProperty {

    public static void handle(HttpExchange exchange) throws IOException {

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "POST method required.");
            return;
        }

        String body = new String(
            exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8
        );

        String propertyIdText = "";

        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts[0].equals("property_id")) {
                propertyIdText = parts.length > 1 ? parts[1] : "";
                break;
            }
        }

        int propertyId;

        try {
            propertyId = Integer.parseInt(propertyIdText);
            if (propertyId <= 0) {
                send(exchange, 400, "Enter a valid Property ID.");
                return;
            }
        } catch (NumberFormatException e) {
            send(exchange, 400, "Enter a valid Property ID.");
            return;
        }

        String sql = "DELETE FROM properties WHERE property_id = ?";

        try (Connection con = DBConnect.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, propertyId);
            int deleted = ps.executeUpdate();

            if (deleted == 1) {
                send(exchange, 200,
                    "Property ID " + propertyId + " deleted successfully.");
            } else {
                send(exchange, 404,
                    "No property found with ID " + propertyId + ".");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500,
                "Database error. Check the Java server terminal.");
        }
    }

    private static void send(
        HttpExchange exchange, int status, String message
    ) throws IOException {

        byte[] response = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type", "text/plain; charset=UTF-8"
        );
        exchange.sendResponseHeaders(status, response.length);

        try (var output = exchange.getResponseBody()) {
            output.write(response);
        }
    }
}
