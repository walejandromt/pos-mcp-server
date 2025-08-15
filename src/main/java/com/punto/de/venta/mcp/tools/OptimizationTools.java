package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.Transaction;
import com.punto.de.venta.mcp.model.TransactionCategory;
import com.punto.de.venta.mcp.model.RecurringTransaction;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.model.Loan;
import com.punto.de.venta.mcp.service.TransactionService;
import com.punto.de.venta.mcp.service.TransactionCategoryService;
import com.punto.de.venta.mcp.service.RecurringTransactionService;
import com.punto.de.venta.mcp.service.UserService;
import com.punto.de.venta.mcp.service.LoanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@Slf4j
public class OptimizationTools {
    
    private final TransactionService transactionService;
    private final TransactionCategoryService transactionCategoryService;
    private final RecurringTransactionService recurringTransactionService;
    private final UserService userService;
    private final LoanService loanService;
    
    public OptimizationTools(TransactionService transactionService, TransactionCategoryService transactionCategoryService, RecurringTransactionService recurringTransactionService, UserService userService, LoanService loanService) {
        this.transactionService = transactionService;
        this.transactionCategoryService = transactionCategoryService;
        this.recurringTransactionService = recurringTransactionService;
        this.userService = userService;
        this.loanService = loanService;
    }
    
    @Tool(name = "mergeSimilarTransactions", description = "Agrupa gastos similares para mejor visualización. Requiere el número de teléfono del usuario y palabras clave para agrupar.")
    public String mergeSimilarTransactions(@ToolParam String numeroTelefono, @ToolParam String palabrasClave, 
                                         @ToolParam String nuevaCategoria) {
        log.info("Agrupando transacciones similares para usuario: {} - Palabras clave: {}, Nueva categoría: {}", 
                numeroTelefono, palabrasClave, nuevaCategoria);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (palabrasClave == null || palabrasClave.trim().isEmpty()) {
            return "Error: Las palabras clave no pueden estar vacías";
        }
        
        if (nuevaCategoria == null || nuevaCategoria.trim().isEmpty()) {
            return "Error: La nueva categoría no puede estar vacía";
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener o crear categoría
            TransactionCategory category = getCategoryByName(nuevaCategoria, user.getId());
            if (category == null) {
                return "Error: No se pudo crear o encontrar la categoría especificada";
            }
            
            // Obtener transacciones del último año
            LocalDate fechaInicio = LocalDate.now().minusYears(1);
            LocalDate fechaFin = LocalDate.now();
            List<Transaction> transacciones = transactionService.getTransactionsByUserIdAndDateRange(
                user.getId(), fechaInicio, fechaFin);
            
            // Filtrar transacciones que contengan las palabras clave
            String[] keywords = palabrasClave.toLowerCase().split(",");
            List<Transaction> transaccionesSimilares = transacciones.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .filter(t -> {
                    String descripcion = t.getDescription().toLowerCase();
                    return java.util.Arrays.stream(keywords)
                        .anyMatch(keyword -> descripcion.contains(keyword.trim()));
                })
                .collect(Collectors.toList());
            
            if (transaccionesSimilares.isEmpty()) {
                return "No se encontraron transacciones que coincidan con las palabras clave: " + palabrasClave;
            }
            
            // Actualizar categorías
            int actualizadas = 0;
            BigDecimal totalAgrupado = BigDecimal.ZERO;
            
            for (Transaction transaction : transaccionesSimilares) {
                transaction.setTransactionCategory(category);
                transactionService.updateTransaction(transaction.getId(), transaction);
                actualizadas++;
                totalAgrupado = totalAgrupado.add(transaction.getAmount());
            }
            
            return String.format("Se agruparon %d transacciones bajo la categoría '%s'\n" +
                "Total agrupado: %s %s\n" +
                "Palabras clave utilizadas: %s", 
                actualizadas, nuevaCategoria, totalAgrupado, user.getCurrency(), palabrasClave);
        } catch (Exception e) {
            log.error("Error al agrupar transacciones similares", e);
            return "Error al agrupar las transacciones: " + e.getMessage();
        }
    }
    
