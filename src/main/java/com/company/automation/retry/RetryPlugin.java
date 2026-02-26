package com.company.automation.retry;

import io.cucumber.core.eventbus.EventBus;
import io.cucumber.core.runtime.TimeServiceEventBus;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Result;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Logs retry activity in real-time.
 * Actual retry is driven by RetryAnalyzer + Hooks — see RetryAnalyzer javadoc.
 */
public class RetryPlugin implements ConcurrentEventListener {

    private static final Logger logger = LogManager.getLogger(RetryPlugin.class);

    private static final ConcurrentHashMap<String, AtomicInteger> counts =
            new ConcurrentHashMap<>();

    @Override
    public void setEventPublisher(EventPublisher publisher) {

        publisher.registerHandlerFor(TestCaseStarted.class, event -> {
            String name    = event.getTestCase().getName();
            int    attempt = counts.computeIfAbsent(name, k -> new AtomicInteger(0))
                    .incrementAndGet();
            int    total   = RetryAnalyzer.getTotalAllowedAttempts();

            if (attempt == 1) {
                logger.info("▶  [Attempt 1/{}] {}", total, name);
            } else {
                logger.warn("🔄 [Attempt {}/{}] RETRY — {}", attempt, total, name);
            }
        });

        publisher.registerHandlerFor(TestCaseFinished.class, event -> {
            String name   = event.getTestCase().getName();
            Status status = event.getResult().getStatus();
            int    total  = counts.getOrDefault(name, new AtomicInteger(1)).get();
            int    max    = RetryAnalyzer.getTotalAllowedAttempts();

            if (status == Status.PASSED && total > 1) {
                logger.info("✅ RECOVERED after {} attempt(s) — {}", total, name);
                counts.remove(name);
            } else if (status == Status.FAILED) {
                if (total < max) {
                    logger.warn("❌ FAILED (attempt {}/{}) — will retry: {}", total, max, name);
                } else {
                    logger.error("❌ FAILED PERMANENTLY after {} attempt(s) — {}", total, name);
                    counts.remove(name);
                }
            }
        });
    }
}