# Requerimientos Backend para Cuenta USD

## 1. Endpoint de Cuentas de Usuario

### GET `/accounts/user-accounts`

**Respuesta actual esperada:**
```json
{
  "success": true,
  "accounts": [
    {
      "id": "account_ars_123",
      "balance": 50000.50,
      "alias": "juan.arcash",
      "cvu": "0000003100000000000001",
      "currency": "ARS"
    },
    {
      "id": "account_usd_456",
      "balance": 100.00,
      "alias": "juan.arcash.usd",
      "cvu": "0000003100000000000002",
      "currency": "USD"
    }
  ]
}
```

**Importante:**
- ✅ Cada cuenta (ARS y USD) debe tener su propio `alias` y `cvu` únicos
- ✅ El campo `currency` debe ser "ARS" o "USD"
- ✅ El `alias` y `cvu` de la cuenta USD deben ser diferentes a los de la cuenta ARS

---

## 2. Transacciones con USD

### ⚠️ IMPORTANTE: Lógica de Transferencias según Tipo de Cuenta

El backend debe identificar el tipo de transferencia según las cuentas involucradas:

```
SI cuenta_origen.currency === "USD" Y cuenta_destino.currency === "USD":
  → Transferencia DIRECTA en USD (NO hay conversión)
  → Debitar de cuenta USD
  → Acreditar en cuenta USD
  → amount en la transacción = monto en USD
  → currency = "USD"
  → originalCurrency = "USD"
  → NO calcular conversión (exchangeRate e amountInArs solo informativos)

SI cuenta_origen.currency === "USD" Y cuenta_destino.currency === "ARS":
  → Transferencia con conversión USD → ARS
  → Debitar de cuenta USD
  → Convertir USD a ARS
  → Acreditar en cuenta ARS
  → amount = monto en USD
  → currency = "ARS"
  → originalCurrency = "USD"
  → amountInArs = amount * exchangeRate

SI cuenta_origen.currency === "ARS" Y cuenta_destino.currency === "USD":
  → Transferencia con conversión ARS → USD
  → Debitar de cuenta ARS
  → Convertir ARS a USD
  → Acreditar en cuenta USD
  → amount = monto en ARS
  → currency = "USD"
  → originalCurrency = "ARS"
  → amountInArs = amount (ya está en ARS)

SI cuenta_origen.currency === "ARS" Y cuenta_destino.currency === "ARS":
  → Transferencia en pesos
  → Debitar de cuenta ARS
  → Acreditar en cuenta ARS
  → currency = "ARS"
  → originalCurrency = "ARS"
```

---

### Tipos de Transferencias:

#### A) Transferencia directa USD → USD (cuenta USD a cuenta USD)

**⚠️ CRÍTICO: NO hay conversión de moneda**

**Flujo del Backend:**
1. Usuario selecciona transferir desde cuenta USD
2. Usuario selecciona cuenta destino (que es USD)
3. Usuario ingresa: 100 USD
4. Backend debe:
   - ✅ Debitar 100 USD de `cuenta_origen_usd.balance`
   - ✅ Acreditar 100 USD a `cuenta_destino_usd.balance`
   - ❌ **NO** debitar de cuenta ARS
   - ❌ **NO** aplicar conversión
   - ❌ **NO** aplicar comisión (solo entre cuentas del mismo usuario o según políticas)

**La transacción debe indicar que AMBOS lados usan USD:**

```json
{
  "id": 123,
  "type": "expense",
  "description": "Transferencia USD a María Pérez",
  "amount": 100.00,
  "date": "2026-01-19T10:30:00Z",
  "from": "juan.arcash.usd",
  "to": "maria.arcash.usd",
  "currency": "USD",
  "exchangeRate": 1100.00,
  "amountInArs": 110000.00,
  "originalCurrency": "USD",
  "status": "COMPLETED"
}
```

**Importante:** 
- ✅ `currency: "USD"` indica que la cuenta destino es USD
- ✅ `originalCurrency: "USD"` indica que la cuenta origen es USD
- ✅ Cuando ambos son "USD", el frontend mostrará el monto en USD directamente
- ✅ `amount: 100.00` es el monto real transferido en USD
- ✅ `exchangeRate` y `amountInArs` son SOLO informativos (para mostrar equivalente)

#### B) Transferencia USD → ARS (cuenta USD a cuenta ARS usando conversión)

```json
{
  "id": 124,
  "type": "expense",
  "description": "Transferencia a cuenta ARS",
  "amount": 10.00,
  "date": "2026-01-19T14:20:00Z",
  "from": "juan.arcash.usd",
  "to": "pedro.arcash",
  "currency": "ARS",
  "exchangeRate": 1100.00,
  "amountInArs": 11000.00,
  "originalCurrency": "USD",
  "status": "COMPLETED"
}
```

