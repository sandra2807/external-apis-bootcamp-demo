package com.accenture.externalapis.demo.dto;

// Raw response from the external API, including fields our application does not expose.
public record BookApiResponse(Long id,
                              String title,
                              String author,
                              String genre,
                              double price,
                              String isbn,
                              Integer publishedYear) {

    public BookDto toDto() {
        return new BookDto(title, author, genre, price);
    }
}
