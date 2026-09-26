package com.atlasiq.enterprise;
import jakarta.enterprise.context.ApplicationScoped;import org.eclipse.microprofile.config.inject.ConfigProperty;
@ApplicationScoped public class OidcSsoProvider implements SsoProvider {
 @ConfigProperty(name="atlasiq.sso.oidc.authorization-endpoint",defaultValue="")String endpoint;
 public String protocol(){return "OIDC";}public String authorizationEndpoint(){return endpoint;}public boolean supportsSingleLogout(){return true;}
}
