package com.accenture.externalapis.demo.client;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "externalApiChecks", matches = "true")
class BookWebClientLiveTest {

    @Autowired
    private BookWebClient client;

    @Test
    void fetchesOneBookFromTheRealService() {
        StepVerifier.create(client.getBookAsync(1L))
                .assertNext(book -> assertThat(book.title()).isEqualTo("The Spaghetti Whisperer"))
                .expectComplete().verify(Duration.ofSeconds(20));
    }

    @Test
    void fetchesAllBooksFromTheRealService() {
        StepVerifier.create(client.getAllBooksAsync().collectList())
                .assertNext(books -> assertThat(books).isNotEmpty())
                .expectComplete().verify(Duration.ofSeconds(20));
    }

    @Test
    void fetchesTwoBooksFromTheRealService() {
        StepVerifier.create(client.getBooksInParallel(1L, 2L))
                .assertNext(books -> assertThat(books).extracting("title")
                        .containsExactly("The Spaghetti Whisperer", "Untangling the Yarn Ball of Doom"))
                .expectComplete().verify(Duration.ofSeconds(20));
    }

    @ParameterizedTest
    @ValueSource(longs = {991, 992, 993})
    void wrapsRealChaosResponses(Long id) {
        StepVerifier.create(client.getBookAsync(id))
                .expectError(ClientException.class).verify(Duration.ofSeconds(20));
    }

    @ParameterizedTest
    @ValueSource(longs = {991, 992, 993})
    void propagatesRealChaosResponsesFromParallelRequests(Long id) {
        StepVerifier.create(client.getBooksInParallel(1L, id))
                .expectError(ClientException.class).verify(Duration.ofSeconds(20));
        StepVerifier.create(client.getBooksInParallel(id, 1L))
                .expectError(ClientException.class).verify(Duration.ofSeconds(20));
    }
}
