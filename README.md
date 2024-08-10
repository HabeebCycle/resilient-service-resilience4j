# Spring Boot Webflux Resilient Service with Resilience4j - Retry & CircuitBreaker with router functions
This demonstrates how to implement resilient service in Spring Boot Webflux using the popular fault tolerance library Resilience4j.
Resilience4j provides higher-order functions (decorators) to enhance any functional interface, lambda expression, or method reference with a Circuit Breaker, Rate Limiter, Retry, or Bulkhead. All these can be used together within a project, class, and even on a single method. Refer to this link for more information.

### This demonstration will use the Resilience4j Retry and Circuit Breaker to implement the resilient service with Spring Boot Webflux router predicates and functions. For annotation based, there are so many examples on the internet, this demo will only focus on the router predicates and router functions reactive approach. The full source code for this demo can be accessed from this GitHub repository https://github.com/HabeebCycle/resilient-service-resilience4j

## Getting Started…
With a simple Spring Boot maven project called resilient-service using Spring Webflux with some other dependencies as shown in the following pom.xml snippet:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
 <modelVersion>4.0.0</modelVersion>
 <parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>3.0.0</version>
  <relativePath/> <!-- lookup parent from repository -->
 </parent>
 <groupId>com.habeebcycle.demo</groupId>
 <artifactId>resilient-service</artifactId>
 <version>0.0.1-SNAPSHOT</version>
 <name>resilient-service</name>
 <description>Resilient Service (Retry-Circuit Breaker) with Spring Boot using Resilience4J</description>
 <properties>
  <java.version>17</java.version>
 </properties>
 <dependencies>
  <dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
  <dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-aop</artifactId>
  </dependency>
  <dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-webflux</artifactId>
  </dependency>

  <dependency>
   <groupId>io.github.resilience4j</groupId>
   <artifactId>resilience4j-spring-boot3</artifactId>
   <version>2.0.2</version>
  </dependency>
  <dependency>
   <groupId>io.github.resilience4j</groupId>
   <artifactId>resilience4j-reactor</artifactId>
   <version>2.0.2</version>
  </dependency>

  <dependency>
   <groupId>org.springframework.boot</groupId>
   <artifactId>spring-boot-starter-test</artifactId>
   <scope>test</scope>
  </dependency>
  <dependency>
   <groupId>io.projectreactor</groupId>
   <artifactId>reactor-test</artifactId>
   <scope>test</scope>
  </dependency>

  <dependency>
   <groupId>org.awaitility</groupId>
   <artifactId>awaitility</artifactId>
   <version>4.2.0</version>
   <scope>test</scope>
  </dependency>
  <dependency>
   <groupId>com.squareup.okhttp3</groupId>
   <artifactId>mockwebserver</artifactId>
   <version>4.10.0</version>
   <scope>test</scope>
  </dependency>
 </dependencies>

 <build>
  <plugins>
   <plugin>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-maven-plugin</artifactId>
   </plugin>
  </plugins>
 </build>

</project>
```

The project used Spring Boot version 3.0.0, Java 17, and Resilience4j version 2.0.2. Note, Spring Boot version 3 requires Java 17+. Spring Boot less than version 3.0.0 can also be used with Java 11+ and Resilience4j 1.7.
The first two dependencies (actuator and aop) are required for resilience-4j metrics recording, health status, and aspect orientation.
Resilience4j for Spring Boot 3 requires Spring Boot (resilience4j-spring-boot3) dependency and reactor (resilience4j-reactor) if using Spring Webflux (project reactor). Since the project is using Webflux, resilience4j-reactor dependency is required.
Other dependencies are for testing purposes. SpringBootTest and Reactor (spring-boot-starter-test and reactor-test) are used to perform the unit test cases. Awaitility (awaitility) is used to await the thread to test the circuit-breaker transition from OPEN state to HALF-OPEN state. Mockwebserver (mockwebserver) is used to mock the third-party API in order to test how the Retry and CircuitBreaker react to the failure responses.
Resilience4j config - properties file or Java config
Setting up the configurations for Resilience4j Retry and CircuitBreaker in a Spring Boot project can be done in two ways - setting the properties in aapplication.properties/yaml file or bootstrapping it in a Spring Boot Java configuration bean. The authors recommend that we always go with properties file configuration unless we are doing a customized configuration in our project. If you have both ways configured in a single project with the same instance name, the properties file config takes precedence over that of the Java config. Let's see how it can be set up either way.

### A. Retry & CircuitBreaker - application properties file configuration
```yaml
# Enable actuator health endpoints
management.endpoint.health.probes.enabled: true
management.endpoint.health.show-details: always

