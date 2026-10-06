package com.queueless.controller;

import com.queueless.dao.UserDAO;
import com.queueless.util.AuthUtil;
import com.queueless.util.DBConnection;
import com.queueless.util.JsonUtil;
import com.queueless.util.PasswordUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;

@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res) throws IOException {

        res.setContentType("application/json;charset=UTF-8");

        String email = req.getParameter("email");
        String pass = req.getParameter("password");

        if (email == null ||
            pass == null ||
            email.isBlank() ||
            pass.isBlank()) {

            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            res.getWriter().write(
                JsonUtil.error(
                    "Email and password are required."
                )
            );

            return;
        }

        try (Connection c = DBConnection.getConnection()) {

            ResultSet r =
                new UserDAO().findForLogin(
                    c,
                    email.trim().toLowerCase()
                );

            if (!r.next() ||
                !PasswordUtil.matches(
                    pass,
                    r.getString("password")
                )) {

                res.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED
                );

                res.getWriter().write(
                    JsonUtil.error(
                        "Invalid email or password."
                    )
                );

                return;
            }

            long userId = r.getLong("id");
            String name = r.getString("name");
            String role = r.getString("role");

            AuthUtil.login(
                req,
                res,
                userId,
                name,
                role
            );

            res.getWriter().write(
                "{\"success\":true," +
                "\"name\":\"" +
                JsonUtil.esc(name) +
                "\"," +
                "\"role\":\"" +
                JsonUtil.esc(role) +
                "\"}"
            );

        } catch (Exception e) {

            e.printStackTrace();

            res.setStatus(
                HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            res.getWriter().write(
                JsonUtil.error(
                    "Unable to sign in right now."
                )
            );
        }
    }
}