    @Tool(name = "analyzeSubscriptions", description = "Detecta gastos recurrentes y suscripciones innecesarias. Requiere el número de teléfono del usuario.")
    public String analyzeSubscriptions(@ToolParam String numeroTelefono) {
        log.info("Analizando suscripciones para usuario: {}", numeroTelefono);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener transacciones recurrentes de gastos
            List<RecurringTransaction> gastosRecurrentes = recurringTransactionService.getRecurringTransactionsByUserIdAndType(
                user.getId(), "EXPENSE");
            
            if (gastosRecurrentes.isEmpty()) {
                return "No tienes gastos recurrentes registrados";
            }
            
            // Agrupar por categoría
            Map<String, List<RecurringTransaction>> gastosPorCategoria = gastosRecurrentes.stream()
                .collect(Collectors.groupingBy(t -> t.getTransactionCategory() != null ? 
                    t.getTransactionCategory().getCategoryName() : "Sin categoría"));
            
            StringBuilder result = new StringBuilder();
            result.append("📱 ANÁLISIS DE SUSCRIPCIONES\n");
            result.append("==========================\n\n");
            
            BigDecimal totalMensual = BigDecimal.ZERO;
            
            for (Map.Entry<String, List<RecurringTransaction>> entry : gastosPorCategoria.entrySet()) {
                String categoria = entry.getKey();
                List<RecurringTransaction> transacciones = entry.getValue();
                
                result.append(String.format("📂 %s:\n", categoria));
                
                for (RecurringTransaction transaction : transacciones) {
                    BigDecimal montoMensual = calcularMontoMensual(transaction);
                    totalMensual = totalMensual.add(montoMensual);
                    
                    result.append(String.format("  • %s: %s %s cada %s\n", 
                        transaction.getDescription(), montoMensual, user.getCurrency(), 
                        transaction.getFrequency()));
                }
                result.append("\n");
            }
            
            result.append(String.format("💰 Total mensual en suscripciones: %s %s\n", totalMensual, user.getCurrency()));
            result.append(String.format("💰 Total anual en suscripciones: %s %s\n\n", 
                totalMensual.multiply(BigDecimal.valueOf(12)), user.getCurrency()));
            
            // Recomendaciones
            result.append("💡 RECOMENDACIONES:\n");
            result.append("------------------\n");
            
            if (totalMensual.compareTo(BigDecimal.valueOf(1000)) > 0) {
                result.append("⚠️ Tus suscripciones representan un gasto significativo\n");
                result.append("• Revisa si realmente usas todos los servicios\n");
                result.append("• Considera cancelar las que no uses frecuentemente\n");
            } else if (totalMensual.compareTo(BigDecimal.valueOf(500)) > 0) {
                result.append("📊 Tus suscripciones están en un nivel moderado\n");
                result.append("• Revisa periódicamente si necesitas todos los servicios\n");
            } else {
                result.append("✅ Tus suscripciones están bien controladas\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al analizar suscripciones", e);
            return "Error al analizar las suscripciones: " + e.getMessage();
        }
    }
    
