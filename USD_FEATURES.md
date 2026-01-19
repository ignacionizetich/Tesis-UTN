# Funcionalidades de Cuentas en Dólares (USD)

## Resumen de Implementación

Se han implementado las siguientes funcionalidades para el manejo de cuentas en dólares:

### 1. Compra de Dólares desde Cuenta en Pesos

**Endpoint:** `POST /api/accounts/{accountArsId}/buy-usd/{accountUsdId}`

**Descripción:** Permite a un usuario comprar dólares desde su cuenta en pesos. La conversión se realiza utilizando la cotización oficial del dólar en el momento de la transacción.

**Parámetros:**
- `accountArsId`: ID de la cuenta en pesos (origen)
- `accountUsdId`: ID de la cuenta en dólares (destino)

**Body:**
```json
{
  "amountArs": 10000.00
}
```

**Respuesta exitosa:**
```json
{
  "success": true,
  "message": "Compra de dólares exitosa",
  "amountArs": 10000.00,
  "amountUsd": 10.00,
  "exchangeRate": 1000.00,
  "newBalanceArs": 5000.00,
  "newBalanceUsd": 10.00
}
```

**Validaciones:**
- Ambas cuentas deben pertenecer al mismo usuario
- La cuenta origen debe ser ARS y la destino USD
- Debe haber saldo suficiente en la cuenta en pesos
- Requiere autenticación con JWT

---

### 2. Transferencias entre Cuentas de Diferentes Monedas

**Endpoint:** `POST /api/transactions/{id1}/transfer/{id2}` (existente, ahora mejorado)

**Descripción:** El endpoint de transferencias ahora soporta automáticamente la conversión de moneda cuando se transfiere desde una cuenta ARS a una cuenta USD.

**Comportamiento:**

#### Caso 1: Transferencia entre cuentas de la misma moneda (ARS→ARS o USD→USD)
- Se transfiere el monto exacto sin conversión
- El receptor recibe exactamente lo que el emisor envía

#### Caso 2: Transferencia con conversión (ARS→USD)
- El emisor especifica el monto en **pesos** que desea enviar
- El sistema convierte automáticamente a dólares usando la cotización oficial
- El receptor recibe **dólares** en su cuenta USD
- Se registra tanto el monto original (ARS) como el monto convertido (USD)
- Se guarda la tasa de cambio aplicada

**Ejemplo de transferencia con conversión:**

Body:
```json
{
  "balance": 5000.00
}
```

Si el remitente envía desde cuenta ARS a cuenta USD:
- Se debitan **$5,000 ARS** de la cuenta origen
- Se acreditan **$5.00 USD** en la cuenta destino (si la cotización es $1000/USD)
- La transacción registra ambos montos y la tasa de cambio

---

### 3. Validaciones Implementadas

#### En compra de dólares:
- ✅ Validación de autenticación (JWT)
- ✅ Validación de propiedad de cuentas
- ✅ Validación de tipos de cuenta (ARS→USD)
- ✅ Validación de saldo suficiente
- ✅ Validación de mismo usuario

#### En transferencias:
- ✅ Solo se permite conversión de ARS a USD (no USD a ARS)
- ✅ Transferencias USD→USD se realizan sin conversión
- ✅ Transferencias ARS→ARS se realizan sin conversión
- ✅ Se registra información completa de la conversión

---

### 4. Información de Transacciones

Todas las transacciones ahora incluyen información adicional sobre moneda y conversión:

**Campos nuevos en TransactionDTO:**
```json
{
  "idTransaction": 123,
  "amount": 5.00,
  "currency": "USD",
  "originalAmount": 5000.00,
  "originalCurrency": "ARS",
  "exchangeRate": 1000.00,
  "converted": true,
  "state": "COMPLETED",
  ...
}
```

**Campos:**
- `currency`: Moneda final de la transacción (la que recibe el destinatario)
- `originalAmount`: Monto original antes de conversión (si hubo conversión)
- `originalCurrency`: Moneda original (si hubo conversión)
- `exchangeRate`: Tasa de cambio aplicada (si hubo conversión)
- `converted`: Booleano que indica si hubo conversión