# Resilience4J properties
# #Enable circuit breaker health status
management.health.circuitbreakers.enabled: true
resilience4j:
  
  # Retry
  retry:
    retryAspectOrder: 2
    instances:
      testService:
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        maxAttempts: 3
        waitDuration: 5s
        retryExceptionPredicate: com.habeebcycle.demo.resilientservice.http.exception.RecordFailurePredicate

  # Circuitbreaker
  circuitbreaker:
    circuitBreakerAspectOrder: 1
    instances:
      testService:
        registerHealthIndicator: true
        slidingWindowSize: 10
        minimumNumberOfCalls: 4
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
        permittedNumberOfCallsInHalfOpenState: 2
        automaticTransitionFromOpenToHalfOpenEnabled: true
        recordFailurePredicate: com.habeebcycle.demo.resilientservice.http.exception.RecordFailurePredicate
```

In the file above, Resilience4j Retry and CircuitBreaker configuration properties were specified. Most of these properties' detailed definitions and what they do can be found on the Resilience4j getting started page.
The first two properties enable the actuator health endpoints to be available with their details and can be accessed via the /actuator/healthendpoint. The third property integrates the CircuitBreaker info such as the status, state, thresholds, etc. into the actuator health info response as shown below
circuit-breakers health status from /actuator/health endpointThe other properties are the specific subjective properties for both the retry mechanism and circuit-breaker configurations. In the above properties file, testService is used as the instance name for both configurations and that will be the name used throughout the demo to reference the single instance of the retry and circuit breaker. If no name is specified, a default name will automatically be registered. We can also have more than one instance with different names and different configurations or even inherit from each other or from the "default" configurations. These properties are subjective and are based on business logic. We need to use whatever values that work for our business design. Read more on https://resilience4j.readme.io/docs under the 'CORE MODULES'.
Retry Properties - enabled the exponential backoff, set the back-off multiplier to 2, and the wait duration to 5s, which allows the retry mechanism to back off for a wait duration of 5 seconds with a multiplier factor of 2 before retrying again for a maximum retry of 3. As specified on the retry exception predicate, a retry will only be performed when any of the exceptions defined on the predicate occurs. Full details of these properties and other properties not specified in this file are available at https://resilience4j.readme.io/docs/retry
CircuitBreaker Properties - register the instance "testService" as part of the circuit breaker health indicator or status. Using the sliding window size of 10, which is used to record the outcome of calls when the circuit breaker is closed. A minimum number of calls of 4, which are required (per sliding window period) before the CircuitBreaker can calculate the error rate or slow call rate. A failure rate threshold of 50%, when the failure rate is equal to or greater than the threshold (in this case 50% of 4 = 2), the CircuitBreaker transitions to open and starts short-circuiting calls. Other properties are the wait duration in the open state before automatically transitioning into a half-open state, and the number of calls to allow in the half-open state. As specified on the record failure predicate, the circuit breaker will only be opened when any of the exceptions/errors defined on the predicate are recorded and equal to or greater than the threshold. Full details of these properties and other properties not specified are available at https://resilience4j.readme.io/docs/circuitbreaker
Aspect Order- as specified on the docs, retry has a lower priority than the circuit breaker, so retry will be applied last. But in this demo, the circuit breaker needs to start after the retry finishes its work, that is why retryAspectOrder property is set to have a greater value than circuitBreakerAspectOrder (the higher value = the higher priority).
A retryExceptionPredicate and recordFailurePredicate - These two properties indicate a predicate when failure should be marked as a candidate for retry or counted towards the circuit breaker threshold. In this demo, a custom-defined predicate class RecordFailurePredicate is used. Other ways to record failure or indicate retry or ignore exceptions are available on the docs.

### B. Retry & CircuitBreaker - Java config file configuration
```java
@Configuration
public class ResilienceConfig {

    private static final String CIRCUIT_BREAKER_CONFIG_NAME = "testService";
    private static final String RETRY_CONFIG_NAME = "testService";

