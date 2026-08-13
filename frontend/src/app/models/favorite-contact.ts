/** Contacto favorito (DTO backend FavoriteContactResponse). */
export interface FavoriteContact {
  id: number;
  contactAlias: string;
  description?: string | null;
  creationDate: string;
  lastUsed?: string | null;
  active: boolean;
  accountOwnerName: string;
  accountOwnerAlias: string;
  accountCbu: string;
  accountAlias: string;
  accountType: string;
}

export interface FavoriteListResponse {
  status: string;
  favorites: FavoriteContact[];
}

export interface FavoriteMutationResponse {
  status: string;
  message?: string;
  success?: boolean;
}

export interface AddFavoriteContactRequest {
  accountId: number;
  contactAlias: string;
  description?: string;
}

export interface UpdateFavoriteContactRequest {
  contactAlias?: string;
  description?: string;
}

/** @deprecated Prefer FavoriteContact */
export default FavoriteContact;
