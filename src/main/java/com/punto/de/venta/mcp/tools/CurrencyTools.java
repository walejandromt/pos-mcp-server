package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.Transaction;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.TransactionService;
import com.punto.de.venta.mcp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CurrencyTools {
    
    private final TransactionService transactionService;
    private final UserService userService;
    private final RestTemplate restTemplate;
    
    private static final String RATES_API_URL = "https://ratesdb.com/api/v1/rates";
    
    public CurrencyTools(TransactionService transactionService, UserService userService, RestTemplate restTemplate) {
        this.transactionService = transactionService;
        this.userService = userService;
        this.restTemplate = restTemplate;
    }
    
    @Tool(name = "currencyConversion", description = "Convierte montos entre diferentes monedas usando tasas actuales. Requiere monto, moneda origen y moneda destino.")
    public String currencyConversion(@ToolParam BigDecimal monto, @ToolParam String monedaOrigen, 
                                   @ToolParam String monedaDestino) {
        log.info("Convirtiendo moneda: {} {} a {}", monto, monedaOrigen, monedaDestino);
        
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto debe ser mayor a cero";
        }
        
        if (monedaOrigen == null || monedaOrigen.trim().isEmpty()) {
            return "Error: La moneda origen no puede estar vacía";
        }
        
        if (monedaDestino == null || monedaDestino.trim().isEmpty()) {
            return "Error: La moneda destino no puede estar vacía";
        }
        
        try {
            // Obtener tasa de cambio
            BigDecimal tasaCambio = obtenerTasaCambio(monedaOrigen.trim().toUpperCase(), 
                                                    monedaDestino.trim().toUpperCase());
            
            if (tasaCambio == null) {
                return "Error: No se pudo obtener la tasa de cambio para " + monedaOrigen + " a " + monedaDestino;
            }
            
            // Calcular conversión
            BigDecimal montoConvertido = monto.multiply(tasaCambio).setScale(2, RoundingMode.HALF_UP);
            
            return String.format("Conversión: %s %s = %s %s (Tasa: 1 %s = %s %s)", 
                monto, monedaOrigen.toUpperCase(), montoConvertido, monedaDestino.toUpperCase(),
                monedaOrigen.toUpperCase(), tasaCambio, monedaDestino.toUpperCase());
        } catch (Exception e) {
            log.error("Error al convertir moneda", e);
            return "Error al convertir la moneda: " + e.getMessage();
        }
    }
    
    @Tool(name = "registrarTransaccionMonedaExtranjera", description = "Registra una transacción en moneda extranjera y la convierte a la moneda local del usuario. Requiere número de teléfono, descripción, monto, moneda extranjera y fecha.")
    public String registrarTransaccionMonedaExtranjera(@ToolParam String numeroTelefono, 
                                                      @ToolParam String descripcion, 
                                                      @ToolParam BigDecimal monto, 
                                                      @ToolParam String monedaExtranjera, 
                                                      @ToolParam String fecha, 
                                                      @ToolParam String tipo) {
        log.info("Registrando transacción en moneda extranjera para usuario: {} - {} {} {} - {}", 
                numeroTelefono, descripcion, monto, monedaExtranjera, tipo);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return "Error: La descripción no puede estar vacía";
        }
        
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto debe ser mayor a cero";
        }
        
        if (monedaExtranjera == null || monedaExtranjera.trim().isEmpty()) {
            return "Error: La moneda extranjera no puede estar vacía";
        }
        
        if (tipo == null || (!"EXPENSE".equals(tipo.toUpperCase()) && !"INCOME".equals(tipo.toUpperCase()))) {
            return "Error: El tipo debe ser 'EXPENSE' o 'INCOME'";
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            String monedaLocal = user.getCurrency();
            
            // Convertir a moneda local
            BigDecimal tasaCambio = obtenerTasaCambio(monedaExtranjera.trim().toUpperCase(), 
                                                    monedaLocal.toUpperCase());
            
            if (tasaCambio == null) {
                return "Error: No se pudo obtener la tasa de cambio para " + monedaExtranjera + " a " + monedaLocal;
            }
            
            BigDecimal montoConvertido = monto.multiply(tasaCambio).setScale(2, RoundingMode.HALF_UP);
            
            // Crear transacción
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setType(tipo.toUpperCase());
            transaction.setDescription(descripcion.trim() + " (" + monto + " " + monedaExtranjera.toUpperCase() + ")");
            transaction.setAmount(montoConvertido);
            transaction.setCategory("Moneda Extranjera");
            transaction.setTransactionDate(parseDate(fecha));
            transaction.setSource("MANUAL");
            
            Transaction savedTransaction = transactionService.createTransaction(transaction);
            
            return String.format("Transacción registrada exitosamente:\n" +
                "ID: %s\n" +
                "Descripción: %s\n" +
                "Monto original: %s %s\n" +
                "Monto convertido: %s %s\n" +
                "Tasa de cambio: 1 %s = %s %s", 
                savedTransaction.getId(), savedTransaction.getDescription(),
                monto, monedaExtranjera.toUpperCase(),
                montoConvertido, monedaLocal,
                monedaExtranjera.toUpperCase(), tasaCambio, monedaLocal);
        } catch (Exception e) {
            log.error("Error al registrar transacción en moneda extranjera", e);
            return "Error al registrar la transacción: " + e.getMessage();
        }
    }
    
    @Tool(name = "obtenerTasasCambio", description = "Obtiene las tasas de cambio actuales para las principales monedas. No requiere parámetros.")
    public String obtenerTasasCambio() {
        log.info("Obteniendo tasas de cambio actuales");
        
        try {
            StringBuilder result = new StringBuilder();
            result.append("💱 TASAS DE CAMBIO ACTUALES\n");
            result.append("==========================\n\n");
            
            // Monedas principales
            String[] monedas = {"USD", "EUR", "MXN", "CAD", "GBP", "JPY"};
            
            for (String moneda : monedas) {
                if (!"USD".equals(moneda)) {
                    BigDecimal tasa = obtenerTasaCambio("USD", moneda);
                    if (tasa != null) {
                        result.append(String.format("1 USD = %s %s\n", tasa, moneda));
                    }
                }
            }
            
            result.append("\n💡 Nota: Las tasas se actualizan en tiempo real desde ratesdb.com");
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al obtener tasas de cambio", e);
            return "Error al obtener las tasas de cambio: " + e.getMessage();
        }
    }
    
    @Tool(name = "analizarGastosMonedaExtranjera", description = "Analiza los gastos en moneda extranjera de un usuario. Requiere el número de teléfono del usuario.")
    public String analizarGastosMonedaExtranjera(@ToolParam String numeroTelefono) {
        log.info("Analizando gastos en moneda extranjera para usuario: {}", numeroTelefono);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener transacciones de moneda extranjera
            List<Transaction> transacciones = transactionService.getTransactionsByUserId(user.getId());
            
            List<Transaction> transaccionesMonedaExtranjera = transacciones.stream()
                .filter(t -> "Moneda Extranjera".equals(t.getCategory()))
                .collect(java.util.stream.Collectors.toList());
            
            if (transaccionesMonedaExtranjera.isEmpty()) {
                return "No tienes transacciones en moneda extranjera registradas";
            }
            
            BigDecimal totalMonedaExtranjera = transaccionesMonedaExtranjera.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            StringBuilder result = new StringBuilder();
            result.append("🌍 ANÁLISIS DE GASTOS EN MONEDA EXTRANJERA\n");
            result.append("========================================\n\n");
            result.append(String.format("Total gastos en moneda extranjera: %s %s\n", 
                totalMonedaExtranjera, user.getCurrency()));
            result.append(String.format("Número de transacciones: %d\n\n", transaccionesMonedaExtranjera.size()));
            
            result.append("📋 ÚLTIMAS TRANSACCIONES:\n");
            result.append("------------------------\n");
            
            transaccionesMonedaExtranjera.stream()
                .limit(10)
                .forEach(t -> {
                    result.append(String.format("• %s: %s %s (%s)\n", 
                        t.getTransactionDate(), t.getAmount(), user.getCurrency(), t.getDescription()));
                });
            
            if (transaccionesMonedaExtranjera.size() > 10) {
                result.append(String.format("\n... y %d transacciones más", transaccionesMonedaExtranjera.size() - 10));
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al analizar gastos en moneda extranjera", e);
            return "Error al analizar los gastos en moneda extranjera: " + e.getMessage();
        }
    }
    
    private BigDecimal obtenerTasaCambio(String monedaOrigen, String monedaDestino) {
        try {
            // Simulación de llamada a la API (en producción usarías la API real)
            // Por ahora usamos tasas simuladas para demostración
            
            if (monedaOrigen.equals(monedaDestino)) {
                return BigDecimal.ONE;
            }
            
            // Tasas simuladas (en producción estas vendrían de la API)
            java.util.Map<String, BigDecimal> tasas = new java.util.HashMap<>();
            tasas.put("USD_EUR", BigDecimal.valueOf(0.85));
            tasas.put("USD_MXN", BigDecimal.valueOf(18.50));
            tasas.put("USD_CAD", BigDecimal.valueOf(1.35));
            tasas.put("USD_GBP", BigDecimal.valueOf(0.73));
            tasas.put("USD_JPY", BigDecimal.valueOf(110.50));
            tasas.put("EUR_USD", BigDecimal.valueOf(1.18));
            tasas.put("EUR_MXN", BigDecimal.valueOf(21.80));
            tasas.put("MXN_USD", BigDecimal.valueOf(0.054));
            tasas.put("MXN_EUR", BigDecimal.valueOf(0.046));
            
            String clave = monedaOrigen + "_" + monedaDestino;
            BigDecimal tasa = tasas.get(clave);
            
            if (tasa == null) {
                // Si no existe la tasa directa, intentar con la inversa
                String claveInversa = monedaDestino + "_" + monedaOrigen;
                BigDecimal tasaInversa = tasas.get(claveInversa);
                if (tasaInversa != null) {
                    tasa = BigDecimal.ONE.divide(tasaInversa, 4, RoundingMode.HALF_UP);
                }
            }
            
            return tasa;
        } catch (Exception e) {
            log.error("Error al obtener tasa de cambio", e);
            return null;
        }
    }
    
    private java.time.LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return java.time.LocalDate.now();
        }
        
        try {
            // Intentar diferentes formatos de fecha
            String[] formats = {"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy"};
            for (String format : formats) {
                try {
                    return java.time.LocalDate.parse(dateStr.trim(), java.time.format.DateTimeFormatter.ofPattern(format));
                } catch (Exception ignored) {
                    // Continuar con el siguiente formato
                }
            }
            // Si no se puede parsear, usar fecha actual
            return java.time.LocalDate.now();
        } catch (Exception e) {
            log.warn("No se pudo parsear la fecha: {}, usando fecha actual", dateStr);
            return java.time.LocalDate.now();
        }
    }
}
