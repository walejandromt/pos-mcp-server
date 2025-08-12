package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.Alert;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.AlertService;
import com.punto.de.venta.mcp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.math.BigDecimal;

@Service
@Slf4j
public class AlertTools {
    
    private final AlertService alertService;
    private final UserService userService;
    
    public AlertTools(AlertService alertService, UserService userService) {
        this.alertService = alertService;
        this.userService = userService;
    }
    
    @Tool(name = "crearAlerta", description = "Crea una alerta personalizada para el usuario. Requiere el número de teléfono del usuario.")
    public String crearAlerta(@ToolParam String numeroTelefono, @ToolParam String tipoAlerta, 
                             @ToolParam String mensaje, @ToolParam String fechaProgramada) {
        log.info("Creando alerta para usuario: {} - Tipo: {}, Mensaje: {}", 
                numeroTelefono, tipoAlerta, mensaje);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (tipoAlerta == null || tipoAlerta.trim().isEmpty()) {
            return "Error: El tipo de alerta no puede estar vacío";
        }
        
        if (mensaje == null || mensaje.trim().isEmpty()) {
            return "Error: El mensaje no puede estar vacío";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Crear alerta
            Alert alert = new Alert();
            alert.setUser(user);
            alert.setAlertType(tipoAlerta.trim());
            alert.setMessage(mensaje.trim());
            alert.setStatus("PENDING");
            alert.setScheduledAt(parseDateTime(fechaProgramada));
            
            Alert savedAlert = alertService.createAlert(alert);
            return String.format("Alerta creada exitosamente - ID: %s, Tipo: %s, Mensaje: %s, Programada para: %s", 
                savedAlert.getId(), savedAlert.getAlertType(), savedAlert.getMessage(), 
                savedAlert.getScheduledAt());
        } catch (Exception e) {
            log.error("Error al crear alerta", e);
            return "Error al crear la alerta: " + e.getMessage();
        }
    }
    
