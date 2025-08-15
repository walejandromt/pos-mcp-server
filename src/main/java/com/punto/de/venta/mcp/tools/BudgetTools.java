package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.Budget;
import com.punto.de.venta.mcp.model.Transaction;
import com.punto.de.venta.mcp.model.TransactionCategory;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.BudgetService;
import com.punto.de.venta.mcp.service.TransactionCategoryService;
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
import java.util.stream.Collectors;

@Service
@Slf4j
public class BudgetTools {
    
    private final BudgetService budgetService;
    private final TransactionService transactionService;
    private final TransactionCategoryService transactionCategoryService;
    private final UserService userService;
    
    public BudgetTools(BudgetService budgetService, TransactionService transactionService, TransactionCategoryService transactionCategoryService, UserService userService) {
        this.budgetService = budgetService;
        this.transactionService = transactionService;
        this.transactionCategoryService = transactionCategoryService;
        this.userService = userService;
    }
    
    @Tool(name = "definirPresupuesto", description = "Define un presupuesto por categoría (ej. comida, transporte, entretenimiento). Requiere el número de teléfono del usuario.")
    public String definirPresupuesto(@ToolParam String numeroTelefono, @ToolParam String categoria, 
                                    @ToolParam BigDecimal montoLimite, @ToolParam String periodo, 
                                    @ToolParam String fechaInicio, @ToolParam String fechaFin) {
        log.info("Definiendo presupuesto para usuario: {} - Categoría: {}, Monto: {}, Periodo: {}", 
                numeroTelefono, categoria, montoLimite, periodo);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (categoria == null || categoria.trim().isEmpty()) {
            return "Error: La categoría no puede estar vacía";
        }
        
        if (montoLimite == null || montoLimite.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto límite debe ser mayor a cero";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener o crear categoría
            TransactionCategory category = getCategoryByName(categoria, user.getId());
            if (category == null) {
                return "Error: No se pudo crear o encontrar la categoría especificada";
            }
            
            // Crear presupuesto
            Budget budget = new Budget();
            budget.setUser(user);
            budget.setTransactionCategory(category);
            budget.setAmountLimit(montoLimite);
            budget.setPeriod(parsePeriod(periodo));
            budget.setStartDate(parseDate(fechaInicio));
            budget.setEndDate(parseDate(fechaFin));
            
            Budget savedBudget = budgetService.createBudget(budget);
            return String.format("Presupuesto definido exitosamente - ID: %s, Categoría: %s, Límite: %s %s, Periodo: %s", 
                savedBudget.getId(), savedBudget.getTransactionCategory().getCategoryName(), savedBudget.getAmountLimit(), 
                user.getCurrency(), savedBudget.getPeriod());
        } catch (Exception e) {
            log.error("Error al definir presupuesto", e);
            return "Error al definir el presupuesto: " + e.getMessage();
        }
    }
    