**Importante:** 
- ✅ `currency: "ARS"` indica que la cuenta destino es ARS
- ✅ `originalCurrency: "USD"` indica que la cuenta origen es USD
- ✅ El frontend mostrará el monto en ARS

#### C) Transferencia ARS → USD (cuenta ARS a cuenta USD usando conversión)

```json
{
  "id": 125,
  "type": "income",
  "description": "Transferencia desde cuenta ARS",
  "amount": 11000.00,
  "date": "2026-01-19T15:00:00Z",
  "from": "pedro.arcash",
  "to": "juan.arcash.usd",
  "currency": "USD",
  "exchangeRate": 1100.00,
  "amountInArs": 11000.00,
  "originalCurrency": "ARS",
  "status": "COMPLETED"
}
```

**Importante:** 
- ✅ `currency: "USD"` indica que la cuenta destino es USD
- ✅ `originalCurrency: "ARS"` indica que la cuenta origen es ARS
- ✅ El frontend mostrará el monto en ARS

### Cuando se recibe una transferencia en USD:

```json
{
  "id": 124,
  "type": "income",
  "description": "Transferencia USD de Pedro López",
  "amount": 25.00,
  "date": "2026-01-19T14:20:00Z",
  "from": "pedro.arcash.usd",
  "to": "juan.arcash.usd",
  "currency": "USD",
  "exchangeRate": 1100.00,
  "amountInArs": 27500.00,
  "originalCurrency": "USD",
  "status": "COMPLETED"
}
```

### Campos requeridos para transacciones USD:

| Campo | Tipo | Descripción | Obligatorio |
|-------|------|-------------|-------------|
| `currency` | string | "USD" o "ARS" - **Moneda de la cuenta DESTINO** | ✅ Sí |
| `exchangeRate` | number | Tipo de cambio USD/ARS al momento de la transacción | ✅ Sí (solo para USD) |
| `amountInArs` | number | Equivalente en pesos de la transacción | ✅ Sí (solo para USD) |
| `originalCurrency` | string | "USD" o "ARS" - **Moneda de la cuenta ORIGEN** | ✅ Sí |

### Lógica de Visualización en Frontend:

| Origen | Destino | `originalCurrency` | `currency` | Se muestra en | Descripción |
|--------|---------|-------------------|------------|---------------|-------------|
| USD | USD | "USD" | "USD" | **USD** | Transferencia directa USD → USD |
| USD | ARS | "USD" | "ARS" | **ARS** | Transferencia desde cuenta USD usando ARS |
| ARS | USD | "ARS" | "USD" | **ARS** | Transferencia a cuenta USD usando ARS |
| ARS | ARS | "ARS" | "ARS" | **ARS** | Transferencia en pesos |

---

## 3. Búsqueda de Cuentas

### GET `/transactions/search/{alias_o_cvu}`

**Debe incluir el campo `currency`:**

```json
{
  "idaccount": "account_usd_789",
  "alias": "maria.arcash.usd",
  "cvu": "0000003100000000000003",
  "currency": "USD",
  "user": {
    "nombre": "María",
    "apellido": "Pérez",
    "dni": "12345678"
  }
}
```

---

## 4. Compra de Dólares

### POST `/accounts/{accountArsId}/buy-usd/{accountUsdId}`

**Request:**
```json
{
  "amountArs": 18150.00
}
```

**Response:**
```json
{
  "success": true,
  "message": "Compra exitosa",
  "amountArs": 18150.00,
  "amountUsd": 15.00,
  "exchangeRate": 1100.00,
  "taxAmount": 2150.00,
  "taxPercentage": 3,
  "totalDebitado": 18150.00,
  "newBalanceArs": 31850.00,
  "newBalanceUsd": 115.00
}
```

**Importante:**
- ✅ La comisión es del **3%** sobre el monto en ARS
- ✅ Retornar el `exchangeRate` usado para la conversión

---

## 5. Venta de Dólares

### POST `/accounts/{accountUsdId}/sell-usd/{accountArsId}`

**Request:**
```json
{
  "amountUsd": 10.00
}
```

**Response:**
```json
{
  "success": true,
  "message": "Venta exitosa",
  "amountUsd": 10.00,
  "amountArs": 11000.00,
  "exchangeRate": 1100.00,
  "newBalanceUsd": 105.00,
  "newBalanceArs": 42850.00
}
```

**Importante:**
- ✅ Retornar el `exchangeRate` usado para la conversión

---

## 6. Endpoint de Transferencias

### POST `/transactions/transfer` o similar

**Request para transferencia USD → USD:**
```json
{
  "fromAccountId": "account_usd_456",
  "toAccountAlias": "maria.arcash.usd",
  "amount": 100.00,
  "currency": "USD",
  "description": "Transferencia a María"
}
```

