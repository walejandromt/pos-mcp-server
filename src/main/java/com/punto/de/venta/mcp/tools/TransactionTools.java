package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.Transaction;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.TransactionService;
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
public class TransactionTools {
    
    private final TransactionService transactionService;
    private final UserService userService;
    
    public TransactionTools(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }
    
    @Tool(name = "registrarGasto", description = "Registra un gasto único con descripción, monto y fecha. Requiere el número de teléfono del usuario.")
    public String registrarGasto(@ToolParam String numeroTelefono, @ToolParam String descripcion, 
                                @ToolParam BigDecimal monto, @ToolParam String fecha, 
                                @ToolParam String categoria) {
        log.info("Registrando gasto para usuario: {} - Descripción: {}, Monto: {}, Fecha: {}", 
                numeroTelefono, descripcion, monto, fecha);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return "Error: La descripción no puede estar vacía";
        }
        
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto debe ser mayor a cero";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Crear transacción
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setType("EXPENSE");
            transaction.setDescription(descripcion.trim());
            transaction.setAmount(monto);
            transaction.setCategory(categoria != null ? categoria.trim() : "General");
            transaction.setTransactionDate(parseDate(fecha));
            transaction.setSource("MANUAL");
            
            Transaction savedTransaction = transactionService.createTransaction(transaction);
            return String.format("Gasto registrado exitosamente - ID: %s, Descripción: %s, Monto: %s %s, Fecha: %s", 
                savedTransaction.getId(), savedTransaction.getDescription(), 
                savedTransaction.getAmount(), user.getCurrency(), savedTransaction.getTransactionDate());
        } catch (Exception e) {
            log.error("Error al registrar gasto", e);
            return "Error al registrar el gasto: " + e.getMessage();
        }
    }
    
    @Tool(name = "registrarIngreso", description = "Registra un ingreso único con descripción, monto y fecha. Requiere el número de teléfono del usuario.")
    public String registrarIngreso(@ToolParam String numeroTelefono, @ToolParam String descripcion, 
                                  @ToolParam BigDecimal monto, @ToolParam String fecha, 
                                  @ToolParam String categoria) {
        log.info("Registrando ingreso para usuario: {} - Descripción: {}, Monto: {}, Fecha: {}", 
                numeroTelefono, descripcion, monto, fecha);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (descripcion == null || descripcion.trim().isEmpty()) {
            return "Error: La descripción no puede estar vacía";
        }
        
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto debe ser mayor a cero";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Crear transacción
            Transaction transaction = new Transaction();
            transaction.setUser(user);
            transaction.setType("INCOME");
            transaction.setDescription(descripcion.trim());
            transaction.setAmount(monto);
            transaction.setCategory(categoria != null ? categoria.trim() : "General");
            transaction.setTransactionDate(parseDate(fecha));
            transaction.setSource("MANUAL");
            
            Transaction savedTransaction = transactionService.createTransaction(transaction);
            return String.format("Ingreso registrado exitosamente - ID: %s, Descripción: %s, Monto: %s %s, Fecha: %s", 
                savedTransaction.getId(), savedTransaction.getDescription(), 
                savedTransaction.getAmount(), user.getCurrency(), savedTransaction.getTransactionDate());
        } catch (Exception e) {
            log.error("Error al registrar ingreso", e);
            return "Error al registrar el ingreso: " + e.getMessage();
        }
    }
    
    @Tool(name = "listarTransacciones", description = "Lista las transacciones de un usuario en un rango de fechas. Requiere número de teléfono, fecha inicio y fecha fin.")
    public String listarTransacciones(@ToolParam String numeroTelefono, @ToolParam String fechaInicio, 
                                     @ToolParam String fechaFin, @ToolParam String tipo) {
        log.info("Listando transacciones para usuario: {} en rango: {} - {}", numeroTelefono, fechaInicio, fechaFin);
        
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
            LocalDate startDate = parseDate(fechaInicio);
            LocalDate endDate = parseDate(fechaFin);
            
            List<Transaction> transactions;
            if (tipo != null && !tipo.trim().isEmpty()) {
                transactions = transactionService.getTransactionsByUserIdAndType(user.getId(), tipo.trim().toUpperCase());
            } else {
                transactions = transactionService.getTransactionsByUserIdAndDateRange(user.getId(), startDate, endDate);
            }
            
            if (transactions.isEmpty()) {
                return "No se encontraron transacciones en el rango de fechas especificado";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Transacciones encontradas:\n");
            
            for (Transaction transaction : transactions) {
                result.append(String.format("- %s: %s (%s) - %s %s\n", 
                    transaction.getTransactionDate(), transaction.getDescription(), 
                    transaction.getCategory(), transaction.getAmount(), user.getCurrency()));
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al listar transacciones", e);
            return "Error al listar las transacciones: " + e.getMessage();
        }
    }
    
    @Tool(name = "obtenerResumenGastos", description = "Calcula cuánto ha gastado un usuario en un periodo específico. Requiere número de teléfono, fecha inicio y fecha fin.")
    public String obtenerResumenGastos(@ToolParam String numeroTelefono, @ToolParam String fechaInicio, 
                                      @ToolParam String fechaFin) {
        log.info("Obteniendo resumen de gastos para usuario: {} en rango: {} - {}", numeroTelefono, fechaInicio, fechaFin);
        
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
            LocalDate startDate = parseDate(fechaInicio);
            LocalDate endDate = parseDate(fechaFin);
            
            BigDecimal totalGastos = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "EXPENSE", startDate, endDate);
            
            BigDecimal totalIngresos = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "INCOME", startDate, endDate);
            
            BigDecimal balance = totalIngresos.subtract(totalGastos);
            
            return String.format("Resumen financiero del %s al %s:\n" +
                "Total gastos: %s %s\n" +
                "Total ingresos: %s %s\n" +
                "Balance: %s %s", 
                startDate, endDate, totalGastos, user.getCurrency(), 
                totalIngresos, user.getCurrency(), balance, user.getCurrency());
        } catch (Exception e) {
            log.error("Error al obtener resumen de gastos", e);
            return "Error al obtener el resumen de gastos: " + e.getMessage();
        }
    }
    
    @Tool(name = "categorizarTransaccion", description = "Actualiza la categoría de una transacción existente. Requiere ID de transacción y nueva categoría.")
    public String categorizarTransaccion(@ToolParam String idTransaccion, @ToolParam String nuevaCategoria) {
        log.info("Categorizando transacción: {} con categoría: {}", idTransaccion, nuevaCategoria);
        
        if (idTransaccion == null || idTransaccion.trim().isEmpty()) {
            return "Error: El ID de la transacción no puede estar vacío";
        }
        
        if (nuevaCategoria == null || nuevaCategoria.trim().isEmpty()) {
            return "Error: La nueva categoría no puede estar vacía";
        }
        
        try {
            Optional<Transaction> transactionOpt = transactionService.getTransactionById(idTransaccion.trim());
            if (transactionOpt.isEmpty()) {
                return "Error: No se encontró la transacción con ID: " + idTransaccion;
            }
            
            Transaction transaction = transactionOpt.get();
            transaction.setCategory(nuevaCategoria.trim());
            
            Transaction updatedTransaction = transactionService.updateTransaction(idTransaccion.trim(), transaction);
            return String.format("Transacción categorizada exitosamente - ID: %s, Nueva categoría: %s", 
                updatedTransaction.getId(), updatedTransaction.getCategory());
        } catch (Exception e) {
            log.error("Error al categorizar transacción", e);
            return "Error al categorizar la transacción: " + e.getMessage();
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
}
