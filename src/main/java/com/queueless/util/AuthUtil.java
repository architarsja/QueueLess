package com.queueless.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class AuthUtil {

    private static final String COOKIE_NAME = "QUEUELESS_AUTH";

    private AuthUtil() {}

    private static String secret() {
        String value = System.getenv("QUEUELESS_AUTH_SECRET");

        if (value == null || value.isBlank()) {
            value = System.getProperty("QUEUELESS_AUTH_SECRET");
        }

        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                "QUEUELESS_AUTH_SECRET is not configured."
            );
        }

        return value;
    }

    private static String sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");

        mac.init(new SecretKeySpec(
            secret().getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        ));

        byte[] signature = mac.doFinal(
            data.getBytes(StandardCharsets.UTF_8)
        );

        return Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(signature);
    }

    public static void login(
            HttpServletRequest req,
            HttpServletResponse res,
            long userId,
            String name,
            String role) throws Exception {

        long expires = (System.currentTimeMillis() / 1000L) + (8 * 60 * 60);

        String cleanName = name == null ? "" : name;
        String cleanRole = role == null ? "" : role;

        String payload =
            userId + "|" +
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    cleanName.getBytes(StandardCharsets.UTF_8)
                ) + "|" +
            cleanRole + "|" +
            expires;

        String signature = sign(payload);

        String token =
            Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                    payload.getBytes(StandardCharsets.UTF_8)
                )
                + "."
                + signature;

        boolean secure =
            req.isSecure()
            || "https".equalsIgnoreCase(
                req.getHeader("X-Forwarded-Proto")
            );

        StringBuilder cookie = new StringBuilder();

        cookie.append(COOKIE_NAME)
              .append("=")
              .append(token)
              .append("; Path=/")
              .append("; Max-Age=28800")
              .append("; HttpOnly")
              .append("; SameSite=Lax");

        if (secure) {
            cookie.append("; Secure");
        }

        res.addHeader("Set-Cookie", cookie.toString());
    }

    public static String require(
            HttpServletRequest req,
            HttpServletResponse res) throws IOException {

        if (userId(req) == null) {
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

            res.getWriter().write(
                JsonUtil.error("Please log in to continue.")
            );

            return null;
        }

        return "authenticated";
    }

    public static boolean staff(HttpServletRequest req) {
        return userId(req) != null;
    }

    public static String role(HttpServletRequest req) {
        try {
            String[] parts = getParts(req);

            if (parts == null) return null;

            return parts[2];

        } catch (Exception e) {
            return null;
        }
    }

    public static Long userId(HttpServletRequest req) {
        try {
            String[] parts = getParts(req);

            if (parts == null) return null;

            return Long.parseLong(parts[0]);

        } catch (Exception e) {
            return null;
        }
    }

    private static String[] getParts(HttpServletRequest req)
            throws Exception {

        Cookie[] cookies = req.getCookies();

        if (cookies == null) return null;

        String token = null;

        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                token = cookie.getValue();
                break;
            }
        }

        if (token == null || token.isBlank()) return null;

        String[] tokenParts = token.split("\\.");

        if (tokenParts.length != 2) return null;

        String encodedPayload = tokenParts[0];
        String receivedSignature = tokenParts[1];

        String payload = new String(
            Base64.getUrlDecoder().decode(encodedPayload),
            StandardCharsets.UTF_8
        );

        String expectedSignature = sign(payload);

        if (!constantTimeEquals(
                expectedSignature,
                receivedSignature)) {
            return null;
        }

        String[] parts = payload.split("\\|", -1);

        if (parts.length != 4) return null;

        long expires = Long.parseLong(parts[3]);

        if ((System.currentTimeMillis() / 1000L) > expires) {
            return null;
        }

        Long.parseLong(parts[0]);

        return parts;
    }

    private static boolean constantTimeEquals(
            String a,
            String b) {

        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);

        if (x.length != y.length) return false;

        int result = 0;

        for (int i = 0; i < x.length; i++) {
            result |= x[i] ^ y[i];
        }

        return result == 0;
    }

    public static void logout(
            HttpServletRequest req,
            HttpServletResponse res) {

        boolean secure =
            req.isSecure()
            || "https".equalsIgnoreCase(
                req.getHeader("X-Forwarded-Proto")
            );

        StringBuilder cookie = new StringBuilder();

        cookie.append(COOKIE_NAME)
              .append("=")
              .append("; Path=/")
              .append("; Max-Age=0")
              .append("; HttpOnly")
              .append("; SameSite=Lax");

        if (secure) {
            cookie.append("; Secure");
        }

        res.addHeader("Set-Cookie", cookie.toString());
    }
}