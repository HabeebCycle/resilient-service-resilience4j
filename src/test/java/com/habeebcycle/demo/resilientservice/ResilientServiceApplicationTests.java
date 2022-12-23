package com.habeebcycle.demo.resilientservice;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.time.Duration;

import static com.habeebcycle.demo.resilientservice.util.Constant.CIRCUIT_BREAKER_CONFIG_NAME;
import static com.habeebcycle.demo.resilientservice.util.Constant.RETRY_CONFIG_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ResilientServiceApplicationTests {

	@Autowired private ApplicationContext context;
	@Autowired private CircuitBreakerRegistry circuitBreakerRegistry;
	@Autowired private RetryRegistry retryRegistry;

	private MockWebServer mockBackEnd;
	private WebTestClient testClient;
	private CircuitBreaker circuitBreaker;
	private Retry retry;

	@BeforeEach
	void setUpTestCase() throws IOException {
		this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_CONFIG_NAME);
		this.retry = retryRegistry.retry(RETRY_CONFIG_NAME);

		this.mockBackEnd = new MockWebServer();
		this.mockBackEnd.start(54500);
	}

	@AfterEach
	void tearDown() throws IOException {
		this.mockBackEnd.shutdown();
	}

	@Test
	void shouldARetryOnErrorAndFetchSuccessResponse() {
		testClient = WebTestClient
				.bindToApplicationContext(context)
				.configureClient().responseTimeout(Duration.ofSeconds(60))
				.build();

		mockBackEnd.enqueue(new MockResponse().setBody("ERROR").setResponseCode(500));
		mockBackEnd.enqueue(new MockResponse().setBody("ERROR").setResponseCode(500));
		mockBackEnd.enqueue(new MockResponse().setBody("{\"message\": \"success\"}").setResponseCode(200));

		testClient.get().uri("/services/posts/6")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.message").isEqualTo("success");

		assertThat(mockBackEnd.getRequestCount()).isEqualTo(3);
		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
	}

	@Test
	void shouldBRetryOnErrorAndBreakCircuitResponse() {
		testClient = WebTestClient
				.bindToApplicationContext(context)
				.configureClient().responseTimeout(Duration.ofSeconds(60))
				.build();

		mockBackEnd.enqueue(new MockResponse().setBody("ERROR").setResponseCode(500));
		mockBackEnd.enqueue(new MockResponse().setBody("ERROR").setResponseCode(500));
		mockBackEnd.enqueue(new MockResponse().setBody("ERROR").setResponseCode(500));
		mockBackEnd.enqueue(new MockResponse().setBody("{\"message\": \"success\"}").setResponseCode(200));

		testClient.get().uri("/services/posts/6")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().is5xxServerError()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.message").isEqualTo("ERROR")
				.jsonPath("$.statusCode").isEqualTo(500)
				.jsonPath("$.timestamp").isNotEmpty();

		assertThat(mockBackEnd.getRequestCount()).isEqualTo(3);

		await().atMost(Duration.ofMillis(10000));
		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
	}

	@Test
	void shouldCRetryAndFailAndBreakCloseTheCircuitTest() {
		testClient = WebTestClient
				.bindToApplicationContext(context)
				.configureClient().responseTimeout(Duration.ofSeconds(60))
				.build();

		mockBackEnd.enqueue(new MockResponse().setBody("ERROR").setResponseCode(500));
		mockBackEnd.enqueue(new MockResponse().setBody("ERROR").setResponseCode(500));
		mockBackEnd.enqueue(new MockResponse().setBody("ERROR").setResponseCode(500));
		mockBackEnd.enqueue(new MockResponse().setBody("ERROR").setResponseCode(500));
		mockBackEnd.enqueue(new MockResponse().setBody("{\"message\": \"success\"}").setResponseCode(200));

		testClient.get().uri("/services/posts/6")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().is5xxServerError()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.message").isEqualTo("ERROR")
				.jsonPath("$.statusCode").isEqualTo(500)
				.jsonPath("$.timestamp").isNotEmpty();

		await().atMost(Duration.ofMillis(10000));
		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

		testClient.get().uri("/services/posts/6")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().is5xxServerError()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.message").isEqualTo("API service is unavailable")
				.jsonPath("$.statusCode").isEqualTo(503)
				.jsonPath("$.timestamp").isNotEmpty();

		//Wait at least 10 seconds for the circuit to transition to half open state
		await().atMost(Duration.ofMillis(10000)).until(() -> circuitBreaker.getState().equals(CircuitBreaker.State.HALF_OPEN));
		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);

		testClient.get().uri("/services/posts/6")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(MediaType.APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.message").isEqualTo("success");

		assertThat(mockBackEnd.getRequestCount()).isEqualTo(5);
		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
	}

}