**Backend debe:**
1. Verificar que `fromAccountId` corresponde a una cuenta USD
2. Buscar cuenta destino por `toAccountAlias`
3. Verificar que cuenta destino es USD
4. **SI AMBAS SON USD:**
   - Debitar `amount` de `fromAccount.balance` (cuenta USD)
   - Acreditar `amount` en `toAccount.balance` (cuenta USD)
   - NO aplicar conversión
5. Crear transacción con:
   - `currency: "USD"`
   - `originalCurrency: "USD"`
   - `amount: 100.00` (monto en USD)
   - `exchangeRate: 1100.00` (solo informativo)
   - `amountInArs: 110000.00` (solo informativo)

**Email de confirmación debe mostrar:**
```
Monto transferido: US$ 100.00 USD
Transferencia directa USD → USD
Equivalente aproximado: $110,000 ARS
```

**❌ NO debe mostrar:**
```
Monto transferido: $110,000 ARS
Conversión aplicada: 100.00 ARS → .05 USD
```

---

## Resumen de Cambios Necesarios

### 🔴 CRÍTICO - Problema Actual:
El backend está:
- ❌ Debitando de cuenta ARS cuando debería debitar de cuenta USD
- ❌ Aplicando conversión en transferencias USD → USD
- ❌ Mostrando en emails que se envió ARS cuando se envió USD

### ✅ Prioridad ALTA
1. **FIX INMEDIATO**: Identificar correctamente el tipo de cuenta origen y destino
2. **FIX INMEDIATO**: Debitar de la cuenta correcta según su currency
3. Cada cuenta USD debe tener su propio `alias` y `cvu` diferentes a la cuenta ARS
4. Las transacciones en USD deben incluir: `currency`, `exchangeRate`, `amountInArs`, `originalCurrency`
5. La búsqueda de cuentas debe devolver el campo `currency`

### 📝 Pseudocódigo para el Backend:

```javascript
function transferir(fromAccountId, toAccountAliasOrCVU, amount, description) {
  // 1. Obtener cuenta origen
  const fromAccount = getAccountById(fromAccountId);
  
  // 2. Buscar cuenta destino
  const toAccount = searchAccount(toAccountAliasOrCVU);
  
  // 3. Determinar tipo de transferencia
  const fromCurrency = fromAccount.currency; // "ARS" o "USD"
  const toCurrency = toAccount.currency;     // "ARS" o "USD"
  
  // 4. Ejecutar según tipo
  if (fromCurrency === "USD" && toCurrency === "USD") {
    // ✅ TRANSFERENCIA DIRECTA USD → USD (SIN CONVERSIÓN)
    fromAccount.balance -= amount; // Debitar USD
    toAccount.balance += amount;   // Acreditar USD
    
    const transaction = {
      amount: amount,              // Monto en USD
      currency: "USD",
      originalCurrency: "USD",
      exchangeRate: getCurrentExchangeRate(), // Solo informativo
      amountInArs: amount * getCurrentExchangeRate(), // Solo informativo
      description: description
    };
    
    // Email debe mostrar: "Transferiste US$ 100.00 USD"
    
  } else if (fromCurrency === "USD" && toCurrency === "ARS") {
    // ✅ CONVERSIÓN USD → ARS
    const exchangeRate = getCurrentExchangeRate();
    const amountInArs = amount * exchangeRate;
    
    fromAccount.balance -= amount;     // Debitar USD
    toAccount.balance += amountInArs;  // Acreditar ARS
    
    const transaction = {
      amount: amount,              // Monto original en USD
      currency: "ARS",
      originalCurrency: "USD",
      exchangeRate: exchangeRate,
      amountInArs: amountInArs,
      description: description
    };
    
    // Email debe mostrar: "Conversión: US$ 100 USD → $110,000 ARS"
    
  } else if (fromCurrency === "ARS" && toCurrency === "USD") {
    // ✅ CONVERSIÓN ARS → USD
    const exchangeRate = getCurrentExchangeRate();
    const amountInUsd = amount / exchangeRate;
    
    fromAccount.balance -= amount;      // Debitar ARS
    toAccount.balance += amountInUsd;   // Acreditar USD
    
    const transaction = {
      amount: amount,              // Monto original en ARS
      currency: "USD",
      originalCurrency: "ARS",
      exchangeRate: exchangeRate,
      amountInArs: amount,
      description: description
    };
    
    // Email debe mostrar: "Conversión: $110,000 ARS → US$ 100 USD"
    
  } else {
    // ✅ TRANSFERENCIA NORMAL EN ARS
    fromAccount.balance -= amount;
    toAccount.balance += amount;
    
    const transaction = {
      amount: amount,
      currency: "ARS",
      originalCurrency: "ARS",
      description: description
    };
    
    // Email debe mostrar: "Transferiste $1,000 ARS"
  }
  
  saveAccounts(fromAccount, toAccount);
  saveTransaction(transaction);
  sendEmailConfirmation(transaction);
}
```

