package com.hisobchi.bot.keepalive;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@ConditionalOnProperty(name = "keep-alive.enabled", havingValue = "true", matchIfMissing = true)
public class KeepAliveService {

    private final RestClient restClient;
    private final String targetUrl;

    public KeepAliveService(
            RestClient restClient,
            @Value("${keep-alive.url:https://hisobchi-bot-4g83.onrender.com/actuator/health}") String targetUrl) {
        this.restClient = restClient;
        this.targetUrl = targetUrl;
    }

    /**
     * Send keep-alive HTTP ping every 9 minutes (540,000 ms) to keep the Render free instance awake.
     * Starts 2 minutes (120,000 ms) after application startup.
     */
    @Scheduled(fixedRate = 540000, initialDelay = 120000)
    public void pingSelf() {
        if (targetUrl == null || targetUrl.isBlank() || targetUrl.contains("localhost")) {
            log.debug("Keep-alive ping skipped for targetUrl: {}", targetUrl);
            return;
        }

        try {
            log.info("Sending keep-alive ping to {}", targetUrl);
            String response = restClient.get()
                    .uri(targetUrl)
                    .retrieve()
                    .body(String.class);
            log.info("Keep-alive ping successful to {}: response length={}",
                    targetUrl, response != null ? response.length() : 0);
        } catch (Exception e) {
            log.warn("Keep-alive ping to {} failed: {}", targetUrl, e.getMessage());
        }
    }
}