    @Bean
    public CircuitBreakerRegistry configureCircuitBreakerRegistry() {
        final CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                //.slidingWindow(10, 4, COUNT_BASED)
                .slidingWindowSize(10)
                .slidingWindowType(COUNT_BASED)
                .minimumNumberOfCalls(4)
                .failureRateThreshold(50)
                .slowCallRateThreshold(100)
                .slowCallDurationThreshold(Duration.ofMillis(30000))
                .waitDurationInOpenState(Duration.ofMillis(10000))
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordException(new RecordFailurePredicate())
                //.recordException(e -> e instanceof CustomResponseStatusException exception
                        //&& exception.getStatus() == INTERNAL_SERVER_ERROR)
                //.recordExceptions(IOException.class, TimeoutException.class)
                //.recordExceptions(WriteTimeoutException.class, ReadTimeoutException.class, ConnectTimeoutException.class)
                .build();

        return CircuitBreakerRegistry.of(Map.of(CIRCUIT_BREAKER_CONFIG_NAME, circuitBreakerConfig));
    }

    @Bean
    public RetryRegistry configureRetryRegistry() {
        final RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                //.waitDuration(Duration.ofMillis(5000)) //Either this OR
                .intervalFunction(IntervalFunction.ofExponentialBackoff(IntervalFunction.DEFAULT_INITIAL_INTERVAL, 2)) // OR this
                .retryOnException(new RecordFailurePredicate())
                .build();

        return RetryRegistry.of(Map.of(RETRY_CONFIG_NAME, retryConfig));
    }
}
```

In the file above, Resilience4j Retry and CircuitBreaker configurations were defined using Spring Bean. The majority of these properties are explained above under the properties file config. Most of these properties' detailed definitions and what they do can be found on the Resilience4j getting started page.
RecordFailurePredicate - a customized Error Predicate class to filter any exception or failure that needs to cause a retry or count toward the circuit breaker threshold. The snippet below shows the implementation of RecordFailurePredicate.java

```java
public class RecordFailurePredicate implements Predicate<Throwable> {
    @Override
    public boolean test(Throwable e) {
        return recordFailures(e);
    }

