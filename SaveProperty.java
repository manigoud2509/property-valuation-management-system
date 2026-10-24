
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

public class SaveProperty {

    public static void handle(HttpExchange exchange)
            throws IOException {

        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            send(exchange, 405, "POST method required.");
            return;
        }

        try {
            String body = new String(
                exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8
            );

            Map<String, String> data = new HashMap<>();

            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);

                String key = URLDecoder.decode(
                    parts[0], StandardCharsets.UTF_8
                );

                String value = parts.length > 1
                    ? URLDecoder.decode(
                        parts[1], StandardCharsets.UTF_8
                    )
                    : "";

                data.put(key, value);
            }

            String owner = data.getOrDefault("owner_name", "").trim();
            String type = data.getOrDefault("property_type", "").trim();
            String location = data.getOrDefault("location", "").trim();

            if (owner.isEmpty() || type.isEmpty() || location.isEmpty()) {
                send(exchange, 400,
                    "Owner name, property type and location are required.");
                return;
            }

            String areaText = data.getOrDefault("area", "").trim();
            String rateText = data.getOrDefault("rate_per_sqft", "").trim();
            String ageText = data.getOrDefault("property_age", "").trim();

            if (areaText.isEmpty() || rateText.isEmpty()
                    || ageText.isEmpty()) {
                send(exchange, 400,
                    "Area, rate and property age are required.");
                return;
            }

            double area;
            double rate;
            int age;

            try {
                area = Double.parseDouble(areaText);
                rate = Double.parseDouble(rateText);
                age = Integer.parseInt(ageText);
            } catch (NumberFormatException e) {
                send(exchange, 400,
                    "Enter valid numbers for area, rate and property age.");
                return;
            }

            if (!Double.isFinite(area) || !Double.isFinite(rate)
                    || area <= 0 || rate <= 0 || age < 0) {
                send(exchange, 400,
                    "Area and rate must be positive. Age cannot be negative.");
                return;
            }

            // Estimated value = area × rate per square foot
            double estimatedValue = area * rate;

            if (!Double.isFinite(estimatedValue)) {
                send(exchange, 400, "The calculated value is too large.");
                return;
            }

            String sql = """
                INSERT INTO properties
                (owner_name, property_type, location, area,
                 rate_per_sqft, property_age, estimated_value)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

            try (Connection con = DBConnect.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                ps.setString(1, owner);
                ps.setString(2, type);
                ps.setString(3, location);
                ps.setDouble(4, area);
                ps.setDouble(5, rate);
                ps.setInt(6, age);
                ps.setBigDecimal(7,
                    java.math.BigDecimal.valueOf(estimatedValue));

                int rows = ps.executeUpdate();

                if (rows == 1) {
                    send(exchange, 200,
                        "Property saved successfully. Estimated value: ₹"
                        + String.format("%.2f", estimatedValue));
                } else {
                    send(exchange, 500, "Property was not saved.");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500,
                "Database error. Check the Java server terminal.");
        } catch (Exception e) {
            e.printStackTrace();
            send(exchange, 500, "Unable to save property.");
        }
    }

    private static void send(
            HttpExchange exchange, int status, String message)
            throws IOException {

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
