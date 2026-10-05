package com.queueless.controller;
import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.*; import com.queueless.util.JsonUtil;
@WebServlet("/api/logout") public class LogoutServlet extends HttpServlet{protected void doPost(HttpServletRequest r,HttpServletResponse s)throws IOException{HttpSession x=r.getSession(false);if(x!=null)x.invalidate();s.setContentType("application/json");s.getWriter().write(JsonUtil.ok("Logged out."));}}
