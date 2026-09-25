package com.atlasiq.scanner;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class GitHubRepositoryAuthenticationTest {

    @Test
    void authorizationHeaderUsesGitHubTokenWithoutEmbeddingItInUrl() throws Exception {
        Method method = GitHubRepositoryAcquirer.class.getDeclaredMethod("basicAuthorization", String.class);
        method.setAccessible(true);
        String header = (String) method.invoke(null, "secret-token");
        assertTrue(header.startsWith("AUTHORIZATION: basic "));
        String encoded = header.substring("AUTHORIZATION: basic ".length());
        assertEquals("x-access-token:secret-token", new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8));
        assertFalse(header.contains("https://"));
    }

    @Test
    void repositoryUrlStillRejectsEmbeddedCredentials() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> GitHubRepositoryAcquirer.validatePublicGitHubUrl("https://token@github.com/acme/private-repo"));
        assertTrue(error.getMessage().contains("only https://github.com"));
    }
}
