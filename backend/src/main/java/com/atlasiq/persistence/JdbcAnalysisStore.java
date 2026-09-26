package com.atlasiq.persistence;
import com.atlasiq.qir.QirModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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

 @PostConstruct void migrate(){
  try(Connection c=dataSource.getConnection(); Statement s=c.createStatement()){
   s.executeUpdate("CREATE TABLE IF NOT EXISTS atlasiq_analysis (id VARCHAR(36) PRIMARY KEY, created_at TIMESTAMP NOT NULL, repository VARCHAR(1024) NOT NULL, ref_name VARCHAR(512), system_name VARCHAR(512), qir_json TEXT NOT NULL)");
   try{s.executeUpdate("CREATE INDEX idx_atlasiq_analysis_created ON atlasiq_analysis(created_at)");}catch(SQLException ignored){}
  }catch(SQLException e){throw new IllegalStateException("unable to initialize AtlasIQ persistence",e);}
 }
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
 private StoredAnalysis read(ResultSet r)throws Exception{return new StoredAnalysis(r.getString(1),r.getTimestamp(2).toInstant(),mapper.readValue(r.getString(3),QirModel.class));}
}