### ⚠️ Prioridad MEDIA
4. Los endpoints de compra/venta deben retornar el `exchangeRate` utilizado

---

## Ejemplo de Flujo Completo

### Usuario compra $10 USD con $11,330 ARS (3% comisión):

1. **Request:** POST `/accounts/ars_123/buy-usd/usd_456`
   ```json
   { "amountArs": 11330 }
   ```

2. **Backend calcula:**
   - Tipo de cambio: $1100 ARS/USD
   - Comisión 3%: $330 ARS
   - Total debitado: $11,330 ARS
   - USD recibidos: $10 USD

3. **Response:**
   ```json
   {
     "success": true,
     "amountUsd": 10.00,
     "exchangeRate": 1100.00,
     "taxPercentage": 3,
     "newBalanceArs": 38670.00,
     "newBalanceUsd": 110.00
   }
   ```

### Usuario transfiere $10 USD a otro usuario con cuenta USD:

1. **Transacción generada (USD → USD directa):**
   ```json
   {
     "id": 125,
     "type": "expense",
     "description": "Transferencia USD a Pedro López",
     "amount": 10.00,
     "currency": "USD",
     "exchangeRate": 1100.00,
     "amountInArs": 11000.00,
     "originalCurrency": "USD"
   }
   ```

2. **Frontend muestra:**
   - En la lista: **"$10.00 USD"** (monto original en dólares)
   - Al hacer click → Detalles:
     - **Tipo**: "Transferencia directa USD → USD"
     - **Monto Transferido**: $10.00 USD
     - **Equivalente aproximado**: $11,000 ARS (solo informativo)

### Usuario transfiere desde cuenta USD a cuenta ARS:

1. **Transacción generada (USD → ARS con conversión):**
   ```json
   {
     "id": 126,
     "type": "expense",
     "description": "Transferencia a cuenta ARS",
     "amount": 10.00,
     "currency": "ARS",
     "exchangeRate": 1100.00,
     "amountInArs": 11000.00,
     "originalCurrency": "USD"
   }
   ```

2. **Frontend muestra:**
   - En la lista: **"$11,000 ARS"** (monto convertido)
   - Al hacer click → Detalles:
     - **Tipo**: "Transferencia desde cuenta USD usando ARS"
     - **Monto en Pesos**: $11,000 ARS
     - **Monto Original (USD)**: $10.00 USD
     - **Tipo de Cambio**: $1,100 ARS/USD

---

## ✅ Lista de Verificación para el Backend

Use esta lista para verificar que la implementación es correcta:

### Transferencia USD → USD (100 USD):
- [ ] Se debita 100 USD de `cuenta_origen_usd.balance`
- [ ] Se acredita 100 USD a `cuenta_destino_usd.balance`
- [ ] NO se toca `cuenta_origen_ars.balance`
- [ ] NO se toca `cuenta_destino_ars.balance`
- [ ] Transacción: `amount: 100.00`
- [ ] Transacción: `currency: "USD"`
- [ ] Transacción: `originalCurrency: "USD"`
- [ ] Email muestra: "Transferiste US$ 100.00 USD"
- [ ] Email NO muestra conversión

### Transferencia USD → ARS (100 USD a $110,000 ARS):
- [ ] Se debita 100 USD de `cuenta_origen_usd.balance`
- [ ] Se acredita $110,000 ARS a `cuenta_destino_ars.balance`
- [ ] Transacción: `amount: 100.00` (USD)
- [ ] Transacción: `currency: "ARS"`
- [ ] Transacción: `originalCurrency: "USD"`
- [ ] Transacción: `amountInArs: 110000.00`
- [ ] Email muestra: "Conversión: 100 USD → $110,000 ARS"

### Transferencia ARS → USD ($110,000 ARS a 100 USD):
- [ ] Se debita $110,000 ARS de `cuenta_origen_ars.balance`
- [ ] Se acredita 100 USD a `cuenta_destino_usd.balance`
- [ ] Transacción: `amount: 110000.00` (ARS)
- [ ] Transacción: `currency: "USD"`
- [ ] Transacción: `originalCurrency: "ARS"`
- [ ] Transacción: `amountInArs: 110000.00`
- [ ] Email muestra: "Conversión: $110,000 ARS → 100 USD"

### Transferencia ARS → ARS ($1,000 ARS):
- [ ] Se debita $1,000 ARS de `cuenta_origen_ars.balance`
- [ ] Se acredita $1,000 ARS a `cuenta_destino_ars.balance`
- [ ] Transacción: `amount: 1000.00`
- [ ] Transacción: `currency: "ARS"`
- [ ] Transacción: `originalCurrency: "ARS"`
- [ ] Email muestra: "Transferiste $1,000 ARS"
