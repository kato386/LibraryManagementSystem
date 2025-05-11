package com.cagatayergunes.library.model.request;

import com.cagatayergunes.library.model.RoleName;
import com.cagatayergunes.library.validator.ValueOfEnum;
import jakarta.validation.constraints.NotBlank;

public record RoleUpdateRequest(
        @NotBlank(message = "Role is required.")
        @ValueOfEnum(enumClass = RoleName.class, message = "Role must be one of: ADMIN, PATRON, LIBRARIAN")
        String role
) {
}
