package com.EDJ.ArCash.DTO.AuthDTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Alta de un contacto favorito.
 *
 * <p>{@code contactAlias} necesita {@code @NotBlank} y no solo {@code @Size}: el controller hace
 * {@code request.contactAlias().trim()}, que con un alias ausente lanzaba
 * {@code NullPointerException} y devolvia un 500 en lugar de un 400.
 */
public record AddFavoriteContactRequest(

        @NotNull(message = "El id de la cuenta es obligatorio")
        @Positive(message = "El id de la cuenta debe ser positivo")
        Long accountId,

        @NotBlank(message = "El alias del contacto es obligatorio")
        @Size(max = 50, message = "El alias no puede superar los 50 caracteres")
        String contactAlias,

        @Size(max = 200, message = "La descripción no puede superar los 200 caracteres")
        String description
) {}
