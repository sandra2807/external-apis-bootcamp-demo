package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BookRestClientImpl implements BookRestClient {


    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public BookDto getBook(Long id) {
        try {
            BookApiResponse response = restClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookApiResponse.class);
            if (response == null) {
                throw new ClientException("External book service returned an empty response for book " + id);
            }
            return response.toDto();
        } catch (RestClientException ex) {
            // Includes HTTP errors, connection failures and unreadable responses.
            throw new ClientException("Could not fetch book " + id + " from the external book service", ex);
        }
    }

    @Override
    public List<BookDto> getAllBooks() {
        try {
            BookApiResponse[] response = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);
            if (response == null) {
                throw new ClientException("External book service returned an empty response for the book list");
            }
            return Arrays.stream(response).map(BookApiResponse::toDto).toList();
        } catch (RestClientException ex) {
            throw new ClientException("Could not fetch books from the external book service", ex);
        }
    }
}
