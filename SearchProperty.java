
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.sun.net.httpserver.HttpExchange;

public class SearchProperty {

    public static void handle(HttpExchange exchange)
            throws IOException {

        String keyword = "";
        String html;

        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            String query = exchange.getRequestURI().getRawQuery();

            if (query != null) {
                for (String pair : query.split("&")) {
                    String[] parts = pair.split("=", 2);

                    if (parts[0].equals("keyword") && parts.length > 1) {
                        keyword = URLDecoder.decode(
                            parts[1], StandardCharsets.UTF_8);
                    }
                }
            }
        }

        StringBuilder page = new StringBuilder("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Search Property</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        background: #f2f5f9;
                        padding: 25px;
                    }
                    h1 { text-align: center; color: #17365d; }
                    .search-box {
                        background: white;
                        padding: 20px;
                        border-radius: 10px;
                        margin-bottom: 20px;
                    }
                    input, button {
                        padding: 10px;
                        font-size: 15px;
                    }
                    input { width: 60%; max-width: 400px; }
                    button {
                        background: #17365d;
                        color: white;
                        border: none;
                        cursor: pointer;
                    }
                    .table-box {
                        overflow-x: auto;
                        background: white;
                        padding: 12px;
                        border-radius: 10px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                    }
                    th, td {
                        border: 1px solid #ddd;
                        padding: 9px;
                        text-align: left;
                    }
                    th { background: #17365d; color: white; }
                    tr:nth-child(even) { background: #f2f5f9; }
                    a { color: #17365d; }
                </style>
            </head>
            <body>
                <h1>Search Property</h1>
                <p><a href="/">Home</a> |
                   <a href="/view-properties">View All Properties</a></p>
                <div class="search-box">
                    <form method="GET" action="/search-property">
                        <label for="keyword">
                            Enter owner name or location:
                        </label><br><br>
            """);

        page.append("<input id=\"keyword\" name=\"keyword\" value=\"")
            .append(escape(keyword))
            .append("\" placeholder=\"e.g. Rahul or Hyderabad\">")
            .append("<button type=\"submit\">Search</button>")
            .append("</form></div>");

        if (!keyword.isBlank()) {
            page.append("""
                <div class="table-box">
                <table>
                <tr>
                    <th>ID</th><th>Owner Name</th><th>Property Type</th>
                    <th>Location</th><th>Area</th><th>Rate / Sq Ft</th>
                    <th>Age</th><th>Estimated Value</th>
                </tr>
                """);

            String sql = """
                SELECT * FROM properties
                WHERE owner_name LIKE ? OR location LIKE ?
                ORDER BY property_id DESC
                """;

            try (Connection con = DBConnect.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql)) {

                String search = "%" + keyword + "%";
                ps.setString(1, search);
                ps.setString(2, search);

                try (ResultSet rs = ps.executeQuery()) {
                    boolean found = false;

                    while (rs.next()) {
                        found = true;
                        page.append("<tr>")
                            .append("<td>").append(rs.getInt("property_id")).append("</td>")
                            .append("<td>").append(escape(rs.getString("owner_name"))).append("</td>")
                            .append("<td>").append(escape(rs.getString("property_type"))).append("</td>")
                            .append("<td>").append(escape(rs.getString("location"))).append("</td>")
                            .append("<td>").append(rs.getBigDecimal("area")).append("</td>")
                            .append("<td>").append(rs.getBigDecimal("rate_per_sqft")).append("</td>")
                            .append("<td>").append(rs.getInt("property_age")).append("</td>")
                            .append("<td>").append(rs.getBigDecimal("estimated_value")).append("</td>")
                            .append("</tr>");
                    }

                    if (!found) {
                        page.append("""
                            <tr><td colspan="8">
                            No matching properties found.
                            </td></tr>
                            """);
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
                page.append("""
                    <p>Database error. Check the VS Code terminal.</p>
                    """);
            }

            page.append("</table></div>");
        }

        page.append("</body></html>");
        html = page.toString();

        byte[] response = html.getBytes(StandardCharsets.UTF_8);
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
