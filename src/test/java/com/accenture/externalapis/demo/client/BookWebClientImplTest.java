package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookDto;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.GET;

class BookWebClientImplTest {

    private String bookJson;
    private final BookDto expectedBook = new BookDto("The Spaghetti Whisperer",
            "Sir Reginald Fluffington III", "Software Engineering", 34.99);

    @BeforeEach
    void setUp() throws IOException {
        bookJson = new ClassPathResource("book.json").getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void fetchesAndMapsOneBookOnlyAfterSubscription() {
        AtomicInteger requests = new AtomicInteger();
        BookWebClient client = client(request -> {
            requests.incrementAndGet();
            assertThat(request.method()).isEqualTo(GET);
            assertThat(request.url().toString()).isEqualTo("https://books.test/api/books/1");
            return Mono.just(response(HttpStatus.OK, MediaType.APPLICATION_JSON, bookJson));
        });

        Mono<BookDto> result = client.getBookAsync(1L);
        assertThat(requests.get()).isZero();
        StepVerifier.create(result).expectNext(expectedBook).verifyComplete();
        assertThat(requests.get()).isEqualTo(1);
    }

    @Test
    void fetchesAndMapsAllBooks() {
        BookWebClient client = client(request -> {
            assertThat(request.method()).isEqualTo(GET);
            assertThat(request.url().toString()).isEqualTo("https://books.test/api/books");
            return Mono.just(response(HttpStatus.OK, MediaType.APPLICATION_JSON,
                    "[" + bookJson + "," + bookJson + "]"));
        });

        StepVerifier.create(client.getAllBooksAsync())
                .expectNext(expectedBook, expectedBook).verifyComplete();
    }

    @Test
    void acceptsAnEmptyBookArray() {
        BookWebClient client = respondingWith(HttpStatus.OK, MediaType.APPLICATION_JSON, "[]");

        StepVerifier.create(client.getAllBooksAsync()).verifyComplete();
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 418, 500, 503})
    void wrapsHttpErrorsAndPreservesTheirCause(int status) {
        BookWebClient client = respondingWith(HttpStatus.valueOf(status), MediaType.APPLICATION_JSON, "{}");

        StepVerifier.create(client.getBookAsync(993L))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ClientException.class).hasMessageContaining("993")
                        .hasCauseInstanceOf(WebClientResponseException.class))
                .verify();
    }

    @Test
    void rejectsWrongContentTypeLikeChaos991() {
        BookWebClient client = respondingWith(HttpStatus.OK, MediaType.TEXT_PLAIN, bookJson);

        StepVerifier.create(client.getBookAsync(991L)).expectError(ClientException.class).verify();
    }

    @Test
    void rejectsEmptyBodyLikeChaos992() {
        BookWebClient client = respondingWith(HttpStatus.OK, MediaType.APPLICATION_JSON, "");

        StepVerifier.create(client.getBookAsync(992L))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ClientException.class).hasMessageContaining("empty response"))
                .verify();
    }

    @Test
    void wrapsMalformedJson() {
        BookWebClient client = respondingWith(HttpStatus.OK, MediaType.APPLICATION_JSON, "{broken");

        StepVerifier.create(client.getBookAsync(1L))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ClientException.class).hasCauseInstanceOf(Exception.class))
                .verify();
    }

    @Test
    void wrapsConnectionFailure() {
        BookWebClient client = unreachableClient();

        StepVerifier.create(client.getBookAsync(1L))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ClientException.class).hasCauseInstanceOf(WebClientRequestException.class))
                .verify();
    }

    @Test
    void wrapsBookListHttpFailure() {
        BookWebClient client = respondingWith(HttpStatus.SERVICE_UNAVAILABLE, MediaType.APPLICATION_JSON, "{}");

        StepVerifier.create(client.getAllBooksAsync())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ClientException.class).hasCauseInstanceOf(WebClientResponseException.class))
                .verify();
    }

    @Test
    void wrapsBookListConnectionFailure() {
        BookWebClient client = unreachableClient();

        StepVerifier.create(client.getAllBooksAsync())
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(ClientException.class).hasCauseInstanceOf(WebClientRequestException.class))
                .verify();
    }

    @Test
    void startsBothRequestsBeforeEitherResponseAndPreservesInputOrder() {
        Sinks.One<ClientResponse> first = Sinks.one();
        Sinks.One<ClientResponse> second = Sinks.one();
        AtomicInteger requests = new AtomicInteger();
        BookWebClient client = client(request -> {
            requests.incrementAndGet();
            return request.url().getPath().endsWith("/1") ? first.asMono() : second.asMono();
        });
        String secondJson = bookJson.replace("The Spaghetti Whisperer", "Second Book");
        BookDto secondBook = new BookDto("Second Book", expectedBook.author(), expectedBook.genre(), expectedBook.price());

        StepVerifier.create(client.getBooksInParallel(1L, 2L))
                .then(() -> assertThat(requests.get()).isEqualTo(2))
                // Return the second response first, so the result must preserve zip's input order.
                .then(() -> second.tryEmitValue(response(HttpStatus.OK, MediaType.APPLICATION_JSON, secondJson)))
                .then(() -> first.tryEmitValue(response(HttpStatus.OK, MediaType.APPLICATION_JSON, bookJson)))
                .expectNext(List.of(expectedBook, secondBook))
                .expectComplete()
                .verify(Duration.ofSeconds(5));
    }

    @ParameterizedTest
    @ValueSource(longs = {991, 992, 993})
    void propagatesChaosErrorsFromParallelRequests(Long id) {
        BookWebClient client = client(request -> {
            if (request.url().getPath().endsWith("/1")) {
                return Mono.just(response(HttpStatus.OK, MediaType.APPLICATION_JSON, bookJson));
            }
            return Mono.just(response(id == 993 ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.OK,
                    id == 991 ? MediaType.TEXT_PLAIN : MediaType.APPLICATION_JSON,
                    id == 992 ? "" : bookJson));
        });

        StepVerifier.create(client.getBooksInParallel(1L, id)).expectError(ClientException.class).verify();
        StepVerifier.create(client.getBooksInParallel(id, 1L)).expectError(ClientException.class).verify();
    }

    private BookWebClient client(ExchangeFunction exchangeFunction) {
        return new BookWebClientImpl(WebClient.builder().exchangeFunction(exchangeFunction),
                new ExternalServiceProperties("https://books.test/api"));
    }

    private BookWebClient respondingWith(HttpStatus status, MediaType contentType, String body) {
        return client(request -> Mono.just(response(status, contentType, body)));
    }

    private ClientResponse response(HttpStatus status, MediaType contentType, String body) {
        ClientResponse.Builder response = ClientResponse.create(status)
                .header("Content-Type", contentType.toString());
        // No data buffers represents a real empty HTTP body.
        if (!body.isEmpty()) {
            response.body(body);
        }
        return response.build();
    }

    private BookWebClient unreachableClient() {
        return client(request -> Mono.error(new WebClientRequestException(
                new ConnectException("Connection refused"), request.method(), request.url(), request.headers())));
    }
}
