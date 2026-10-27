package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class BookWebClientImpl implements BookWebClient {

    private final WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        return webClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .bodyToMono(BookApiResponse.class)
                .map(BookApiResponse::toDto)
                .onErrorMap(ex -> new ClientException(
                        "Could not fetch book " + id + " from the external book service", ex))
                .switchIfEmpty(Mono.error(new ClientException(
                        "External book service returned an empty response for book " + id)));
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        return webClient.get()
                .uri("/books")
                .retrieve()
                .bodyToFlux(BookApiResponse.class)
                .map(BookApiResponse::toDto)
                .onErrorMap(ex -> new ClientException(
                        "Could not fetch books from the external book service", ex));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        return Mono.zip(getBookAsync(id1), getBookAsync(id2))
                .map(books -> List.of(books.getT1(), books.getT2()));
    }
}