    @Tool(name = "autoCategorizeTransactions", description = "Clasifica automáticamente nuevos gastos según descripción. Requiere el número de teléfono del usuario.")
    public String autoCategorizeTransactions(@ToolParam String numeroTelefono) {
        log.info("Clasificando automáticamente transacciones para usuario: {}", numeroTelefono);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener transacciones sin categoría o con categoría "General"
            LocalDate fechaInicio = LocalDate.now().minusMonths(3);
            LocalDate fechaFin = LocalDate.now();
            List<Transaction> transacciones = transactionService.getTransactionsByUserIdAndDateRange(
                user.getId(), fechaInicio, fechaFin);
            
            List<Transaction> transaccionesSinCategoria = transacciones.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .filter(t -> t.getTransactionCategory() == null || 
                           (t.getTransactionCategory() != null && "General".equals(t.getTransactionCategory().getCategoryName())))
                .collect(Collectors.toList());
            
            if (transaccionesSinCategoria.isEmpty()) {
                return "No hay transacciones pendientes de categorización automática";
            }
            
            // Reglas de categorización automática
            Map<String, String> reglasCategorizacion = new HashMap<>();
            reglasCategorizacion.put("uber", "Transporte");
            reglasCategorizacion.put("didi", "Transporte");
            reglasCategorizacion.put("taxi", "Transporte");
            reglasCategorizacion.put("gasolina", "Transporte");
            reglasCategorizacion.put("estacionamiento", "Transporte");
            reglasCategorizacion.put("netflix", "Entretenimiento");
            reglasCategorizacion.put("spotify", "Entretenimiento");
            reglasCategorizacion.put("youtube", "Entretenimiento");
            reglasCategorizacion.put("amazon", "Compras");
            reglasCategorizacion.put("walmart", "Compras");
            reglasCategorizacion.put("soriana", "Compras");
            reglasCategorizacion.put("restaurante", "Comida");
            reglasCategorizacion.put("cafe", "Comida");
            reglasCategorizacion.put("starbucks", "Comida");
            reglasCategorizacion.put("mcdonalds", "Comida");
            reglasCategorizacion.put("banco", "Servicios");
            reglasCategorizacion.put("electricidad", "Servicios");
            reglasCategorizacion.put("agua", "Servicios");
            reglasCategorizacion.put("internet", "Servicios");
            reglasCategorizacion.put("telefono", "Servicios");
            reglasCategorizacion.put("farmacia", "Salud");
            reglasCategorizacion.put("medico", "Salud");
            reglasCategorizacion.put("hospital", "Salud");
            
            int categorizadas = 0;
            StringBuilder result = new StringBuilder();
            result.append("🤖 CATEGORIZACIÓN AUTOMÁTICA\n");
            result.append("==========================\n\n");
            
            for (Transaction transaction : transaccionesSinCategoria) {
                String descripcion = transaction.getDescription().toLowerCase();
                String nombreCategoriaAsignada = null;
                
                // Buscar coincidencias en las reglas
                for (Map.Entry<String, String> regla : reglasCategorizacion.entrySet()) {
                    if (descripcion.contains(regla.getKey())) {
                        nombreCategoriaAsignada = regla.getValue();
                        break;
                    }
                }
                
                if (nombreCategoriaAsignada != null) {
                    // Obtener o crear categoría
                    TransactionCategory category = getCategoryByName(nombreCategoriaAsignada, user.getId());
                    if (category != null) {
                        transaction.setTransactionCategory(category);
                        transactionService.updateTransaction(transaction.getId(), transaction);
                        categorizadas++;
                        
                        result.append(String.format("✅ %s → %s\n", 
                            transaction.getDescription(), nombreCategoriaAsignada));
                    }
                }
            }
            
            result.append(String.format("\n📊 Resumen: %d de %d transacciones categorizadas automáticamente", 
                categorizadas, transaccionesSinCategoria.size()));
            
            if (categorizadas < transaccionesSinCategoria.size()) {
                result.append("\n💡 Las transacciones no categorizadas pueden ser revisadas manualmente");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al categorizar automáticamente transacciones", e);
            return "Error al categorizar automáticamente las transacciones: " + e.getMessage();
        }
    }
    
