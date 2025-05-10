package com.cagatayergunes.library.model.request;



import jakarta.validation.constraints.*;

public record FeedbackRequest(
        @Positive(message = "200")
        @Min(value = 0, message = "201")
        @Max(value = 5, message = "202")
        Double note,
        @NotNull(message = "203")
        @NotEmpty(message = "204")
        @NotBlank(message = "205")
        String comment,
        @NotNull(message = "206")
        Long bookId
) {
}
