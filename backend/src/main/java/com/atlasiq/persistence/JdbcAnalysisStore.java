package com.atlasiq.persistence;
import com.atlasiq.qir.QirModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;

@ApplicationScoped
public class JdbcAnalysisStore implements AnalysisStore {
 @Inject DataSource dataSource;
 @Inject ObjectMapper mapper;

 @Override public StoredAnalysis save(QirModel model){
  String id=UUID.randomUUID().toString(); Instant now=Instant.now();
  try(Connection c=dataSource.getConnection(); PreparedStatement p=c.prepareStatement("INSERT INTO atlasiq_analysis(id,created_at,repository,ref_name,system_name,qir_json) VALUES(?,?,?,?,?,?)")){
   p.setString(1,id); p.setTimestamp(2,Timestamp.from(now)); p.setString(3,model.repository()); p.setString(4,model.ref());
   p.setString(5,model.scope()==null?null:model.scope().system()); p.setString(6,mapper.writeValueAsString(model)); p.executeUpdate();
   return new StoredAnalysis(id,now,model);
  }catch(Exception e){throw new IllegalStateException("unable to persist analysis",e);}
 }
 @Override public Optional<StoredAnalysis> find(String id){
  try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,created_at,qir_json FROM atlasiq_analysis WHERE id=?")){
   p.setString(1,id);try(ResultSet r=p.executeQuery()){return r.next()?Optional.of(read(r)):Optional.empty();}
  }catch(Exception e){throw new IllegalStateException("unable to read analysis",e);}
 }
 @Override public List<StoredAnalysis> recent(int limit){
  int safe=Math.max(1,Math.min(limit,100));
  try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement("SELECT id,created_at,qir_json FROM atlasiq_analysis ORDER BY created_at DESC")){
   p.setMaxRows(safe);try(ResultSet r=p.executeQuery()){List<StoredAnalysis> out=new ArrayList<>();while(r.next())out.add(read(r));return List.copyOf(out);}
  }catch(Exception e){throw new IllegalStateException("unable to list analyses",e);}
 }
 @Override public List<StoredAnalysis> history(String repository,String system,int limit){
  int safe=Math.max(1,Math.min(limit,200)); StringBuilder sql=new StringBuilder("SELECT id,created_at,qir_json FROM atlasiq_analysis WHERE 1=1");
  List<String> args=new ArrayList<>(); if(repository!=null&&!repository.isBlank()){sql.append(" AND repository=?");args.add(repository);}
  if(system!=null&&!system.isBlank()){sql.append(" AND system_name=?");args.add(system);} sql.append(" ORDER BY created_at DESC");
  try(Connection c=dataSource.getConnection();PreparedStatement p=c.prepareStatement(sql.toString())){
   for(int i=0;i<args.size();i++)p.setString(i+1,args.get(i));p.setMaxRows(safe);
   try(ResultSet r=p.executeQuery()){List<StoredAnalysis> out=new ArrayList<>();while(r.next())out.add(read(r));return List.copyOf(out);}
  }catch(Exception e){throw new IllegalStateException("unable to read analysis history",e);}
 }
 private StoredAnalysis read(ResultSet r)throws Exception{return new StoredAnalysis(r.getString(1),r.getTimestamp(2).toInstant(),mapper.readValue(r.getString(3),QirModel.class));}
}
