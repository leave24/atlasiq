package com.atlasiq.scanner;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GitHubRepositoryAcquirerTest {

    @Test
    void acceptsPublicGithubHttpsRepository() {
        var uri = GitHubRepositoryAcquirer.validatePublicGitHubUrl("https://github.com/leave24/infranauta");
        assertEquals("github.com", uri.getHost());
    }

    @Test
    void rejectsNonGithubAndUrlsWithCredentialsOrQuery() {
        assertThrows(IllegalArgumentException.class,
                () -> GitHubRepositoryAcquirer.validatePublicGitHubUrl("https://example.com/owner/repo"));
        assertThrows(IllegalArgumentException.class,
                () -> GitHubRepositoryAcquirer.validatePublicGitHubUrl("https://user@github.com/owner/repo"));
        assertThrows(IllegalArgumentException.class,
                () -> GitHubRepositoryAcquirer.validatePublicGitHubUrl("https://github.com/owner/repo?token=secret"));
    }

    @Test
    void validatesRefsWithoutPassingShellSyntax() {
        assertNull(GitHubRepositoryAcquirer.normalizeRef(""));
        assertEquals("main", GitHubRepositoryAcquirer.normalizeRef("main"));
        assertEquals("feature/demo", GitHubRepositoryAcquirer.normalizeRef("feature/demo"));
        assertThrows(IllegalArgumentException.class, () -> GitHubRepositoryAcquirer.normalizeRef("--upload-pack=x"));
        assertThrows(IllegalArgumentException.class, () -> GitHubRepositoryAcquirer.normalizeRef("../main"));
    }
}
