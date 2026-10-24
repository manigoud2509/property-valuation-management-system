
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

public class EditProperty {

    public static void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            send(exchange, 405, "POST method required.");
            return;
        }

        Map<String, String> data = new HashMap<>();

        try {
            String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8
                );
                String value = parts.length > 1
                    ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                    : "";
                data.put(key, value.trim());
            }

            int id = Integer.parseInt(data.getOrDefault("property_id", ""));
            String owner = data.getOrDefault("owner_name", "");
            String type = data.getOrDefault("property_type", "");
            String location = data.getOrDefault("location", "");
            double area = Double.parseDouble(data.getOrDefault("area", ""));
            double rate = Double.parseDouble(data.getOrDefault("rate_per_sqft", ""));
            int age = Integer.parseInt(data.getOrDefault("property_age", ""));

            if (id <= 0 || owner.isBlank() || type.isBlank()
                    || location.isBlank() || area <= 0
                    || rate <= 0 || age < 0
                    || !Double.isFinite(area) || !Double.isFinite(rate)) {
                send(exchange, 400,
                    "Invalid input. Check all fields and try again.");
                return;
            }

            try (Connection con = DBConnect.getConnection()) {
                String sql = """
                    UPDATE properties
                    SET owner_name = ?, property_type = ?, location = ?,
                        area = ?, rate_per_sqft = ?, property_age = ?,
                        estimated_value = ?
                    WHERE property_id = ?
                    """;

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, owner);
                    ps.setString(2, type);
                    ps.setString(3, location);
                    ps.setDouble(4, area);
                    ps.setDouble(5, rate);
                    ps.setInt(6, age);
                    ps.setDouble(7, area * rate);
                    ps.setInt(8, id);

                    int updated = ps.executeUpdate();

                    if (updated == 0) {
                        send(exchange, 404,
                            "Property ID " + id + " was not found.");
                    } else {
                        send(exchange, 200,
                            "Property updated successfully! Estimated value: "
                            + String.format("%.2f", area * rate));
                    }
                }
            }
        } catch (NumberFormatException e) {
            send(exchange, 400,
                "Enter a valid Property ID, area, rate, and age.");
        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500,
                "Database error. Check the VS Code server terminal.");
        }
    }

    private static void send(
            HttpExchange exchange, int status, String message)
            throws IOException {
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
