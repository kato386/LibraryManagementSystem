package com.cagatayergunes.library.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;


public record BookRequest(
        Long id,

        @NotNull(message = "Title cannot be null")
        @NotEmpty(message = "Title cannot be empty")
        String title,
        @NotNull(message = "Author cannot be null")
        @NotEmpty(message = "Author cannot be empty")
        String authorName,
        @NotNull(message = "Isbn cannot be null")
        @NotEmpty(message = "Isbn cannot be empty")
        String isbn,
        @NotNull(message = "Synopsis cannot be null")
        @NotEmpty(message = "Synopsis cannot be empty")
        String synopsis,
        @NotNull(message = "Genre cannot be null")
        @NotEmpty(message = "Genre cannot be empty")
        String genre,
        LocalDate publicationDate,
        boolean shareable
) {
}
