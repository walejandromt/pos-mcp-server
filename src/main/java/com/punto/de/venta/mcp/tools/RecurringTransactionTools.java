package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.RecurringTransaction;
import com.punto.de.venta.mcp.model.TransactionCategory;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.RecurringTransactionService;
import com.punto.de.venta.mcp.service.TransactionCategoryService;
import com.punto.de.venta.mcp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class RecurringTransactionTools {
    
    private final RecurringTransactionService recurringTransactionService;
    private final TransactionCategoryService transactionCategoryService;
    private final UserService userService;
    
    public RecurringTransactionTools(RecurringTransactionService recurringTransactionService, TransactionCategoryService transactionCategoryService, UserService userService) {
        this.recurringTransactionService = recurringTransactionService;
        this.transactionCategoryService = transactionCategoryService;
        this.userService = userService;
    }
    
    @Tool(name = "registrarIngresoRecurrente", description = "Registra un ingreso recurrente con descripción, monto, frecuencia y fecha de inicio. Requiere el número de teléfono del usuario.")
    public String registrarIngresoRecurrente(@ToolParam String numeroTelefono, @ToolParam String descripcion, 
                                            @ToolParam BigDecimal monto, @ToolParam String frecuencia, 
                                            @ToolParam String fechaInicio, @ToolParam String fechaFin, 
                                            @ToolParam String categoria) {
        log.info("Registrando ingreso recurrente para usuario: {} - Descripción: {}, Monto: {}, Frecuencia: {}", 
                numeroTelefono, descripcion, monto, frecuencia);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return "Error: La descripción no puede estar vacía";
        }
        
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto debe ser mayor a cero";
        }
        
        if (frecuencia == null || frecuencia.trim().isEmpty()) {
            return "Error: La frecuencia no puede estar vacía";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Crear transacción recurrente
            RecurringTransaction recurringTransaction = new RecurringTransaction();
            recurringTransaction.setUser(user);
            recurringTransaction.setType("INCOME");
            recurringTransaction.setDescription(descripcion.trim());
            recurringTransaction.setAmount(monto);
            recurringTransaction.setFrequency(parseFrequency(frecuencia));
            recurringTransaction.setStartDate(parseDate(fechaInicio));
            recurringTransaction.setEndDate(parseDate(fechaFin));
            // Obtener o crear categoría
            TransactionCategory category = getCategoryByName(categoria, user.getId());
            if (category == null) {
                return "Error: No se pudo crear o encontrar la categoría especificada";
            }
            recurringTransaction.setTransactionCategory(category);
            
            RecurringTransaction savedTransaction = recurringTransactionService.createRecurringTransaction(recurringTransaction);
            return String.format("Ingreso recurrente registrado exitosamente - ID: %s, Descripción: %s, Monto: %s %s, Frecuencia: %s", 
                savedTransaction.getId(), savedTransaction.getDescription(), 
                savedTransaction.getAmount(), user.getCurrency(), savedTransaction.getFrequency());
        } catch (Exception e) {
            log.error("Error al registrar ingreso recurrente", e);
            return "Error al registrar el ingreso recurrente: " + e.getMessage();
        }
    }
    
    @Tool(name = "registrarGastoRecurrente", description = "Registra un gasto recurrente como suscripciones, pagos MSI, créditos. Requiere el número de teléfono del usuario.")
    public String registrarGastoRecurrente(@ToolParam String numeroTelefono, @ToolParam String descripcion, 
                                          @ToolParam BigDecimal monto, @ToolParam String frecuencia, 
                                          @ToolParam String fechaInicio, @ToolParam String fechaFin, 
                                          @ToolParam String categoria) {
        log.info("Registrando gasto recurrente para usuario: {} - Descripción: {}, Monto: {}, Frecuencia: {}", 
                numeroTelefono, descripcion, monto, frecuencia);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return "Error: La descripción no puede estar vacía";
        }
        
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto debe ser mayor a cero";
        }
        
        if (frecuencia == null || frecuencia.trim().isEmpty()) {
            return "Error: La frecuencia no puede estar vacía";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Crear transacción recurrente
            RecurringTransaction recurringTransaction = new RecurringTransaction();
            recurringTransaction.setUser(user);
            recurringTransaction.setType("EXPENSE");
            recurringTransaction.setDescription(descripcion.trim());
            recurringTransaction.setAmount(monto);
            recurringTransaction.setFrequency(parseFrequency(frecuencia));
            recurringTransaction.setStartDate(parseDate(fechaInicio));
            recurringTransaction.setEndDate(parseDate(fechaFin));
            // Obtener o crear categoría
            TransactionCategory category = getCategoryByName(categoria, user.getId());
            if (category == null) {
                return "Error: No se pudo crear o encontrar la categoría especificada";
            }
            recurringTransaction.setTransactionCategory(category);
            
            RecurringTransaction savedTransaction = recurringTransactionService.createRecurringTransaction(recurringTransaction);
            return String.format("Gasto recurrente registrado exitosamente - ID: %s, Descripción: %s, Monto: %s %s, Frecuencia: %s", 
                savedTransaction.getId(), savedTransaction.getDescription(), 
                savedTransaction.getAmount(), user.getCurrency(), savedTransaction.getFrequency());
        } catch (Exception e) {
            log.error("Error al registrar gasto recurrente", e);
            return "Error al registrar el gasto recurrente: " + e.getMessage();
        }
    }
    
    @Tool(name = "listarTransaccionesRecurrentes", description = "Lista las transacciones recurrentes de un usuario. Requiere número de teléfono y opcionalmente el tipo.")
    public String listarTransaccionesRecurrentes(@ToolParam String numeroTelefono, @ToolParam String tipo) {
        log.info("Listando transacciones recurrentes para usuario: {} tipo: {}", numeroTelefono, tipo);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            List<RecurringTransaction> transactions;
            if (tipo != null && !tipo.trim().isEmpty()) {
                transactions = recurringTransactionService.getRecurringTransactionsByUserIdAndType(user.getId(), tipo.trim().toUpperCase());
            } else {
                transactions = recurringTransactionService.getRecurringTransactionsByUserId(user.getId());
            }
            
            if (transactions.isEmpty()) {
                return "No se encontraron transacciones recurrentes";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Transacciones recurrentes encontradas:\n");
            
            for (RecurringTransaction transaction : transactions) {
                result.append(String.format("- %s: %s (%s) - %s %s cada %s\n", 
                    transaction.getType(), transaction.getDescription(), 
                    transaction.getTransactionCategory() != null ? transaction.getTransactionCategory().getCategoryName() : "Sin categoría", 
                    transaction.getAmount(), user.getCurrency(), 
                    transaction.getFrequency()));
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al listar transacciones recurrentes", e);
            return "Error al listar las transacciones recurrentes: " + e.getMessage();
        }
    }
    
    @Tool(name = "recordarPagosProximos", description = "Lista los pagos próximos como MSI, suscripciones, préstamos. Requiere número de teléfono del usuario.")
    public String recordarPagosProximos(@ToolParam String numeroTelefono) {
        log.info("Recordando pagos próximos para usuario: {}", numeroTelefono);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener transacciones recurrentes de gastos
            List<RecurringTransaction> gastosRecurrentes = recurringTransactionService.getRecurringTransactionsByUserIdAndType(user.getId(), "EXPENSE");
            
            if (gastosRecurrentes.isEmpty()) {
                return "No tienes pagos recurrentes programados";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Pagos recurrentes próximos:\n");
            
            for (RecurringTransaction transaction : gastosRecurrentes) {
                result.append(String.format("- %s: %s %s cada %s\n", 
                    transaction.getDescription(), transaction.getAmount(), user.getCurrency(), 
                    transaction.getFrequency()));
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al recordar pagos próximos", e);
            return "Error al obtener los pagos próximos: " + e.getMessage();
        }
    }
    
    private String parseFrequency(String frequency) {
        if (frequency == null || frequency.trim().isEmpty()) {
            return "MONTHLY";
        }
        
        String freq = frequency.trim().toUpperCase();
        switch (freq) {
            case "DIARIO":
            case "DAILY":
                return "DAILY";
            case "SEMANAL":
            case "WEEKLY":
                return "WEEKLY";
            case "MENSUAL":
            case "MONTHLY":
                return "MONTHLY";
            case "ANUAL":
            case "YEARLY":
                return "YEARLY";
            default:
                return "MONTHLY";
        }
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now();
        }
        
        try {
            // Intentar diferentes formatos de fecha
            String[] formats = {"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy"};
            for (String format : formats) {
                try {
                    return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern(format));
                } catch (Exception ignored) {
                    // Continuar con el siguiente formato
                }
            }
            // Si no se puede parsear, usar fecha actual
            return LocalDate.now();
        } catch (Exception e) {
            log.warn("No se pudo parsear la fecha: {}, usando fecha actual", dateStr);
            return LocalDate.now();
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
}