    @Tool(name = "listarAlertas", description = "Lista todas las alertas de un usuario. Requiere el número de teléfono del usuario.")
    public String listarAlertas(@ToolParam String numeroTelefono) {
        log.info("Listando alertas para usuario: {}", numeroTelefono);
        
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
            
            List<Alert> alerts = alertService.getAlertsByUserId(user.getId());
            
            if (alerts.isEmpty()) {
                return "No tienes alertas configuradas";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Alertas configuradas:\n");
            
            for (Alert alert : alerts) {
                result.append(String.format("- %s: %s (Estado: %s)\n", 
                    alert.getAlertType(), alert.getMessage(), alert.getStatus()));
                if (alert.getScheduledAt() != null) {
                    result.append(String.format("  Programada para: %s\n", alert.getScheduledAt()));
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al listar alertas", e);
            return "Error al listar las alertas: " + e.getMessage();
        }
    }
    
    @Tool(name = "alertarPresupuestoExcedido", description = "Crea automáticamente una alerta cuando se supera un presupuesto. Requiere el número de teléfono del usuario y la categoría.")
    public String alertarPresupuestoExcedido(@ToolParam String numeroTelefono, @ToolParam String categoria) {
        log.info("Creando alerta de presupuesto excedido para usuario: {} - Categoría: {}", numeroTelefono, categoria);
        
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
            
            // Crear alerta de presupuesto excedido
            Alert alert = new Alert();
            alert.setUser(user);
            alert.setAlertType("PRESUPUESTO_EXCEDIDO");
            alert.setMessage(String.format("Has excedido tu presupuesto en la categoría: %s. Revisa tus gastos.", categoria));
            alert.setStatus("PENDING");
            alert.setScheduledAt(LocalDateTime.now());
            
            Alert savedAlert = alertService.createAlert(alert);
            return String.format("Alerta de presupuesto excedido creada - ID: %s, Categoría: %s", 
                savedAlert.getId(), categoria);
        } catch (Exception e) {
            log.error("Error al crear alerta de presupuesto excedido", e);
            return "Error al crear la alerta de presupuesto excedido: " + e.getMessage();
        }
    }
    
    @Tool(name = "detectarGastoInusual", description = "Detecta y alerta si hay un gasto mucho mayor al promedio. Requiere el número de teléfono del usuario.")
    public String detectarGastoInusual(@ToolParam String numeroTelefono, @ToolParam BigDecimal umbral) {
        log.info("Detectando gastos inusuales para usuario: {} - Umbral: {}", numeroTelefono, umbral);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (umbral == null || umbral.compareTo(BigDecimal.ZERO) <= 0) {
            umbral = BigDecimal.valueOf(1000); // Umbral por defecto
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Crear alerta de gasto inusual
            Alert alert = new Alert();
            alert.setUser(user);
            alert.setAlertType("GASTO_INUSUAL");
            alert.setMessage(String.format("Se detectó un gasto inusual mayor a %s %s. Revisa si es correcto.", 
                umbral, user.getCurrency()));
            alert.setStatus("PENDING");
            alert.setScheduledAt(LocalDateTime.now());
            
            Alert savedAlert = alertService.createAlert(alert);
            return String.format("Alerta de gasto inusual creada - ID: %s, Umbral: %s %s", 
                savedAlert.getId(), umbral, user.getCurrency());
        } catch (Exception e) {
            log.error("Error al detectar gasto inusual", e);
            return "Error al detectar gasto inusual: " + e.getMessage();
        }
    }
    
    @Tool(name = "sugerirOportunidadesAhorro", description = "Sugiere ajustes según patrones de gastos. Requiere el número de teléfono del usuario.")
    public String sugerirOportunidadesAhorro(@ToolParam String numeroTelefono) {
        log.info("Sugiriendo oportunidades de ahorro para usuario: {}", numeroTelefono);
        
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
            
            // Crear alerta con sugerencias de ahorro
            Alert alert = new Alert();
            alert.setUser(user);
            alert.setAlertType("OPORTUNIDAD_AHORRO");
            alert.setMessage("💡 Sugerencias de ahorro:\n" +
                "• Revisa tus suscripciones mensuales\n" +
                "• Considera cocinar en lugar de comer fuera\n" +
                "• Usa transporte público cuando sea posible\n" +
                "• Establece límites diarios de gastos");
            alert.setStatus("PENDING");
            alert.setScheduledAt(LocalDateTime.now());
            
            Alert savedAlert = alertService.createAlert(alert);
            return String.format("Sugerencias de ahorro creadas - ID: %s", savedAlert.getId());
        } catch (Exception e) {
            log.error("Error al sugerir oportunidades de ahorro", e);
            return "Error al sugerir oportunidades de ahorro: " + e.getMessage();
        }
    }
    
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // Intentar diferentes formatos de fecha y hora
            String[] formats = {
                "yyyy-MM-dd HH:mm:ss",
                "yyyy-MM-dd HH:mm",
                "yyyy-MM-dd",
                "dd/MM/yyyy HH:mm:ss",
                "dd/MM/yyyy HH:mm",
                "dd/MM/yyyy"
            };
            
            for (String format : formats) {
                try {
                    if (format.contains("HH:mm")) {
                        return LocalDateTime.parse(dateTimeStr.trim(), DateTimeFormatter.ofPattern(format));
                    } else {
                        return LocalDate.parse(dateTimeStr.trim(), DateTimeFormatter.ofPattern(format))
                            .atStartOfDay();
                    }
                } catch (Exception ignored) {
                    // Continuar con el siguiente formato
                }
            }
            // Si no se puede parsear, usar fecha y hora actual
            return LocalDateTime.now();
        } catch (Exception e) {
            log.warn("No se pudo parsear la fecha/hora: {}, usando fecha/hora actual", dateTimeStr);
            return LocalDateTime.now();
        }
    }
}
