package com.cagatayergunes.library.model.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record UserRequest (
        @NotEmpty(message = "First Name is mandatory")
        @NotBlank(message = "First Name is mandatory")
        String firstName,
        @NotEmpty(message = "Last Name is mandatory")
        @NotBlank(message = "Last Name is mandatory")
        String lastName,
        @Email(message = "Email is not well formatted")
        @NotEmpty(message = "Email is mandatory")
        @NotBlank(message = "Email is mandatory")
        String email,
        boolean accountLocked
){
}
