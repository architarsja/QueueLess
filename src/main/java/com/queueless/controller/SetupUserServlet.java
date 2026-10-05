package com.queueless.controller;

import com.queueless.dao.UserDAO;
import com.queueless.util.JsonUtil;
import com.queueless.util.PasswordUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@WebServlet("/api/setup-user")
public class SetupUserServlet extends HttpServlet {

    private String getSetupKey() {

        // 1. Try Windows environment variable
        String key = System.getenv("QUEUELESS_SETUP_KEY");

        if (key != null && !key.isBlank()) {
            return key;
        }

        // 2. Try Java system property
        key = System.getProperty("QUEUELESS_SETUP_KEY");

        if (key != null && !key.isBlank()) {
            return key;
        }

        // 3. Try db.properties
        Properties properties = new Properties();

        try (InputStream input =
                     getClass().getClassLoader().getResourceAsStream("db.properties")) {

            if (input != null) {
                properties.load(input);

                key = properties.getProperty("setup.key");

                if (key != null && !key.isBlank()) {
                    return key;
                }
            }

        } catch (Exception ignored) {
        }

        return null;
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse res) throws IOException {

        res.setContentType("application/json;charset=UTF-8");

        try {

            String key = req.getParameter("setupKey");
            String expected = getSetupKey();

            if (expected == null ||
                expected.isBlank() ||
                key == null ||
                !expected.equals(key)) {

                res.setStatus(HttpServletResponse.SC_FORBIDDEN);

                res.getWriter().write(
                    JsonUtil.error(
                        "Account setup is disabled or the setup key is invalid."
                    )
                );

                return;
            }

            String name = req.getParameter("name");
            String email = req.getParameter("email");
            String phone = req.getParameter("phone");
            String pass = req.getParameter("password");
            String role = req.getParameter("role");

            if (name == null ||
                email == null ||
                pass == null ||
                name.isBlank() ||
                !email.contains("@") ||
                pass.length() < 8) {

                throw new IllegalArgumentException(
                    "Name, valid email and password of at least 8 characters are required."
                );
            }

            if (!"ADMIN".equals(role) && !"STAFF".equals(role)) {
                role = "STAFF";
            }

            UserDAO dao = new UserDAO();

            String cleanEmail = email.trim().toLowerCase();

            if (dao.existsEmail(cleanEmail)) {

                throw new IllegalArgumentException(
                    "An account with this email already exists."
                );
            }

            dao.create(
                name.trim(),
                cleanEmail,
                phone,
                PasswordUtil.hash(pass),
                role
            );

            res.getWriter().write(
                JsonUtil.ok("Account created successfully.")
            );

        } catch (Exception e) {

            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            res.getWriter().write(
                JsonUtil.error(
                    e.getMessage() == null
                        ? "Could not create account."
                        : e.getMessage()
                )
            );
        }
    }
}