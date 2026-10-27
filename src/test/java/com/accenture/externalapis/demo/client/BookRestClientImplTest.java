package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookDto;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

class BookRestClientImplTest {

    private MockRestServiceServer server;
    private BookRestClient client;
    private String bookJson;
    private final BookDto expectedBook = new BookDto("The Spaghetti Whisperer",
            "Sir Reginald Fluffington III", "Software Engineering", 34.99);

    @BeforeEach
    void setUp() throws IOException {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BookRestClientImpl(builder, new ExternalServiceProperties("https://books.test/api"));
        bookJson = new ClassPathResource("book.json").getContentAsString(StandardCharsets.UTF_8);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void fetchesAndMapsOneBook() {
        server.expect(requestTo("https://books.test/api/books/1")).andExpect(method(GET))
                .andRespond(withSuccess(bookJson, MediaType.APPLICATION_JSON));

        assertThat(client.getBook(1L)).isEqualTo(expectedBook);
    }

    @Test
    void fetchesAndMapsAllBooks() {
        server.expect(requestTo("https://books.test/api/books")).andExpect(method(GET))
                .andRespond(withSuccess("[" + bookJson + "," + bookJson + "]", MediaType.APPLICATION_JSON));

        assertThat(client.getAllBooks()).containsExactly(expectedBook, expectedBook);
    }

    @Test
    void acceptsAnEmptyBookArray() {
        server.expect(requestTo("https://books.test/api/books"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.getAllBooks()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {404, 418, 500, 503})
    void wrapsHttpErrorsAndPreservesTheirCause(int status) {
        server.expect(requestTo("https://books.test/api/books/993"))
                .andRespond(withStatus(HttpStatus.valueOf(status)));

        assertThatThrownBy(() -> client.getBook(993L))
                .isInstanceOf(ClientException.class).hasMessageContaining("993")
                .hasCauseInstanceOf(RestClientResponseException.class);
    }

    @Test
    void rejectsWrongContentTypeLikeChaos991() {
        server.expect(requestTo("https://books.test/api/books/991"))
                .andRespond(withSuccess(bookJson, MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.getBook(991L)).isInstanceOf(ClientException.class);
    }

    @Test
    void rejectsEmptyBodyLikeChaos992() {
        server.expect(requestTo("https://books.test/api/books/992"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getBook(992L))
                .isInstanceOf(ClientException.class).hasMessageContaining("empty response");
    }

    @Test
    void wrapsMalformedJson() {
        server.expect(requestTo("https://books.test/api/books/1"))
                .andRespond(withSuccess("{broken", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getBook(1L)).isInstanceOf(ClientException.class).hasCauseInstanceOf(Exception.class);
    }

    @Test
    void wrapsConnectionFailure() {
        server.expect(requestTo("https://books.test/api/books/1"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(() -> client.getBook(1L))
                .isInstanceOf(ClientException.class).hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void wrapsBookListHttpFailure() {
        server.expect(requestTo("https://books.test/api/books"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(client::getAllBooks)
                .isInstanceOf(ClientException.class).hasCauseInstanceOf(RestClientResponseException.class);
    }

    @Test
    void wrapsBookListConnectionFailure() {
        server.expect(requestTo("https://books.test/api/books"))
                .andRespond(withException(new IOException("Connection refused")));

        assertThatThrownBy(client::getAllBooks)
                .isInstanceOf(ClientException.class).hasCauseInstanceOf(ResourceAccessException.class);
    }

    @Test
    void rejectsEmptyBookListBody() {
        server.expect(requestTo("https://books.test/api/books"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThatThrownBy(client::getAllBooks)
                .isInstanceOf(ClientException.class).hasMessageContaining("empty response");
    }
}
