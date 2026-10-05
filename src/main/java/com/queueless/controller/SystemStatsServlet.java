package com.queueless.controller;
import com.queueless.dao.QueueDAO; import com.queueless.util.*; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.*; import java.sql.*;
@WebServlet("/api/system-stats") public class SystemStatsServlet extends HttpServlet{
 protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException{if(AuthUtil.require(req,res)==null)return;try{long customers=new QueueDAO().totalCustomers();long services=0;try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM services");ResultSet r=p.executeQuery()){r.next();services=r.getLong(1);}long entries=customers;res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"success\":true,\"customers\":"+customers+",\"services\":"+services+",\"queueEntries\":"+entries+"}");}catch(Exception e){res.setStatus(500);res.getWriter().write(JsonUtil.error("Unable to load system overview."));}}
}
