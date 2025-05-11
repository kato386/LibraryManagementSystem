package com.cagatayergunes.library.model.request;



import jakarta.validation.constraints.*;

public record FeedbackRequest(
        @Positive(message = "Note must be positive")
        @Min(value = 0, message = "Note must be greater than 0.")
        @Max(value = 5, message = "Note must be lesser than 5.")
        Double note,
        @NotNull(message = "Comment can not be null.")
        @NotEmpty(message = "Comment can not be empty.")
        @NotBlank(message = "Comment can not be blank.")
        String comment,
        @NotNull(message = "Book id is required for feedback.")
        Long bookId
) {
}