---

### 5. Flujo Completo de Uso

#### Escenario: Usuario quiere enviar dinero a una cuenta en dólares

1. **El usuario tiene cuenta en pesos con $10,000 ARS**
2. **Quiere transferir a una cuenta USD de otro usuario**

**Opción A: Transferencia directa con conversión automática**
```
POST /api/transactions/{idCuentaPesos}/transfer/{idCuentaUsdDestino}
Body: { "balance": 10000.00 }

Resultado:
- Se debitan $10,000 ARS de la cuenta origen
- Se acreditan $10.00 USD en la cuenta destino (cotización $1000/USD)
- Al receptor le aparece: "Has recibido $10.00 USD"
```

**Opción B: Primero comprar dólares, luego transferir**
```
1. POST /api/accounts/{idCuentaPesos}/buy-usd/{idCuentaUsdPropia}
   Body: { "amountArs": 10000.00 }
   Resultado: Ahora tienes $10.00 USD en tu cuenta USD

2. POST /api/transactions/{idCuentaUsdPropia}/transfer/{idCuentaUsdDestino}
   Body: { "balance": 10.00 }
   Resultado: Transfieres $10.00 USD directamente
```

---

### 6. Experiencia del Usuario en Frontend

**Para el remitente:**
- Al seleccionar una cuenta destino USD, se debe mostrar:
  - "El destinatario recibirá la transferencia en dólares"
  - "Se aplicará la cotización oficial del momento: $X.XX/USD"
  - "Si envías $5,000 ARS, el destinatario recibirá $5.00 USD"

**Para el receptor:**
- En el historial de transacciones:
  - Si `converted === true`: "Recibiste $X.XX USD (convertido desde $Y.YY ARS a tasa $Z.ZZ)"
  - Si `converted === false`: "Recibiste $X.XX [currency]"

---

### 7. Cotización del Dólar

- Se utiliza la API oficial: `https://dolarapi.com/v1/dolares/oficial`
- La cotización se actualiza automáticamente cada 10 minutos
- Se usa el precio de **venta** para todas las conversiones
- El servicio `CotizationUsdService` gestiona el cache de la cotización

---

## Cambios en la Base de Datos

### Tabla `transactions` - Nuevas columnas:

```sql
currency VARCHAR(10)           -- Moneda final (ARS/USD)
original_amount DOUBLE         -- Monto original antes de conversión
original_currency VARCHAR(10)  -- Moneda original (si hubo conversión)
exchange_rate DOUBLE           -- Tasa de cambio aplicada
```

**Nota:** Ejecuta las migraciones correspondientes para agregar estas columnas a tu base de datos.

---

## Testing

### Casos de prueba sugeridos:

1. ✅ Comprar dólares con saldo suficiente
2. ✅ Comprar dólares con saldo insuficiente (debe fallar)
3. ✅ Transferir ARS→USD (debe convertir)
4. ✅ Transferir USD→USD (no debe convertir)
5. ✅ Transferir ARS→ARS (no debe convertir)
6. ✅ Intentar transferir USD→ARS (debe rechazar)
7. ✅ Verificar que la tasa de cambio se registre correctamente
8. ✅ Verificar que el historial muestre la información de conversión

---

## Ejemplos de Uso con cURL

### Comprar dólares:
```bash
curl -X POST http://localhost:8080/api/accounts/1/buy-usd/2 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"amountArs": 10000.00}'
```

### Transferir con conversión automática:
```bash
curl -X POST http://localhost:8080/api/transactions/1/transfer/2 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"balance": 5000.00}'
```

---

## Notas Adicionales

- Las conversiones solo se permiten de ARS a USD, no en sentido inverso
- Todas las operaciones son transaccionales (si falla algo, se hace rollback completo)
- Se registra un historial completo con información de conversión
- Los eventos de transacción incluyen información sobre la conversión cuando aplica
