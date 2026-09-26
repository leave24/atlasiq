package com.atlasiq.pr;

import com.atlasiq.intelligence.ArchitectureDiff;
import com.atlasiq.intelligence.PrChangeImpact;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GitHubPrProviderTest {

    @Test
    void publishCheckUsesImpactScoreThreshold() throws Exception {
        GitHubPrProvider provider = new GitHubPrProvider();
        HttpClient client = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(201);
        when(response.body()).thenReturn("");

        List<String> payloads = new ArrayList<>();
        when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocation -> {
            payloads.add(body(invocation.getArgument(0, HttpRequest.class)));
            return response;
        });

        Field http = GitHubPrProvider.class.getDeclaredField("http");
        http.setAccessible(true);
        http.set(provider, client);

        PrProvider.PrTarget target = new PrProvider.PrTarget("github", "owner/repo", "1", "token");
        provider.publishCheck(target, report(69, List.of()));
        provider.publishCheck(target, report(70, List.of()));

        assertTrue(payloads.get(0).contains("\"conclusion\":\"success\""));
        assertTrue(payloads.get(1).contains("\"conclusion\":\"failure\""));
    }

    private static PrIntelligenceReport report(int score, List<PrIntelligenceReport.PolicyViolation> violations) {
        ArchitectureDiff diff = new ArchitectureDiff("a", "b", List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        PrChangeImpact impact = new PrChangeImpact("a", "b", "low", score, List.of("reason"), diff);
        return new PrIntelligenceReport("github", "owner/repo", "1", "base", "head", "a", "b", diff, impact, List.of(), List.of(), List.of(), violations, List.of());
    }

    private static String body(HttpRequest request) throws Exception {
        BodySubscriber subscriber = new BodySubscriber();
        request.bodyPublisher().orElseThrow().subscribe(subscriber);
        return subscriber.await();
    }

    private static final class BodySubscriber implements Flow.Subscriber<ByteBuffer> {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();
        private final CountDownLatch done = new CountDownLatch(1);

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            subscription.request(Long.MAX_VALUE);
        }

        @Override
        public void onNext(ByteBuffer item) {
            byte[] bytes = new byte[item.remaining()];
            item.get(bytes);
            output.write(bytes, 0, bytes.length);
        }

        @Override
        public void onError(Throwable throwable) {
            done.countDown();
            throw new RuntimeException(throwable);
        }

        @Override
        public void onComplete() {
            done.countDown();
        }

        private String await() throws InterruptedException {
            assertTrue(done.await(5, TimeUnit.SECONDS));
            return output.toString(StandardCharsets.UTF_8);
        }
    }
}
