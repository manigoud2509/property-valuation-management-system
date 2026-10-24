
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sun.net.httpserver.HttpExchange;

public class ViewProperties {

    public static void handle(HttpExchange exchange)
            throws IOException {

        StringBuilder html = new StringBuilder();

        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>View Properties</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: #f2f5f9;
                        padding: 25px;
                    }
                    h1 {
                        text-align: center;
                        color: #17365d;
                    }
                    .table-container {
                        overflow-x: auto;
                        background: white;
                        padding: 15px;
                        border-radius: 10px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                    }
                    th, td {
                        border: 1px solid #ddd;
                        padding: 10px;
                        text-align: left;
                    }
                    th {
                        background: #17365d;
                        color: white;
                    }
                    tr:nth-child(even) {
                        background: #f2f5f9;
                    }
                    a {
                        display: inline-block;
                        margin-bottom: 15px;
                        color: #17365d;
                    }
                    .message {
                        text-align: center;
                        padding: 15px;
                    }
                </style>
            </head>
            <body>
                <h1>Property Valuation Management System</h1>
                <p><a href="/addproperty.html">Add New Property</a></p>
                <div class="table-container">
                <table>
                    <tr>
                        <th>ID</th>
                        <th>Owner Name</th>
                        <th>Property Type</th>
                        <th>Location</th>
                        <th>Area</th>
                        <th>Rate / Sq Ft</th>
                        <th>Age</th>
                        <th>Estimated Value</th>
                    </tr>
            """);

        try (Connection con = DBConnect.getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT * FROM properties ORDER BY property_id DESC");
             ResultSet rs = ps.executeQuery()) {

            boolean found = false;

            while (rs.next()) {
                found = true;
                html.append("<tr>");
                html.append("<td>").append(rs.getInt("property_id")).append("</td>");
                html.append("<td>").append(escape(rs.getString("owner_name"))).append("</td>");
                html.append("<td>").append(escape(rs.getString("property_type"))).append("</td>");
                html.append("<td>").append(escape(rs.getString("location"))).append("</td>");
                html.append("<td>").append(rs.getBigDecimal("area")).append("</td>");
                html.append("<td>").append(rs.getBigDecimal("rate_per_sqft")).append("</td>");
                html.append("<td>").append(rs.getInt("property_age")).append("</td>");
                html.append("<td>").append(rs.getBigDecimal("estimated_value")).append("</td>");
                html.append("</tr>");
            }

            if (!found) {
                html.append("""
                    <tr><td colspan="8" class="message">
                    No properties found. Add your first property.
                    </td></tr>
                    """);
            }

        } catch (SQLException e) {
            html.append("""
                <tr><td colspan="8" class="message">
                Could not load properties from the database.
                Check the Java server terminal for details.
                </td></tr>
                """);
            e.printStackTrace();
        }

        html.append("""
                </table>
                </div>
                </body>
                </html>
            """);

        byte[] response = html.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type", "text/html; charset=UTF-8");

        exchange.sendResponseHeaders(200, response.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
