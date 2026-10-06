package com.queueless.controller;

import com.queueless.util.AuthUtil;
import com.queueless.util.JsonUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet("/api/logout")
public class LogoutServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res) throws IOException {

        AuthUtil.logout(req, res);

        res.setContentType(
            "application/json;charset=UTF-8"
        );

        res.getWriter().write(
            JsonUtil.ok("Logged out.")
        );
    }
}