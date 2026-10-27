package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookDto;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class BookWebClientImpl implements BookWebClient {

    private WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        // TODO: Build the WebClient using builder.baseUrl(properties.baseUrl()).build()
        // and assign it to this.webClient
        //
        // Optional/bonus: this service doesn't require auth, but in a real API you would
        // often also add builder.defaultHeader("Authorization", "Bearer " + token) here.
    }

    // Task 2 will replace these placeholders. Implementing the interface lets
    // Spring inject this bean into the existing controller during Task 1.
    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return Mono.error(new UnsupportedOperationException("WebClient is implemented in Practical Task 2"));
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return Flux.error(new UnsupportedOperationException("WebClient is implemented in Practical Task 2"));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        return Mono.error(new UnsupportedOperationException("WebClient is implemented in Practical Task 2"));
    }

    // TODO: Implement getBookAsync(Long id) - fetch one book from GET /books/{id} as
    // Mono<BookApiResponse>, then map it onto a Mono<BookDto>.
    //
    // TODO: Handle the main WebClient error cases and rethrow them as ClientException,
    // e.g. via onStatus()/onErrorResume():
    //  - WebClientResponseException (4xx/5xx, e.g. book not found or the faulty/teapot book)
    //  - WebClientRequestException (connection refused / timeout - the external service is unreachable)

    // TODO: Implement getAllBooksAsync() - fetch all books from GET /books as
    // Flux<BookApiResponse>, then map each one onto a BookDto. Handle the same error
    // cases as getBookAsync() above.

    // TODO: Implement getBooksInParallel(Long id1, Long id2) - fetch two books in
    // parallel with Mono.zip(). Handle the same error cases as getBookAsync() above.
}
