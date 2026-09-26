package com.atlasiq.enterprise;
public interface SsoProvider {String protocol();String authorizationEndpoint();boolean supportsSingleLogout();}
