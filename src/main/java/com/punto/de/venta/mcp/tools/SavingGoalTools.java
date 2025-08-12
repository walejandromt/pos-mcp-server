package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.SavingGoal;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.SavingGoalService;
import com.punto.de.venta.mcp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class SavingGoalTools {
    
    private final SavingGoalService savingGoalService;
    private final UserService userService;
    
    public SavingGoalTools(SavingGoalService savingGoalService, UserService userService) {
        this.savingGoalService = savingGoalService;
        this.userService = userService;
    }
    
    @Tool(name = "crearMetaAhorro", description = "Crea una meta de ahorro con fecha y monto objetivo. Requiere el número de teléfono del usuario.")
    public String crearMetaAhorro(@ToolParam String numeroTelefono, @ToolParam String nombreMeta, 
                                 @ToolParam BigDecimal montoObjetivo, @ToolParam String fechaMeta) {
        log.info("Creando meta de ahorro para usuario: {} - Meta: {}, Monto: {}", 
                numeroTelefono, nombreMeta, montoObjetivo);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (nombreMeta == null || nombreMeta.trim().isEmpty()) {
            return "Error: El nombre de la meta no puede estar vacío";
        }
        
        if (montoObjetivo == null || montoObjetivo.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto objetivo debe ser mayor a cero";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Crear meta de ahorro
            SavingGoal savingGoal = new SavingGoal();
            savingGoal.setUser(user);
            savingGoal.setGoalName(nombreMeta.trim());
            savingGoal.setTargetAmount(montoObjetivo);
            savingGoal.setCurrentAmount(BigDecimal.ZERO);
            savingGoal.setTargetDate(parseDate(fechaMeta));
            
            SavingGoal savedGoal = savingGoalService.createSavingGoal(savingGoal);
            return String.format("Meta de ahorro creada exitosamente - ID: %s, Meta: %s, Objetivo: %s %s, Fecha meta: %s", 
                savedGoal.getId(), savedGoal.getGoalName(), savedGoal.getTargetAmount(), 
                user.getCurrency(), savedGoal.getTargetDate());
        } catch (Exception e) {
            log.error("Error al crear meta de ahorro", e);
            return "Error al crear la meta de ahorro: " + e.getMessage();
        }
    }
    
    @Tool(name = "seguimientoMetaAhorro", description = "Ver el progreso de una meta de ahorro específica. Requiere el número de teléfono del usuario y el nombre de la meta.")
    public String seguimientoMetaAhorro(@ToolParam String numeroTelefono, @ToolParam String nombreMeta) {
        log.info("Verificando seguimiento de meta de ahorro para usuario: {} - Meta: {}", numeroTelefono, nombreMeta);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (nombreMeta == null || nombreMeta.trim().isEmpty()) {
            return "Error: El nombre de la meta no puede estar vacío";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener todas las metas del usuario
            List<SavingGoal> goals = savingGoalService.getSavingGoalsByUserId(user.getId());
            
            // Buscar la meta específica
            Optional<SavingGoal> goalOpt = goals.stream()
                .filter(g -> g.getGoalName().equalsIgnoreCase(nombreMeta.trim()))
                .findFirst();
            
            if (goalOpt.isEmpty()) {
                return "No se encontró una meta de ahorro con el nombre: " + nombreMeta;
            }
            
            SavingGoal goal = goalOpt.get();
            
            // Calcular progreso
            BigDecimal montoObjetivo = goal.getTargetAmount();
            BigDecimal montoActual = goal.getCurrentAmount();
            BigDecimal montoRestante = montoObjetivo.subtract(montoActual);
            BigDecimal porcentajeCompletado = montoActual.multiply(BigDecimal.valueOf(100))
                .divide(montoObjetivo, 2, BigDecimal.ROUND_HALF_UP);
            
            // Calcular tiempo restante
            LocalDate fechaActual = LocalDate.now();
            LocalDate fechaMeta = goal.getTargetDate();
            long diasRestantes = ChronoUnit.DAYS.between(fechaActual, fechaMeta);
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Progreso de la meta: %s\n", goal.getGoalName()));
            result.append(String.format("Monto objetivo: %s %s\n", montoObjetivo, user.getCurrency()));
            result.append(String.format("Monto actual: %s %s\n", montoActual, user.getCurrency()));
            result.append(String.format("Monto restante: %s %s\n", montoRestante, user.getCurrency()));
            result.append(String.format("Porcentaje completado: %s%%\n", porcentajeCompletado));
            result.append(String.format("Fecha meta: %s\n", fechaMeta));
            result.append(String.format("Días restantes: %d\n", diasRestantes));
            
            if (porcentajeCompletado.compareTo(BigDecimal.valueOf(100)) >= 0) {
                result.append("🎉 ¡Meta completada!\n");
            } else if (diasRestantes < 0) {
                result.append("⚠️ La fecha meta ya pasó\n");
            } else if (porcentajeCompletado.compareTo(BigDecimal.valueOf(80)) > 0) {
                result.append("✅ ¡Casi lo logras!\n");
            } else if (porcentajeCompletado.compareTo(BigDecimal.valueOf(50)) > 0) {
                result.append("👍 ¡Vas por buen camino!\n");
            } else {
                result.append("💪 ¡Sigue ahorrando!\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al verificar seguimiento de meta de ahorro", e);
            return "Error al verificar el seguimiento de la meta de ahorro: " + e.getMessage();
        }
    }
    
    @Tool(name = "listarMetasAhorro", description = "Lista todas las metas de ahorro de un usuario. Requiere el número de teléfono del usuario.")
    public String listarMetasAhorro(@ToolParam String numeroTelefono) {
        log.info("Listando metas de ahorro para usuario: {}", numeroTelefono);
        
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
            
            List<SavingGoal> goals = savingGoalService.getSavingGoalsByUserId(user.getId());
            
            if (goals.isEmpty()) {
                return "No tienes metas de ahorro definidas";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Metas de ahorro:\n");
            
            for (SavingGoal goal : goals) {
                BigDecimal porcentaje = goal.getCurrentAmount().multiply(BigDecimal.valueOf(100))
                    .divide(goal.getTargetAmount(), 1, BigDecimal.ROUND_HALF_UP);
                
                result.append(String.format("- %s: %s %s / %s %s (%s%%) - Meta: %s\n", 
                    goal.getGoalName(), goal.getCurrentAmount(), user.getCurrency(),
                    goal.getTargetAmount(), user.getCurrency(), porcentaje, goal.getTargetDate()));
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al listar metas de ahorro", e);
            return "Error al listar las metas de ahorro: " + e.getMessage();
        }
    }
    
    @Tool(name = "generarPlanAhorro", description = "Genera un plan de ahorro según ingresos/gastos y meta. Requiere el número de teléfono del usuario y el nombre de la meta.")
    public String generarPlanAhorro(@ToolParam String numeroTelefono, @ToolParam String nombreMeta) {
        log.info("Generando plan de ahorro para usuario: {} - Meta: {}", numeroTelefono, nombreMeta);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (nombreMeta == null || nombreMeta.trim().isEmpty()) {
            return "Error: El nombre de la meta no puede estar vacío";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Obtener la meta específica
            List<SavingGoal> goals = savingGoalService.getSavingGoalsByUserId(user.getId());
            Optional<SavingGoal> goalOpt = goals.stream()
                .filter(g -> g.getGoalName().equalsIgnoreCase(nombreMeta.trim()))
                .findFirst();
            
            if (goalOpt.isEmpty()) {
                return "No se encontró una meta de ahorro con el nombre: " + nombreMeta;
            }
            
            SavingGoal goal = goalOpt.get();
            
            // Calcular plan de ahorro
            BigDecimal montoRestante = goal.getTargetAmount().subtract(goal.getCurrentAmount());
            LocalDate fechaActual = LocalDate.now();
            LocalDate fechaMeta = goal.getTargetDate();
            long diasRestantes = ChronoUnit.DAYS.between(fechaActual, fechaMeta);
            
            if (diasRestantes <= 0) {
                return "La fecha meta ya pasó. Considera ajustar la fecha o el monto objetivo.";
            }
            
            BigDecimal ahorroDiario = montoRestante.divide(BigDecimal.valueOf(diasRestantes), 2, BigDecimal.ROUND_HALF_UP);
            BigDecimal ahorroSemanal = ahorroDiario.multiply(BigDecimal.valueOf(7));
            BigDecimal ahorroMensual = ahorroDiario.multiply(BigDecimal.valueOf(30));
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Plan de ahorro para: %s\n", goal.getGoalName()));
            result.append(String.format("Monto restante: %s %s\n", montoRestante, user.getCurrency()));
            result.append(String.format("Días restantes: %d\n", diasRestantes));
            result.append(String.format("Ahorro diario recomendado: %s %s\n", ahorroDiario, user.getCurrency()));
            result.append(String.format("Ahorro semanal recomendado: %s %s\n", ahorroSemanal, user.getCurrency()));
            result.append(String.format("Ahorro mensual recomendado: %s %s\n", ahorroMensual, user.getCurrency()));
            
            if (ahorroDiario.compareTo(BigDecimal.valueOf(100)) > 0) {
                result.append("💡 Consejo: Considera aumentar el plazo o reducir el monto objetivo\n");
            } else if (ahorroDiario.compareTo(BigDecimal.valueOf(10)) < 0) {
                result.append("💡 Consejo: ¡Excelente! Este plan es muy alcanzable\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al generar plan de ahorro", e);
            return "Error al generar el plan de ahorro: " + e.getMessage();
        }
    }
    
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return LocalDate.now().plusMonths(12); // Por defecto, meta en 1 año
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
            // Si no se puede parsear, usar fecha en 1 año
            return LocalDate.now().plusMonths(12);
        } catch (Exception e) {
            log.warn("No se pudo parsear la fecha: {}, usando fecha en 1 año", dateStr);
            return LocalDate.now().plusMonths(12);
        }
    }
}
