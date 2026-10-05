package com.queueless.dao;
import com.queueless.util.DBConnection; import java.sql.*;
public class UserDAO {
 public ResultSet findForLogin(Connection c,String email)throws SQLException{PreparedStatement p=c.prepareStatement("SELECT id,name,email,password,role FROM users WHERE email=?");p.setString(1,email);return p.executeQuery();}
 public boolean existsEmail(String email)throws SQLException{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT 1 FROM users WHERE email=?")){p.setString(1,email);try(ResultSet r=p.executeQuery()){return r.next();}}}
 public void create(String name,String email,String phone,String password,String role)throws SQLException{try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO users(name,email,phone,password,role) VALUES(?,?,?,?,?)")){p.setString(1,name);p.setString(2,email);p.setString(3,phone);p.setString(4,password);p.setString(5,role);p.executeUpdate();}}
}
