
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

public class PropertyServer {

    public static void main(String[] args) throws IOException {

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0
        );

        server.createContext("/save-property", SaveProperty::handle);
        server.createContext("/search-property", SearchProperty::handle);
        server.createContext("/delete-property", DeleteProperty::handle);
        server.createContext("/view-properties", ViewProperties::handle);
        server.createContext("/edit-property", EditProperty::handle);
        server.createContext("/dashboard", Dashboard::handle);
        server.createContext("/login", Login::handle);

        // Logout route
        server.createContext("/logout", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "POST");
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            SessionManager.logout(exchange);

            exchange.getResponseHeaders().set(
                "Set-Cookie",
                "PVMS_SESSION=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax"
            );

            exchange.getResponseHeaders().set(
                "Location", "/login.html"
            );

            exchange.sendResponseHeaders(303, -1);
        });

        server.createContext("/", PropertyServer::servePage);

        server.setExecutor(null);
        server.start();

        System.out.println("Property Valuation Management System started!");
        System.out.println("Home Page: http://localhost:8080/");
        System.out.println("Login Page: http://localhost:8080/login.html");
        System.out.println("Dashboard: http://localhost:8080/dashboard");
        System.out.println("Add Property: http://localhost:8080/addproperty.html");
    }

    private static void servePage(HttpExchange exchange)
            throws IOException {

        String path = exchange.getRequestURI().getPath();

        if (path.equals("/")) {
            path = "/index.html";
        }

        // Serve only simple filenames from the project folder.
        String filename = path.substring(1);

        if (filename.isEmpty()
                || filename.contains("/")
                || filename.contains("\\")
                || filename.contains("..")) {
            send(exchange, 400, "Invalid page request");
            return;
        }

        Path file = Path.of(filename);

        if (!Files.isRegularFile(file)) {
            send(exchange, 404, "Page not found");
            return;
        }

        String contentType = filename.endsWith(".css")
                ? "text/css; charset=UTF-8"
                : "text/html; charset=UTF-8";

        byte[] data = Files.readAllBytes(file);

        exchange.getResponseHeaders().set(
            "Content-Type", contentType
        );

        exchange.sendResponseHeaders(200, data.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(data);
        }
    }

    private static void send(
            HttpExchange exchange,
            int status,
            String message) throws IOException {

        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "text/plain; charset=UTF-8"
        );

        exchange.sendResponseHeaders(status, data.length);

        try (OutputStream output = exchange.getResponseBody()) {
            output.write(data);
        }
    }
}@