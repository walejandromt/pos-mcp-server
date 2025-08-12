# Herramientas de Finanzas Personales - MCP Server

Este proyecto implementa un conjunto completo de herramientas para que un agente de IA pueda administrar las finanzas personales de los usuarios. Las herramientas permiten consultar, registrar, actualizar y analizar información financiera individual.

## Servicios Implementados

### 1. Usuarios (UserTools)
- **obtenerUsuarioPorTelefono**: Obtiene información del usuario por número de teléfono
- **verificarExistenciaUsuario**: Verifica si existe un usuario con el número de teléfono
- **crearUsuario**: Crea un nuevo usuario con nombre, teléfono y moneda

### 2. Transacciones (TransactionTools)
- **registrarGasto**: Registra un gasto único con descripción, monto y fecha
- **registrarIngreso**: Registra un ingreso único con descripción, monto y fecha
- **listarTransacciones**: Lista transacciones en un rango de fechas
- **obtenerResumenGastos**: Calcula resumen de gastos e ingresos en un periodo
- **categorizarTransaccion**: Actualiza la categoría de una transacción existente

### 3. Transacciones Recurrentes (RecurringTransactionTools)
- **registrarIngresoRecurrente**: Registra un ingreso recurrente (sueldo, renta, etc.)
- **registrarGastoRecurrente**: Registra un gasto recurrente (suscripciones, MSI, etc.)
- **listarTransaccionesRecurrentes**: Lista todas las transacciones recurrentes
- **recordarPagosProximos**: Lista los pagos próximos programados

### 4. Préstamos (LoanTools)
- **registrarPrestamo**: Registra un préstamo con tasa de interés y pagos periódicos
- **listarPrestamos**: Lista todos los préstamos del usuario
- **calcularPlanPagoDeudas**: Calcula plan de pago con estrategias snowball/avalanche

### 5. Presupuestos (BudgetTools)
- **definirPresupuesto**: Define un presupuesto por categoría y periodo
- **verificarEstadoPresupuesto**: Verifica cuánto se ha gastado vs el límite
- **listarPresupuestos**: Lista todos los presupuestos del usuario
- **predecirGastos**: Predice gastos futuros basado en hábitos

### 6. Metas de Ahorro (SavingGoalTools)
- **crearMetaAhorro**: Crea una meta de ahorro con fecha y monto objetivo
- **seguimientoMetaAhorro**: Ver el progreso de una meta específica
- **listarMetasAhorro**: Lista todas las metas de ahorro
- **generarPlanAhorro**: Genera un plan de ahorro personalizado

### 7. Alertas (AlertTools)
- **crearAlerta**: Crea una alerta personalizada
- **listarAlertas**: Lista todas las alertas del usuario
- **alertarPresupuestoExcedido**: Alerta automática cuando se excede presupuesto
- **detectarGastoInusual**: Detecta gastos fuera del rango normal
- **sugerirOportunidadesAhorro**: Sugiere ajustes para ahorrar más

## Configuración

### URLs de APIs
Las herramientas consumen los servicios REST del proyecto `ai-finance-rest-api`. Las URLs se configuran en `application.properties`:

```properties
ai.finance.api.users.url=http://localhost:8080/api/users
ai.finance.api.transactions.url=http://localhost:8080/api/transactions
ai.finance.api.recurring-transactions.url=http://localhost:8080/api/recurring-transactions
ai.finance.api.loans.url=http://localhost:8080/api/loans
ai.finance.api.budgets.url=http://localhost:8080/api/budgets
ai.finance.api.saving-goals.url=http://localhost:8080/api/saving-goals
ai.finance.api.alerts.url=http://localhost:8080/api/alerts
```

## Uso del Agente de IA

El agente puede interactuar con los usuarios en lenguaje natural y llamar las herramientas correspondientes. Ejemplos de uso:

### Registro de Gastos
```
Usuario: "Gasté 500 pesos en comida ayer"
Agente: Llamará `registrarGasto` con los parámetros:
- numeroTelefono: [número del usuario]
- descripcion: "Comida"
- monto: 500
- fecha: [fecha de ayer]
- categoria: "Alimentación"
```

### Consulta de Presupuesto
```
Usuario: "¿Cuánto me queda del presupuesto de comida?"
Agente: Llamará `verificarEstadoPresupuesto` con:
- numeroTelefono: [número del usuario]
- categoria: "Alimentación"
```

### Creación de Meta de Ahorro
```
Usuario: "Quiero ahorrar 10,000 pesos para diciembre"
Agente: Llamará `crearMetaAhorro` con:
- numeroTelefono: [número del usuario]
- nombreMeta: "Ahorro diciembre"
- montoObjetivo: 10000
- fechaMeta: "2024-12-31"
```

## Características Principales

1. **Identificación por Teléfono**: Todas las herramientas requieren el número de teléfono para identificar al usuario
2. **Validación de Datos**: Validación completa de parámetros de entrada
3. **Manejo de Errores**: Respuestas informativas en caso de errores
4. **Flexibilidad de Fechas**: Soporte para múltiples formatos de fecha
5. **Análisis Inteligente**: Cálculos automáticos de progreso, predicciones y sugerencias
6. **Alertas Proactivas**: Sistema de alertas para mantener al usuario informado

## Arquitectura

- **Modelos**: Representan las entidades de respuesta de la API
- **Servicios**: Consumen las APIs REST y GraphQL
- **Herramientas**: Implementan la lógica de negocio y exponen las funciones al agente
- **Configuración**: Centraliza las URLs y configuración del sistema

El sistema está diseñado para ser escalable y fácil de mantener, permitiendo agregar nuevas funcionalidades según las necesidades del negocio.
