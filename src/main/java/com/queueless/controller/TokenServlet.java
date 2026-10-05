package com.queueless.controller;
import com.queueless.dao.QueueDAO; import com.queueless.model.QueueEntry; import jakarta.servlet.annotation.WebServlet; import jakarta.servlet.http.*; import java.io.*;
@WebServlet("/token") public class TokenServlet extends HttpServlet{
 protected void doGet(HttpServletRequest req,HttpServletResponse res)throws IOException{try{String token=req.getParameter("token");QueueEntry q=new QueueDAO().findByToken(token==null?"":token.trim().toUpperCase());if(q==null){res.sendError(404,"Token not found");return;}req.setAttribute("entry",q);req.getRequestDispatcher("/token.jsp").forward(req,res);}catch(Exception e){res.sendError(500,"Unable to load token");}}
}
