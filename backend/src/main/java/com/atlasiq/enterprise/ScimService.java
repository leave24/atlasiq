package com.atlasiq.enterprise;
import jakarta.enterprise.context.ApplicationScoped;import java.util.*;
@ApplicationScoped public class ScimService {private final OrganizationService orgs;public ScimService(OrganizationService o){orgs=o;}public void provisionMembership(String org,String workspace,String subject,String role){orgs.addMembership(org,workspace,subject,role);}public record ScimUser(String id,String userName,boolean active,List<String> roles){}}
