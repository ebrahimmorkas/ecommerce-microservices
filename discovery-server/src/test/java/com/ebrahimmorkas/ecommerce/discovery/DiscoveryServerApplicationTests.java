package com.ebrahimmorkas.ecommerce.discovery;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void eurekaDashboardAndHealthEndpointAreAvailable() {
        assertThat(restTemplate.getForEntity("/actuator/health", String.class).getBody()).contains("UP");
        assertThat(restTemplate.getForEntity("/eureka/apps", String.class).getStatusCode().is2xxSuccessful()).isTrue();
    }
}
