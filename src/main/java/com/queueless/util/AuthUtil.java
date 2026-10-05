package com.queueless.util;
import jakarta.servlet.http.*;
public final class AuthUtil {
    private AuthUtil() {}
    public static HttpSession require(HttpServletRequest req, HttpServletResponse res) throws java.io.IOException {
        HttpSession s=req.getSession(false);
        if(s==null || s.getAttribute("userId")==null){ res.setStatus(401); res.getWriter().write(com.queueless.util.JsonUtil.error("Please log in to continue.")); return null; }
        return s;
    }
    public static boolean staff(HttpServletRequest req){ HttpSession s=req.getSession(false); return s!=null && s.getAttribute("userId")!=null; }
    public static String role(HttpServletRequest req){ HttpSession s=req.getSession(false); return s==null?null:(String)s.getAttribute("role"); }
}
