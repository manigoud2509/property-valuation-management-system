
import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.sun.net.httpserver.HttpExchange;

public class SessionManager {

    private static final ConcurrentMap<String, Boolean> sessions =
            new ConcurrentHashMap<>();

    private static final SecureRandom random = new SecureRandom();

    // Create a session after successful login.
    public static String createSession() {
        byte[] token = new byte[32];
        random.nextBytes(token);

        String sessionId = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(token);

        sessions.put(sessionId, true);
        return sessionId;
    }

    // Check whether the browser has a valid session.
    public static boolean isLoggedIn(HttpExchange exchange) {
        String cookieHeader =
                exchange.getRequestHeaders().getFirst("Cookie");

        if (cookieHeader == null) {
            return false;
        }

        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=", 2);

            if (parts.length == 2
                    && parts[0].equals("PVMS_SESSION")) {
                return sessions.containsKey(parts[1]);
            }
        }

        return false;
    }

    // Remove the session when the user logs out.
    public static void logout(HttpExchange exchange) {
        String cookieHeader =
                exchange.getRequestHeaders().getFirst("Cookie");

        if (cookieHeader == null) {
            return;
        }

        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=", 2);

            if (parts.length == 2
                    && parts[0].equals("PVMS_SESSION")) {
                sessions.remove(parts[1]);
                return;
            }
        }
    }
}