    @Tool(name = "optimizeLoanPayments", description = "Recomienda refinanciar si se detectan mejores tasas. Requiere el número de teléfono del usuario.")
    public String optimizeLoanPayments(@ToolParam String numeroTelefono, @ToolParam BigDecimal tasaReferencia) {
        log.info("Optimizando pagos de préstamos para usuario: {} - Tasa referencia: {}", numeroTelefono, tasaReferencia);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (tasaReferencia == null || tasaReferencia.compareTo(BigDecimal.ZERO) < 0) {
            tasaReferencia = BigDecimal.valueOf(15); // Tasa de referencia por defecto
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener préstamos del usuario
            List<Loan> prestamos = loanService.getLoansByUserId(user.getId());
            
            if (prestamos.isEmpty()) {
                return "No tienes préstamos registrados para optimizar";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🏦 ANÁLISIS DE OPTIMIZACIÓN DE PRÉSTAMOS\n");
            result.append("=====================================\n\n");
            result.append(String.format("Tasa de referencia: %s%%\n\n", tasaReferencia));
            
            boolean hayOportunidades = false;
            
            for (Loan prestamo : prestamos) {
                BigDecimal tasaActual = prestamo.getInterestRate();
                BigDecimal diferencia = tasaActual.subtract(tasaReferencia);
                
                result.append(String.format("📋 %s:\n", prestamo.getDescription()));
                result.append(String.format("  • Monto: %s %s\n", prestamo.getPrincipal(), user.getCurrency()));
                result.append(String.format("  • Tasa actual: %s%%\n", tasaActual));
                result.append(String.format("  • Pago mensual: %s %s\n", prestamo.getMonthlyPayment(), user.getCurrency()));
                
                if (diferencia.compareTo(BigDecimal.valueOf(5)) > 0) {
                    hayOportunidades = true;
                    result.append(String.format("  ⚠️ Tasa %s%% mayor que la referencia\n", diferencia));
                    result.append("  💡 Considera refinanciar este préstamo\n");
                } else if (diferencia.compareTo(BigDecimal.valueOf(2)) > 0) {
                    result.append(String.format("  📊 Tasa %s%% mayor que la referencia\n", diferencia));
                    result.append("  💭 Podrías considerar refinanciar\n");
                } else {
                    result.append("  ✅ Tasa competitiva\n");
                }
                result.append("\n");
            }
            
            if (hayOportunidades) {
                result.append("🎯 RECOMENDACIONES:\n");
                result.append("------------------\n");
                result.append("• Contacta a diferentes instituciones financieras\n");
                result.append("• Compara tasas y condiciones\n");
                result.append("• Considera los costos de refinanciamiento\n");
                result.append("• Evalúa si el ahorro justifica el cambio\n");
            } else {
                result.append("✅ Tus préstamos tienen tasas competitivas\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al optimizar pagos de préstamos", e);
            return "Error al optimizar los pagos de préstamos: " + e.getMessage();
        }
    }
    
    private TransactionCategory getCategoryByName(String nombreCategoria, Long userId) {
        if (nombreCategoria == null || nombreCategoria.trim().isEmpty()) {
            nombreCategoria = "General";
        }
        
        try {
            // Buscar categoría existente por nombre y usuario
            List<TransactionCategory> categorias = transactionCategoryService.searchTransactionCategoriesByUserIdAndCategoryName(
                userId, nombreCategoria.trim());
            
            if (!categorias.isEmpty()) {
                return categorias.get(0);
            }
            
            // Si no existe, crear nueva categoría
            TransactionCategory nuevaCategoria = new TransactionCategory();
            User user = new User();
            user.setId(userId);
            nuevaCategoria.setUser(user);
            nuevaCategoria.setCategoryName(nombreCategoria.trim());
            
            TransactionCategory categoriaCreada = transactionCategoryService.createTransactionCategory(nuevaCategoria);
            return categoriaCreada;
        } catch (Exception e) {
            log.error("Error al obtener/crear categoría: {}", nombreCategoria, e);
            // Fallback: buscar categoría "General" o crear una
            try {
                List<TransactionCategory> generalCategories = transactionCategoryService.searchTransactionCategoriesByUserIdAndCategoryName(
                    userId, "General");
                if (!generalCategories.isEmpty()) {
                    return generalCategories.get(0);
                }
                
                // Crear categoría General como último recurso
                TransactionCategory generalCategory = new TransactionCategory();
                User user = new User();
                user.setId(userId);
                generalCategory.setUser(user);
                generalCategory.setCategoryName("General");
                
                TransactionCategory categoriaCreada = transactionCategoryService.createTransactionCategory(generalCategory);
                return categoriaCreada;
            } catch (Exception fallbackException) {
                log.error("Error al crear categoría de fallback", fallbackException);
                return null;
            }
        }
    }
    
    private BigDecimal calcularMontoMensual(RecurringTransaction transaction) {
        BigDecimal monto = transaction.getAmount();
        String frecuencia = transaction.getFrequency();
        
        switch (frecuencia.toUpperCase()) {
            case "DAILY":
                return monto.multiply(BigDecimal.valueOf(30));
            case "WEEKLY":
                return monto.multiply(BigDecimal.valueOf(4.33)); // Promedio semanal
            case "MONTHLY":
                return monto;
            case "YEARLY":
                return monto.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
            default:
                return monto;
        }
    }
}
