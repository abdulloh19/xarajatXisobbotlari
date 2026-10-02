package com.hisobchi.bot.keepalive;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeepAliveServiceTest {

    @Mock
    private RestClient restClient;

    @Test
    @DisplayName("pingSelf should skip ping when URL is null, blank, or contains localhost")
    void testPingSkippedForLocalhost() {
        KeepAliveService service = new KeepAliveService(restClient, "http://localhost:10000/actuator/health");
        service.pingSelf();
        verifyNoInteractions(restClient);
    }
}