    private boolean recordFailures(Throwable throwable) {
        return
                (throwable instanceof CustomResponseStatusException ex && ex.getStatus().is5xxServerError()) ||
                        throwable instanceof TimeoutException || throwable instanceof IOException ||
                throwable instanceof WebClientException;
    }
}
```

In this demo, only 5xx (Server) errors from API or TimeoutException or IOException or any WebClientExceptions will be used to initiate a retry or recorded as counting against the circuit breaker threshold. This can be implemented based on your business logic.

### C. Registering Retry & CircuitBreaker Events

All the events pertaining to the Retry mechanism and CircuitBreaker actions can be tracked or logged.
```java
@Configuration
public class EventsLoggerConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(EventsLoggerConfig.class);

    private static final String CIRCUIT_BREAKER_CONFIG_NAME = "testService";
    private static final String RETRY_CONFIG_NAME = "testService";

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public EventsLoggerConfig(CircuitBreakerRegistry circuitBreakerRegistry, RetryRegistry retryRegistry) {
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_CONFIG_NAME);
        this.retry = retryRegistry.retry(RETRY_CONFIG_NAME);
    }

    @PostConstruct
    public void logRegistryEvents() {
        retry.getEventPublisher()
                .onRetry(event -> LOGGER.info("RetryRegistryEventListener: [{}]", event));

        circuitBreaker.getEventPublisher()
                .onCallNotPermitted(event -> LOGGER.info("CircuitBreakerRegistryEventListener: State: [{}] Details: [{}]", circuitBreaker.getState(), event))
                .onSuccess(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onSuccess: [{}] - State: [{}]", event.getEventType(), circuitBreaker.getState()))
                .onError(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onError: [{}]", event))
                .onIgnoredError(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onIgnoredError: [{}] - State: [{}]", event.getEventType(), circuitBreaker.getState()))
                .onReset(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onReset: [{}] - State: [{}]", event.getEventType(), circuitBreaker.getState()))
                .onStateTransition(event -> LOGGER.info("CircuitBreakerRegistryEventListener: onStateTransition: [{}] - State: [{}]", event.getEventType(), circuitBreaker.getState()));
    }
}
```

In the file above, the PostConstruct lifecycle of Spring is hooked on in order to register the events being published. 
For the retry, the application will be listening to the onRetry event - when the application is going to retry the call, request, or action again and log it. 
For the circuit breaker, the application will be listening to onCallNotPermitted - when the circuit is in the open state and no action/call is allowed, onSuccess - when action/call is successful, onError - when there is a recorded error on the action/call being performed, onIgnoredError - when there is an error that is not recorded on the action/call being performed, onReset - when the circuit breaker is reset, and onstateTransition - when the circuit breaker transition from one state to another.
Resilient-Service - Sample Application
The sample application is a service implemented to call a third-party API service popularly known as JSONPlaceholder. We will make a GET call to the /posts and /posts/{id} to test our application and tweak the timeouts configured on our app to make the call fail in order to see the retry and the circuit breaker in action.
Project structure - the final structure of the project looks like this…
IntelliJ - final project structure with packages.Application WebClient - In the ApplicationWebClient.java file, WebClientBuilder is configured as a Spring bean with a base URL of https://jsonplaceholder.typicode.com with a custom client connector and custom logging of the request and the response as shown in the snippet below:

```java
@Component
public class ApplicationWebClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationWebClient.class);

    private final String serverBaseUrl;
    private final int connectTimeout;
    private final int readTimeout;
    private final int writeTimeout;
    private final int maxInMemorySize;

    public ApplicationWebClient(@Value("${api.client.baseUrl}")String serverBaseUrl,
                                @Value("${api.client.connectTimeout}") int connectTimeout,
                                @Value("${api.client.readTimeout}") int readTimeout,
                                @Value("${api.client.writeTimeout}") int writeTimeout,
                                @Value("${api.client.maxInMemorySize:2048}") int maxInMemorySize) {
        this.serverBaseUrl = serverBaseUrl;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.writeTimeout = writeTimeout;
        this.maxInMemorySize = maxInMemorySize;
    }

    @Bean
    public WebClient apiGatewayWebClient(final WebClient.Builder webClientBuilder) {
        return webClientBuilder
                .exchangeStrategies(ExchangeStrategies.builder()
                    .codecs(config -> config.defaultCodecs().maxInMemorySize(maxInMemorySize)).build())
                .filter(logRequestDetails())
                .filter(logResponseDetails())
                .defaultHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                .defaultHeader(ACCEPT, APPLICATION_JSON_VALUE)
                .clientConnector(new ReactorClientHttpConnector(clientConnectorConfig()))
                .baseUrl(serverBaseUrl)
                .build();
    }

    private ExchangeFilterFunction logRequestDetails() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            LOGGER.info("Sending [{}] request to URL [{}] with request headers [{}]",
                    clientRequest.method(), clientRequest.url(), clientRequest.headers());
            return just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponseDetails() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse ->
                clientResponse.bodyToMono(String.class).defaultIfEmpty("").flatMap(responseBody -> {
                    final ClientResponse orgClientResponse = clientResponse.mutate().body(responseBody).build();
                    LOGGER.info("Received response from API with body [{}] status [{}] with response headers [{}]",
                            responseBody, clientResponse.statusCode(), clientResponse.headers().asHttpHeaders().toSingleValueMap());
                    return just(orgClientResponse);
                })
        );
    }

    private HttpClient clientConnectorConfig() {
        return HttpClient.create().option(CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, MILLISECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(writeTimeout, MILLISECONDS));
                });
    }
}
```

The web client baseUrl, connectTimeout, readTimeout , writeTimeout, and maxInMemorySize have been configured in the application properties file and injected via constructor injection. The properties (from the application.yaml file) look as follows:

```yaml
server:
  port: 8095
  shutdown: GRACEFUL

api:
  client:
    baseUrl: https://jsonplaceholder.typicode.com
    connectTimeout: 5000
    readTimeout: 5000
    writeTimeout: 5000
    maxInMemorySize: 67108864
```

Application Router - This is the entry point for the service call. Three routes have been configured. The application will be running on port 8095 with one route /services/posts and the second as /services/posts/{id}. The third route is a default route that catches all incorrect routes invoked on the application and returns a 404 NOT_FOUND message. The snippet below shows how it is being configured in the ApplicationRouter.java file.

```java
@Component
public class ApplicationRouter {

    private static final Logger LOG = getLogger(ApplicationRouter.class);
    private static final String ROOT_PATH = "/services";

    @Bean
    @Order(1)
    public RouterFunction<ServerResponse> routerFunction(final ApplicationHandler handler) {
        return nest(path(ROOT_PATH).and(accept(APPLICATION_JSON)),
                route(GET("/posts"), request -> handler.apiGetRequest(request.path().substring(ROOT_PATH.length())))
                        .andRoute(GET("/posts/{id}"), request -> handler.apiGetRequest(request.path().substring(ROOT_PATH.length())))
        );
    }

