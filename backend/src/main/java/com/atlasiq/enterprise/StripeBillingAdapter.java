package com.atlasiq.enterprise;
import jakarta.enterprise.context.ApplicationScoped;import org.eclipse.microprofile.config.inject.ConfigProperty;
@ApplicationScoped public class StripeBillingAdapter {private final String key;public StripeBillingAdapter(@ConfigProperty(name="atlasiq.stripe.secret-key",defaultValue="")String key){this.key=key;}public boolean configured(){return !key.isBlank();}public void requireConfigured(){if(!configured())throw new IllegalStateException("Stripe is not configured");}}
