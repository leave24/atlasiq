package com.atlasiq.enterprise;
import org.eclipse.microprofile.config.inject.ConfigProperty;
public class SamlSsoProvider implements SsoProvider {
 @ConfigProperty(name="atlasiq.sso.saml.sso-url",defaultValue="")String endpoint;
 public String protocol(){return "SAML2";}public String authorizationEndpoint(){return endpoint;}public boolean supportsSingleLogout(){return true;}
}