    @Tool(name = "verificarEstadoPresupuesto", description = "Verifica cuánto del presupuesto has gastado y si lo excediste. Requiere el número de teléfono del usuario y la categoría.")
    public String verificarEstadoPresupuesto(@ToolParam String numeroTelefono, @ToolParam String categoria) {
        log.info("Verificando estado de presupuesto para usuario: {} - Categoría: {}", numeroTelefono, categoria);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (categoria == null || categoria.trim().isEmpty()) {
            return "Error: La categoría no puede estar vacía";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener o buscar categoría
            TransactionCategory category = getCategoryByName(categoria, user.getId());
            if (category == null) {
                return "Error: No se pudo encontrar la categoría especificada";
            }
            
            // Obtener presupuesto por categoría - usar directamente el servicio getBudgetsByUserId y filtrar
            List<Budget> allBudgets = budgetService.getBudgetsByUserId(user.getId());
            List<Budget> budgets = allBudgets.stream()
                .filter(b -> b.getTransactionCategory() != null && 
                           b.getTransactionCategory().getId().equals(category.getId()))
                .collect(Collectors.toList());
            
            if (budgets.isEmpty()) {
                return "No tienes un presupuesto definido para la categoría: " + categoria;
            }
            
            Budget budget = budgets.get(0); // Tomar el primer presupuesto de la categoría
            
            // Obtener gastos de la categoría en el periodo del presupuesto
            List<Transaction> gastos = transactionService.getTransactionsByUserIdAndCategoryId(user.getId(), category.getId());
            
            // Filtrar gastos por fecha del presupuesto
            LocalDate startDate = budget.getStartDate();
            LocalDate endDate = budget.getEndDate() != null ? budget.getEndDate() : LocalDate.now();
            
            BigDecimal totalGastado = gastos.stream()
                .filter(t -> t.getTransactionDate().isAfter(startDate.minusDays(1)) && 
                           t.getTransactionDate().isBefore(endDate.plusDays(1)) &&
                           "EXPENSE".equals(t.getType()))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal limite = budget.getAmountLimit();
            BigDecimal restante = limite.subtract(totalGastado);
            BigDecimal porcentajeUsado = totalGastado.multiply(BigDecimal.valueOf(100)).divide(limite, 2, java.math.RoundingMode.HALF_UP);
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Estado del presupuesto para %s:\n", categoria));
            result.append(String.format("Límite: %s %s\n", limite, user.getCurrency()));
            result.append(String.format("Gastado: %s %s\n", totalGastado, user.getCurrency()));
            result.append(String.format("Restante: %s %s\n", restante, user.getCurrency()));
            result.append(String.format("Porcentaje usado: %s%%\n", porcentajeUsado));
            
            if (restante.compareTo(BigDecimal.ZERO) < 0) {
                result.append("⚠️ ¡Has excedido tu presupuesto!\n");
            } else if (porcentajeUsado.compareTo(BigDecimal.valueOf(80)) > 0) {
                result.append("⚠️ ¡Cuidado! Estás cerca de exceder tu presupuesto\n");
            } else {
                result.append("✅ Tu presupuesto está bajo control\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al verificar estado de presupuesto", e);
            return "Error al verificar el estado del presupuesto: " + e.getMessage();
        }
    }
    
    @Tool(name = "listarPresupuestos", description = "Lista todos los presupuestos de un usuario. Requiere el número de teléfono del usuario.")
    public String listarPresupuestos(@ToolParam String numeroTelefono) {
        log.info("Listando presupuestos para usuario: {}", numeroTelefono);
        
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
            
            List<Budget> budgets = budgetService.getBudgetsByUserId(user.getId());
            
            if (budgets.isEmpty()) {
                return "No tienes presupuestos definidos";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Presupuestos definidos:\n");
            
            for (Budget budget : budgets) {
                result.append(String.format("- %s: %s %s (%s)\n", 
                    budget.getTransactionCategory() != null ? budget.getTransactionCategory().getCategoryName() : "Sin categoría", 
                    budget.getAmountLimit(), user.getCurrency(), 
                    budget.getPeriod()));
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al listar presupuestos", e);
            return "Error al listar los presupuestos: " + e.getMessage();
        }
    }
    
    @Tool(name = "predecirGastos", description = "Predice cuánto gastarás este mes según hábitos y recurrentes. Requiere el número de teléfono del usuario.")
    public String predecirGastos(@ToolParam String numeroTelefono) {
        log.info("Prediciendo gastos para usuario: {}", numeroTelefono);
        
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
            
            // Obtener gastos del mes anterior
            LocalDate inicioMesAnterior = LocalDate.now().minusMonths(1).withDayOfMonth(1);
            LocalDate finMesAnterior = LocalDate.now().minusMonths(1).withDayOfMonth(
                LocalDate.now().minusMonths(1).lengthOfMonth());
            
            BigDecimal gastosMesAnterior = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "EXPENSE", inicioMesAnterior, finMesAnterior);
            
            // Obtener gastos recurrentes mensuales
            List<Transaction> gastosRecurrentes = transactionService.getTransactionsByUserIdAndType(user.getId(), "EXPENSE");
            BigDecimal gastosRecurrentesMensuales = gastosRecurrentes.stream()
                .filter(t -> t.getRecurringRef() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calcular predicción (promedio del mes anterior + gastos recurrentes)
            BigDecimal prediccion = gastosMesAnterior.add(gastosRecurrentesMensuales);
            
            StringBuilder result = new StringBuilder();
            result.append("Predicción de gastos para este mes:\n");
            result.append(String.format("Gastos del mes anterior: %s %s\n", gastosMesAnterior, user.getCurrency()));
            result.append(String.format("Gastos recurrentes mensuales: %s %s\n", gastosRecurrentesMensuales, user.getCurrency()));
            result.append(String.format("Predicción total: %s %s\n", prediccion, user.getCurrency()));
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al predecir gastos", e);
            return "Error al predecir los gastos: " + e.getMessage();
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
    
    private String parsePeriod(String period) {
        if (period == null || period.trim().isEmpty()) {
            return "MONTHLY";
        }
        
        String per = period.trim().toUpperCase();
        switch (per) {
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
}
