package com.queueless.dao;
import com.queueless.model.ServiceModel;
import com.queueless.util.DBConnection;
import java.sql.*; import java.util.*;
public class ServiceDAO {
 public List<ServiceModel> list(boolean activeOnly) throws SQLException { String q="SELECT * FROM services"+(activeOnly?" WHERE status='ACTIVE'":"")+" ORDER BY service_name"; List<ServiceModel> out=new ArrayList<>(); try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement(q);ResultSet r=p.executeQuery()){while(r.next())out.add(map(r));} return out; }
 public ServiceModel find(long id) throws SQLException { try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("SELECT * FROM services WHERE id=?")){p.setLong(1,id);try(ResultSet r=p.executeQuery()){return r.next()?map(r):null;}} }
 public long create(String name,String desc,String prefix,int avg) throws SQLException { try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("INSERT INTO services(service_name,description,prefix,average_service_time) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){p.setString(1,name);p.setString(2,desc);p.setString(3,prefix.toUpperCase());p.setInt(4,avg);p.executeUpdate();try(ResultSet r=p.getGeneratedKeys()){r.next();return r.getLong(1);}} }
 public void update(long id,String name,String desc,String prefix,int avg,String status) throws SQLException { try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE services SET service_name=?,description=?,prefix=?,average_service_time=?,status=? WHERE id=?")){p.setString(1,name);p.setString(2,desc);p.setString(3,prefix.toUpperCase());p.setInt(4,avg);p.setString(5,status);p.setLong(6,id);p.executeUpdate();} }
 public void deactivate(long id) throws SQLException { try(Connection c=DBConnection.getConnection();PreparedStatement p=c.prepareStatement("UPDATE services SET status='INACTIVE' WHERE id=?")){p.setLong(1,id);p.executeUpdate();} }
 private ServiceModel map(ResultSet r)throws SQLException{ServiceModel s=new ServiceModel();s.id=r.getLong("id");s.name=r.getString("service_name");s.description=r.getString("description");s.prefix=r.getString("prefix");s.averageTime=r.getInt("average_service_time");s.status=r.getString("status");return s;}
}
