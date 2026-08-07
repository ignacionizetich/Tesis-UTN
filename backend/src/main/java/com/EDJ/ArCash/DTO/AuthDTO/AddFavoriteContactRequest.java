package com.EDJ.ArCash.DTO.AuthDTO;

public record AddFavoriteContactRequest(

        Long accountId,

        String contactAlias,

        String description // Opcional
) {}