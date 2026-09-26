package com.atlasiq.enterprise;
import jakarta.enterprise.context.RequestScoped;
@RequestScoped public class TenantContext {private String organizationId;private String workspaceId;private String subject;public String organizationId(){return organizationId;}public String workspaceId(){return workspaceId;}public String subject(){return subject;}public void set(String o,String w,String s){organizationId=o;workspaceId=w;subject=s;}}
