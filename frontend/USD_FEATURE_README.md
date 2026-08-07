# Funcionalidad de Cuenta en Dólares (USD)

## Descripción
Se ha implementado la funcionalidad completa para abrir una cuenta en dólares y comprar USD desde pesos (ARS) en la aplicación ArCash.

## Nuevos Componentes

### 1. Servicio de Cuentas (AccountService)
**Ubicación:** `src/app/services/account-service/account.service.ts`

**Funcionalidades:**
- `openUsdAccount()`: Abre una cuenta en dólares para el usuario autenticado
- `buyUsd()`: Compra dólares desde una cuenta ARS a una cuenta USD
- `getUserAccounts()`: Obtiene las cuentas del usuario (ARS y USD)

**Interfaces:**
- `Account`: Representa una cuenta (ARS o USD)
- `BuyUsdRequest`: Request para comprar dólares
- `BuyUsdResponse`: Response con detalles de la compra

### 2. Modales en Dashboard

#### Modal de Cuenta USD (`showUsdAccountModal`)
**Características:**
- Vista para crear cuenta en dólares si no existe
- Muestra beneficios de la cuenta USD
- Vista de cuenta creada con saldo disponible
- Botón para comprar dólares

#### Modal de Compra de Dólares (`showBuyUsdModal`)
**Características:**
- Muestra saldo en ARS y USD
- Input para ingresar monto en pesos a convertir
- Advertencia sobre impuestos (30% PAIS + 35% Ganancias)
- Confirmación de compra con detalles completos

### 3. Nueva Tarjeta de Acción
En la sección "Acciones Rápidas" del dashboard se agregó:
- **Tarjeta "Cuenta en USD"**: Con estilo verde distintivo
- Ícono de dólar
- Al hacer clic abre el modal de cuenta USD

## Estilos

### Archivo CSS
**Ubicación:** `src/app/pages/dashboard/dashboard-usd.css`

**Características:**
- Paleta de colores verde (#2E7D32 a #4CAF50) para identificación USD
- Gradientes para tarjetas y modales
- Animaciones hover consistentes con el diseño existente
- Responsive design
- Soporte para tema claro y oscuro

### Elementos Estilizados
- `.action-card.usd-card`: Tarjeta principal con gradiente verde
- `.usd-account-create`: Modal de creación de cuenta
- `.usd-account-details`: Vista de cuenta creada
- `.buy-usd-modal`: Modal de compra de dólares
- `.usd-balance-card`: Tarjeta de saldo en dólares
- `.benefit-item`: Items de beneficios con animación
- `.buy-warning`: Advertencia de impuestos

## Flujo de Usuario

### Abrir Cuenta en Dólares
1. Usuario hace clic en "Cuenta en USD" en acciones rápidas
2. Se abre modal mostrando beneficios
3. Usuario hace clic en "Abrir cuenta en dólares"
4. Sistema crea la cuenta y muestra confirmación
5. Modal se actualiza mostrando saldo USD (inicialmente $0)

### Comprar Dólares
1. Usuario hace clic en "Comprar dólares" (en modal USD o tarjeta)
2. Se abre modal de compra mostrando:
   - Saldo actual en ARS
   - Saldo actual en USD
   - Input para monto en pesos
   - Advertencia de impuestos
3. Usuario ingresa monto y confirma
4. Sistema procesa la compra aplicando impuestos
5. Se muestra confirmación con detalles:
   - Monto en ARS debitado
   - Monto en USD acreditado
   - Tipo de cambio usado
   - Impuestos aplicados
   - Nuevos saldos

## Integración con Backend

### Endpoints Utilizados
- `POST /account/usd` - Crear cuenta en dólares
- `POST /account/{accountArsId}/buy-usd/{accountUsdId}` - Comprar dólares

### Autenticación
- Usa token JWT del localStorage
- Validación de pertenencia de cuentas al usuario

## Características Técnicas

### Gestión de Estado
- Variables de estado para modales: `showUsdAccountModal`, `showBuyUsdModal`
- Variables de datos: `hasUsdAccount`, `usdAccountId`, `usdBalance`, `amountToBuyUsd`
- Estados de carga: `isCreatingUsdAccount`, `isBuyingUsd`

### Notificaciones
- Toast de éxito al crear cuenta
- Toast de éxito al comprar dólares
- Toast de error con mensajes descriptivos

### Validaciones
- Monto mayor a cero
- Saldo suficiente en ARS (incluye impuestos)
- Autenticación de usuario
- Pertenencia de cuentas

## Próximas Mejoras Sugeridas
- [ ] Agregar historial de compras de USD
- [ ] Mostrar gráfico de cotización histórica
- [ ] Permitir venta de dólares
- [ ] Agregar límites de compra diarios/mensuales
- [ ] Mostrar cotización en tiempo real
- [ ] Notificaciones de cambios en la cotización

## Pruebas Recomendadas
1. Crear cuenta USD con usuario sin cuenta USD
2. Intentar crear cuenta USD con usuario que ya tiene cuenta
3. Comprar USD con saldo suficiente
4. Comprar USD con saldo insuficiente
5. Verificar cálculo de impuestos
6. Verificar actualización de saldos
7. Probar en tema claro y oscuro
8. Probar en diferentes tamaños de pantalla
