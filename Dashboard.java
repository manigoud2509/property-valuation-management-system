
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sun.net.httpserver.HttpExchange;

public class Dashboard {

    public static void handle(HttpExchange exchange)
            throws IOException {

        // Check whether the user is logged in.
        if (!SessionManager.isLoggedIn(exchange)) {
            exchange.getResponseHeaders().set(
                "Location", "/login.html"
            );
            exchange.sendResponseHeaders(302, -1);
            return;
        }

        // Get property statistics.
        int totalProperties = 0;
        int totalLocations = 0;
        double totalValue = 0;
        double averageValue = 0;

        try (Connection con = DBConnect.getConnection()) {

            String sql = "SELECT COUNT(*) AS total_properties, " +
                    "COALESCE(SUM(estimated_value), 0) AS total_value, " +
                    "COALESCE(AVG(estimated_value), 0) AS average_value, " +
                    "COUNT(DISTINCT location) AS total_locations " +
                    "FROM properties";

            try (PreparedStatement ps =
                     con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    totalProperties =
                        rs.getInt("total_properties");
                    totalValue =
                        rs.getDouble("total_value");
                    averageValue =
                        rs.getDouble("average_value");
                    totalLocations =
                        rs.getInt("total_locations");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            send(exchange, 500,
                "<h2>Database Error</h2>" +
                "<p>Check your MySQL connection and properties table.</p>");
            return;
        }

        // Display dashboard.
        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport"
                      content="width=device-width, initial-scale=1.0">

                <title>Property Dashboard</title>

                <style>
                    body {
                        margin: 0;
                        font-family: Arial, sans-serif;
                        background: #f2f5f9;
                        color: #263238;
                    }

                    header {
                        background: #17365d;
                        color: white;
                        text-align: center;
                        padding: 25px 10px;
                    }

                    nav {
                        background: #102742;
                        padding: 15px;
                        text-align: center;
                    }

                    nav a {
                        color: white;
                        text-decoration: none;
                        margin: 8px;
                        display: inline-block;
                    }

                    main {
                        max-width: 1000px;
                        margin: 30px auto;
                        padding: 15px;
                    }

                    .cards {
                        display: grid;
                        grid-template-columns:
                            repeat(auto-fit, minmax(210px, 1fr));
                        gap: 20px;
                    }

                    .card {
                        background: white;
                        padding: 25px;
                        border-radius: 8px;
                        border-left: 5px solid #24558b;
                        box-shadow: 0 3px 8px #ddd;
                    }

                    .card h3 {
                        color: #526579;
                        font-size: 16px;
                    }

                    .number {
                        color: #17365d;
                        font-size: 25px;
                        font-weight: bold;
                        overflow-wrap: anywhere;
                    }

                    .button {
                        display: inline-block;
                        background: #24558b;
                        color: white;
                        text-decoration: none;
                        padding: 12px 18px;
                        margin: 10px 5px 0 0;
                        border-radius: 5px;
                        border: none;
                        cursor: pointer;
                        font-size: 15px;
                    }

                    .logout-button {
                        background: #c62828;
                    }

                    .button:hover {
                        opacity: 0.85;
                    }

                    .logout-form {
                        display: inline-block;
                        margin: 0;
                    }

                    footer {
                        margin-top: 40px;
                        padding: 18px;
                        background: #102742;
                        color: white;
                        text-align: center;
                    }
                </style>
            </head>

            <body>
                <header>
                    <h1>Property Valuation Management System</h1>
                    <p>Dashboard</p>
                </header>

                <nav>
                    <a href="/">Home</a>
                    <a href="/addproperty.html">Add Property</a>
                    <a href="/searchproperty.html">Search Property</a>
                    <a href="/view-properties">View Properties</a>
                    <a href="/editproperty.html">Edit Property</a>
                    <a href="/deleteproperty.html">Delete Property</a>
                </nav>

                <main>
                    <h2>Property Statistics</h2>
                    <p>Summary of records stored in the database.</p>

                    <div class="cards">
                        <div class="card">
                            <h3>Total Properties</h3>
                            <div class="number">%d</div>
                        </div>

                        <div class="card">
                            <h3>Total Estimated Value</h3>
                            <div class="number">₹ %,.2f</div>
                        </div>

                        <div class="card">
                            <h3>Average Property Value</h3>
                            <div class="number">₹ %,.2f</div>
                        </div>

                        <div class="card">
                            <h3>Different Locations</h3>
                            <div class="number">%d</div>
                        </div>
                    </div>

                    <p>
                        <a class="button" href="/addproperty.html">
                            Add New Property
                        </a>

                        <a class="button" href="/view-properties">
                            View All Properties
                        </a>

                        <a class="button" href="/">
                            Back to Home
                        </a>

                        <form class="logout-form"
                              action="/logout"
                              method="post"
                              onsubmit="return confirm('Are you sure you want to logout?');">
                            <button type="submit"
                                    class="button logout-button">
                                Logout
                            </button>
                        </form>
                    </p>
                </main>

                <footer>
                    <p>Property Valuation Management System</p>
                    <p>B.Com Mini Project</p>
                </footer>
            </body>
            </html>
            """.formatted(
                totalProperties,
                totalValue,
                averageValue,
                totalLocations
            );

        send(exchange, 200, html);
    }

    private static void send(
            HttpExchange exchange,
            int status,
            String html) throws IOException {

        byte[] response =
            html.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type",
            "text/html; charset=UTF-8"
        );

        exchange.sendResponseHeaders(
            status,
            response.length
        );

        try (OutputStream os =
                 exchange.getResponseBody()) {
            os.write(response);
        }
    }
}
