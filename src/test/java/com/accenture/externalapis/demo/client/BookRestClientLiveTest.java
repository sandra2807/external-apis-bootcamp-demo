package com.accenture.externalapis.demo.client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Opt in explicitly: these checks require access to the configured external service.
@SpringBootTest
@EnabledIfSystemProperty(named = "externalApiChecks", matches = "true")
class BookRestClientLiveTest {

    @Autowired
    private BookRestClient client;

    @Test
    void fetchesOneBookFromTheRealService() {
        assertThat(client.getBook(1L).title()).isEqualTo("The Spaghetti Whisperer");
    }

    @Test
    void fetchesAllBooksFromTheRealService() {
        assertThat(client.getAllBooks()).isNotEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {991, 992, 993})
    void wrapsRealChaosResponses(Long id) {
        assertThatThrownBy(() -> client.getBook(id)).isInstanceOf(ClientException.class);
    }
}
