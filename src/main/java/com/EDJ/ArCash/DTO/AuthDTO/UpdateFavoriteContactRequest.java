package com.EDJ.ArCash.DTO.AuthDTO;

import jakarta.validation.constraints.Size;

public record UpdateFavoriteContactRequest(
        @Size(min = 1, max = 50, message = "El alias debe tener entre 1 y 50 caracteres")
        String contactAlias,

        @Size(max = 200, message = "La descripción no puede superar los 200 caracteres")
        String description
) {}