export type CardStatus = 'ACTIVE' | 'PAUSED' | 'CANCELLED';

export interface VirtualCardSummary {
  id: number;
  accountId: number;
  currency: 'ARS' | 'USD';
  last4: string;
  status: CardStatus;
  dailyLimit: number;
  holderName: string;
  pinConfigured: boolean;
  expMonth?: number;
  expYear?: number;
  expired?: boolean;
  cancelledAt?: string | null;
  canReissue?: boolean;
  reissueMessage?: string | null;
}

export interface VirtualCardListResponse {
  pinConfigured: boolean;
  cards: VirtualCardSummary[];
}

export interface CardUnlockResponse {
  success: boolean;
  message: string;
  unlockToken?: string | null;
  locked?: boolean;
  expiresInSeconds?: number;
}

export interface VirtualCardReveal {
  id: number;
  currency: 'ARS' | 'USD';
  pan: string;
  last4: string;
  cvc: string;
  expMonth: number;
  expYear: number;
  status: CardStatus;
  dailyLimit: number;
  holderName: string;
}

export interface CardAuditEvent {
  id: number;
  cardId: number | null;
  type: string;
  meta: string | null;
  createdAt: string;
}