    @Bean
    @Order(99)
    public RouterFunction<ServerResponse> defaultRouterFunction() {
        return route(path("/**"), this::defaultResponse);
    }

    private @NonNull Mono<ServerResponse> defaultResponse(final ServerRequest serverRequest) {
        final String path = serverRequest.path();
        LOG.info("Requested url or resource {} is not found.", path);
        return Mono.error(new CustomResponseStatusException(NOT_FOUND, "Resource requested is not found."));
    }
}
Application Handler - This implements the handler for the two routes. The method apiGetRequest handles the request from the two routes and performs the GET request with the configured web client to the external API. The snippet below shows how it is configured in the ApplicationHandler.java file.
@Component
public class ApplicationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationHandler.class);
    private static final String CIRCUIT_BREAKER_CONFIG_NAME = "testService";
    private static final String RETRY_CONFIG_NAME = "testService";

    private final WebClient webClient;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ApplicationHandler(final WebClient webClient, final CircuitBreakerRegistry circuitBreakerRegistry, final RetryRegistry retryRegistry) {
        this.webClient = webClient;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_CONFIG_NAME);
        this.retry = retryRegistry.retry(RETRY_CONFIG_NAME);
    }

    public Mono<ServerResponse> apiGetRequest(final String path) {
        Mono<String> responseBody = webClient.get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::handleErrorResponse)
                .bodyToMono(String.class)
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker)) // ORDER - If written below, circuit breaker will record a single failure after the max-retry
                .transformDeferred(RetryOperator.of(retry)) // ORDER - If above, retry will complete before a failure is recorded by the circuit breaker
                .doOnError(CallNotPermittedException.class::isInstance, throwable -> {
                    LOG.error("Circuit Breaker is in [{}]... Providing fallback response without calling the API", circuitBreaker.getState());
                    throw new CustomResponseStatusException(SERVICE_UNAVAILABLE, "API service is unavailable");
                });

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody, String.class);
    }

    private Mono<CustomResponseStatusException> handleErrorResponse(final ClientResponse clientResponse) {
        LOG.info("Handling error response: [{}]", clientResponse.statusCode());
        return clientResponse
                .bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(res -> Mono.just(new CustomResponseStatusException(clientResponse.statusCode(), res)));
    }
}
```

The code above (apiGetRequest method) chains the web client with the CircuitBreaker and Retry in a functional style using the transformDeferred chain. If any exception or failure that satisfies the defined error predicate (RecordFailurePredicate.java) is thrown from the API request, they will be a cause of retry or counted towards the circuit breaker threshold.

#### - The order of the two transform-deferred chains matters. 
If the retry is placed above the circuit breaker, all the retries will be exhausted before a circuit breaker can record an exception. 
For instance, if max retry is configured to be 3 and there is a failure/exception from the API call, the 3 retries will be completed before a circuit breaker records a failure (if all the three retries failed and satisfy the error predicate configured).
If the circuit breaker is placed above the retry as shown in the snippet above, every failure from each retry will be recorded and counted towards the circuit breaker threshold. For instance, if the max retry is configured to be 3 and the minimum number of calls to trigger the circuit breaker is 3 with a threshold of 100%, the circuit will break after the 3 retries as each failure from each call is counted towards the threshold of the circuit breaker.
#### - The circuit is opened.
If the circuit breaker is opened or in an OPEN state, for every request to the API, a CallNotPermittedException will be thrown that is handled by the doOnError chain, which will return a fallback response for all the requests to the API for the duration of the waitDurationInOpenState configured after which it will transition to a HALF_OPEN state if automaticTransitionFromOpenToHalfOpenEnabled is configured to be true.
#### - Fallback response in the open state
No request will be made if the circuit is in an open state. During this time, any call or request made will be short-circuited and a CallNotPermittedException will be thrown. This exception can be handled to return a configured response which is known as a fallback response. In this demo, this exception is handled on the doOnError chain and the request returns the fallback response of 503. This response will continue to be served for the duration waitDurationInOpenState until the circuit transition to a half-open state.
#### - Half-Open state
In the half-open state, the configured value permittedNumberOfCallsInHalfOpenStaterequests will be made in order to know if the circuit should be closed or not. If the number of requests that are successful is greater than the failure threshold, the circuit will transition back to a CLOSED state. More info at https://resilience4j.readme.io/docs/retry and https://resilience4j.readme.io/docs/circuitbreaker
#### - Running the sample application -

#### Everything is good -
```shell
curl http://localhost:8095/services/posts -H "Accept: application/json"
```
```log
#Logs
2022-12-25T19:13:32.450+11:00  INFO 15428 --- [ctor-http-nio-3] c.h.d.r.http.web.ApplicationWebClient    : Sending [GET] request to URL [https://jsonplaceholder.typicode.com/posts] with request headers [[Content-Type:"application/json", Accept:"application/json"]]
2022-12-25T19:13:33.748+11:00  INFO 15428 --- [ctor-http-nio-3] c.h.d.r.http.web.ApplicationWebClient    : Received response from API with body [[
  {
    "userId": 1,
    "id": 1,
    "title": "sunt aut facere repellat provident occaecati excepturi optio reprehenderit",
    "body": "quia et suscipit\nsuscipit recusandae consequuntur expedita et cum\nreprehenderit molestiae ut ut quas totam\nnostrum rerum est autem sunt rem eveniet architecto"
  },
  {
    "userId": 1,
    "id": 2,
    "title": "qui est esse",
    "body": "est rerum tempore vitae\nsequi sint nihil reprehenderit dolor beatae ea dolores neque qui neque nisi nulla"
  }
]] status [200 OK]
2022-12-25T19:13:33.768+11:00  INFO 15428 --- [ctor-http-nio-3] c.h.d.r.config.EventsLoggerConfig        : CircuitBreakerRegistryEventListener: onSuccess: [SUCCESS] - State: [CLOSED]
The request returns a successful response and no retry event occurs, the circuit breaker onSuccess event is: SUCCESS and the State is: CLOSED.
```

#### Testing Retry - 
To test the retry mechanism, the connectTimeout property inside theapplication.yaml is changed from 5000ms to 100ms as this will not allow the web client to have much time in having a successful handshake with the API server.
```yaml
api:
  client:
    baseUrl: https://jsonplaceholder.typicode.com
    connectTimeout: 100 #Changing this from 5000 to 100 - Error Scenario
    readTimeout: 5000
    writeTimeout: 5000
    maxInMemorySize: 67108864
```
```shell
curl http://localhost:8095/services/posts -H "Accept: application/json"
```
```log
#Logs
2022-12-25T19:34:02.919+11:00  :Sending [GET] request to URL [https://jsonplaceholder.typicode.com/posts] with request headers [[Content-Type:"application/json", Accept:"application/json"]]
2022-12-25T19:34:03.590+11:00  :CircuitBreakerRegistryEventListener: onError: [2022-12-25T19:34:03.589256700+11:00[Australia/Sydney]: CircuitBreaker 'testService' recorded an error: 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/104.21.58.30:443'. Elapsed time: 671 ms]
2022-12-25T19:34:03.600+11:00  :RetryRegistryEventListener: [2022-12-25T19:34:03.600249600+11:00[Australia/Sydney]: Retry 'testService', waiting PT5S until attempt '1'. Last attempt failed with exception 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/104.21.58.30:443'.]
2022-12-25T19:34:08.615+11:00  :Sending [GET] request to URL [https://jsonplaceholder.typicode.com/posts] with request headers [[Content-Type:"application/json", Accept:"application/json"]]
2022-12-25T19:34:08.852+11:00  :CircuitBreakerRegistryEventListener: onError: [2022-12-25T19:34:08.852861800+11:00[Australia/Sydney]: CircuitBreaker 'testService' recorded an error: 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/172.67.155.76:443'. Elapsed time: 237 ms]
2022-12-25T19:34:08.855+11:00  :RetryRegistryEventListener: [2022-12-25T19:34:08.855380800+11:00[Australia/Sydney]: Retry 'testService', waiting PT10S until attempt '2'. Last attempt failed with exception 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/172.67.155.76:443'.]
2022-12-25T19:34:18.870+11:00  :Sending [GET] request to URL [https://jsonplaceholder.typicode.com/posts] with request headers [[Content-Type:"application/json", Accept:"application/json"]]
2022-12-25T19:34:19.105+11:00  :CircuitBreakerRegistryEventListener: onError: [2022-12-25T19:34:19.105265700+11:00[Australia/Sydney]: CircuitBreaker 'testService' recorded an error: 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/104.21.58.30:443'. Elapsed time: 235 ms]
2022-12-25T19:34:19.117+11:00  :[86f725c7-1]  500 Server Error for HTTP GET "/services/posts"
Here, WebClientRequestException a subclass of WebClientException is being thrown. From the logs, it is obvious that it retries 3 times (maxRetry) with an exponential back-off and wait for the duration of 5s and 10s (exponential) before making the second and the third retries. Also, for every retry, the circuit breaker is recording the number of failures and counts toward the threshold.
Testing the CircuitBreaker -
 If a request is made twice, the 4 retries (3 retries from the first request and one retry of the second request) will swing the circuit breaker into action. Since the minimum number of calls is 4 and the threshold is 50%, once the request reaches 4 and if there is a minimum of 2 failures, the circuit will be opened.
2022-12-25T19:49:35.020+11:00  : Sending [GET] request to URL [https://jsonplaceholder.typicode.com/posts] with request headers [[Content-Type:"application/json", Accept:"application/json"]]
2022-12-25T19:49:35.650+11:00  : CircuitBreakerRegistryEventListener: onError: [2022-12-25T19:49:35.649680300+11:00[Australia/Sydney]: CircuitBreaker 'testService' recorded an error: 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/172.67.155.76:443'. Elapsed time: 631 ms]
2022-12-25T19:49:35.659+11:00  : RetryRegistryEventListener: [2022-12-25T19:49:35.659672800+11:00[Australia/Sydney]: Retry 'testService', waiting PT5S until attempt '1'. Last attempt failed with exception 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/172.67.155.76:443'.]
2022-12-25T19:49:40.675+11:00  : Sending [GET] request to URL [https://jsonplaceholder.typicode.com/posts] with request headers [[Content-Type:"application/json", Accept:"application/json"]]
2022-12-25T19:49:40.910+11:00  : CircuitBreakerRegistryEventListener: onError: [2022-12-25T19:49:40.910986700+11:00[Australia/Sydney]: CircuitBreaker 'testService' recorded an error: 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/104.21.58.30:443'. Elapsed time: 235 ms]
2022-12-25T19:49:40.912+11:00  : RetryRegistryEventListener: [2022-12-25T19:49:40.912985400+11:00[Australia/Sydney]: Retry 'testService', waiting PT10S until attempt '2'. Last attempt failed with exception 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/104.21.58.30:443'.]
2022-12-25T19:49:50.921+11:00  : Sending [GET] request to URL [https://jsonplaceholder.typicode.com/posts] with request headers [[Content-Type:"application/json", Accept:"application/json"]]
2022-12-25T19:49:51.158+11:00  : CircuitBreakerRegistryEventListener: onError: [2022-12-25T19:49:51.158965100+11:00[Australia/Sydney]: CircuitBreaker 'testService' recorded an error: 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/172.67.155.76:443'. Elapsed time: 237 ms]
2022-12-25T19:49:51.169+11:00  : [8e9a8f1f-1]  500 Server Error for HTTP GET "/services/posts"

2022-12-25T19:49:51.267+11:00  : Sending [GET] request to URL [https://jsonplaceholder.typicode.com/posts] with request headers [[Content-Type:"application/json", Accept:"application/json"]]
2022-12-25T19:49:51.490+11:00  : CircuitBreakerRegistryEventListener: onError: [2022-12-25T19:49:51.490901300+11:00[Australia/Sydney]: CircuitBreaker 'testService' recorded an error: 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/172.67.155.76:443'. Elapsed time: 223 ms]
2022-12-25T19:49:51.495+11:00  : CircuitBreakerRegistryEventListener: onStateTransition: [STATE_TRANSITION] - State: [OPEN]
2022-12-25T19:49:51.495+11:00  : RetryRegistryEventListener: [2022-12-25T19:49:51.495894200+11:00[Australia/Sydney]: Retry 'testService', waiting PT5S until attempt '1'. Last attempt failed with exception 'org.springframework.web.reactive.function.client.WebClientRequestException: connection timed out: jsonplaceholder.typicode.com/172.67.155.76:443'.]
2022-12-25T19:49:56.505+11:00  : CircuitBreakerRegistryEventListener: State: [OPEN] Details: [2022-12-25T19:49:56.505291500+11:00[Australia/Sydney]: CircuitBreaker 'testService' recorded a call which was not permitted.]
2022-12-25T19:49:56.508+11:00  : Circuit Breaker is in [OPEN]... Providing fallback response without calling the API
2022-12-25T19:50:24.270+11:00  : CircuitBreakerRegistryEventListener: State: [OPEN] Details: [2022-12-25T19:50:24.270646700+11:00[Australia/Sydney]: CircuitBreaker 'testService' recorded a call which was not permitted.]
2022-12-25T19:50:24.270+11:00  : Circuit Breaker is in [OPEN]... Providing fallback response without calling the API
2022-12-25T19:50:51.508+11:00  : CircuitBreakerRegistryEventListener: onStateTransition: [STATE_TRANSITION] - State: [HALF_OPEN]
```

From the logs, it is noted that after the 4th request, the circuit breaker triggers, and the circuit transition to an OPEN state at 19:49:51. The two calls made in that open state quickly return a fallback response without even making any request to the API. The circuit transition to a HALF_OPEN state at 19:50:51, 60 seconds after the circuit transition to an open state.
Integration Tests - Complete cycle of Retry and CircuitBreaker.
For the tests, we will be using the following configuration
```yaml
server:
  port: 55550
  shutdown: GRACEFUL

api:
  client:
    baseUrl: http://localhost:54500 #Port defined here should be used with mockWebServer
    connectTimeout: 5000
    readTimeout: 5000
    writeTimeout: 5000
    maxInMemorySize: 67108864

resilience4j:
  # Retry
  retry:
    retryAspectOrder: 2
    instances:
      testService:
        enableExponentialBackoff: true
        exponentialBackoffMultiplier: 2
        maxAttempts: 3
        waitDuration: 1s
        retryExceptionPredicate: com.habeebcycle.demo.resilientservice.http.exception.RecordFailurePredicate

  # Circuitbreaker
  circuitbreaker:
    circuitBreakerAspectOrder: 1
    instances:
      testService:
        registerHealthIndicator: true
        slidingWindowSize: 5
        minimumNumberOfCalls: 3
        failureRateThreshold: 100
        waitDurationInOpenState: 10s
        permittedNumberOfCallsInHalfOpenState: 2
        automaticTransitionFromOpenToHalfOpenEnabled: true
        recordFailurePredicate: com.habeebcycle.demo.resilientservice.http.exception.RecordFailurePredicate
```
```java
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
  this.mockBackEnd.start(54500);  //Port defined on the application.yaml file
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
```

For the test, the test class is annotated @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) to clear the context and reset the counters, otherwise, the test will fail.
Test 1: shouldARetryOnErrorAndFetchSuccessResponse
Three responses were configured for the 3 retries request. The first two responses will return 500 error responses and the third one will return an OK response. The test request asserts a status of OK and also asserts that the number of requests that hit the mock web server is 3 because of the 3 retries. Also, the circuit breaker is in a CLOSED state.
Test 2: shouldBRetryOnErrorAndBreakCircuitResponse
Four responses were configured. The first three responses will return 500 error responses and the fourth one will return an OK response. But since the minimum number of calls for the circuit breaker is 3 and failureRateThreshold is configured to be 100%, once all the 3 retries were exhausted, the circuit transitioned to an OPEN state. The test request asserts a status of 500 and also asserts that the number of requests that hit our mock web server is 3 because of the 3 retries. Also, the circuit breaker is in an OPEN state.
Test 3: shouldCRetryAndFailAndBreakAndCloseTheCircuitTest
The first request was retried 3 times and caused the circuit to break and transition to an OPEN state. A request in that OPEN state quickly returned a fallback 503 Error. After 10s (waitDurationInOpenState - configured), the circuit transitions to HALF_OPEN state. In this state, a request was made that returned an error after the first request and returned a successful response after the first retry (after the second request). This last successful response closed the circuit as permittedNumberOfCallsInHalfOpenState is 2 with failureRateThreshold of 100%. Since the failure count is 1 out of 2, the circuit will be closed.

### Conclusion -
The above application demonstrates an example of using Circuit Breaker and Retry in a reactive functional way. The most common approach is to use them as annotation which is not possible with reactive router functions and router predicates.
Resilience4j provides higher-order functions (decorators) to enhance any functional interface, lambda expression, or method reference with a Circuit Breaker, Rate Limiter, Retry, or Bulkhead. You can stack more than one decorator on any functional interface, lambda expression, or method reference. The advantage is that you have the choice to select the decorators you need and nothing else. With Resilience4j you don't have to go all-in, you can pick what you need.
The source code for this sample project is available on GitHub. Clone it and try it. If there is any better approach, suggestions, or corrections, please don't hesitate to let me know.

Thanks for reading…
