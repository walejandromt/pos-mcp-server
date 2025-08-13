package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.Loan;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.LoanService;
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
public class LoanTools {
    
    private final LoanService loanService;
    private final UserService userService;
    
    public LoanTools(LoanService loanService, UserService userService) {
        this.loanService = loanService;
        this.userService = userService;
    }
    
    @Tool(name = "registrarPrestamo", description = "Registra un nuevo préstamo para el usuario. Requiere el número de teléfono del usuario, monto del préstamo, tasa de interés, monto del pago mensual, día de pago, fecha de inicio y descripción.")
    public String registrarPrestamo(@ToolParam String numeroTelefono, @ToolParam BigDecimal montoPrestamo, 
                                   @ToolParam BigDecimal tasaInteres, @ToolParam BigDecimal montoPago, 
                                   @ToolParam Integer diaPago, @ToolParam String fechaInicio, 
                                   @ToolParam String descripcion) {
        log.info("Registrando préstamo para usuario: {} - Monto: {}, Tasa: {}%, Pago mensual: {}", 
                numeroTelefono, montoPrestamo, tasaInteres, montoPago);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (montoPrestamo == null || montoPrestamo.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto del préstamo debe ser mayor a cero";
        }
        
        if (tasaInteres == null || tasaInteres.compareTo(BigDecimal.ZERO) < 0) {
            return "Error: La tasa de interés no puede ser negativa";
        }
        
        if (montoPago == null || montoPago.compareTo(BigDecimal.ZERO) <= 0) {
            return "Error: El monto del pago debe ser mayor a cero";
        }
        
        if (diaPago == null || diaPago < 1 || diaPago > 31) {
            return "Error: El día de pago debe estar entre 1 y 31";
        }
        
        try {
            // Obtener usuario por teléfono
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Crear préstamo
            Loan loan = new Loan();
            loan.setUser(user);
            loan.setPrincipal(montoPrestamo);
            loan.setInterestRate(tasaInteres);
            loan.setMonthlyPayment(montoPago);
            loan.setStartDate(parseDate(fechaInicio));
            loan.setPaymentDay(diaPago);
            loan.setDescription(descripcion != null ? descripcion.trim() : "Préstamo personal");
            
            Loan savedLoan = loanService.createLoan(loan);
            return String.format("Préstamo registrado exitosamente - ID: %s, Monto: %s %s, Tasa: %s%%, Pago mensual: %s %s, Día de pago: %d", 
                savedLoan.getId(), savedLoan.getPrincipal(), user.getCurrency(), 
                savedLoan.getInterestRate(), savedLoan.getMonthlyPayment(), user.getCurrency(), 
                savedLoan.getPaymentDay());
        } catch (Exception e) {
            log.error("Error al registrar préstamo", e);
            return "Error al registrar el préstamo: " + e.getMessage();
        }
    }
    
    @Tool(name = "listarPrestamos", description = "Lista todos los préstamos de un usuario. Requiere el número de teléfono del usuario.")
    public String listarPrestamos(@ToolParam String numeroTelefono) {
        log.info("Listando préstamos para usuario: {}", numeroTelefono);
        
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
            
            List<Loan> loans = loanService.getLoansByUserId(user.getId());
            
            if (loans.isEmpty()) {
                return "No tienes préstamos registrados";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Préstamos registrados:\n");
            
            for (Loan loan : loans) {
                result.append(String.format("- %s: Monto original %s %s, Tasa %s%%, Pago mensual %s %s, Día %d\n", 
                    loan.getDescription(), loan.getPrincipal(), user.getCurrency(), 
                    loan.getInterestRate(), loan.getMonthlyPayment(), user.getCurrency(), 
                    loan.getPaymentDay()));
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al listar préstamos", e);
            return "Error al listar los préstamos: " + e.getMessage();
        }
    }
    
    @Tool(name = "calcularPlanPagoDeudas", description = "Calcula un plan para pagar deudas más rápido con diferentes estrategias. Requiere el número de teléfono del usuario.")
    public String calcularPlanPagoDeudas(@ToolParam String numeroTelefono, @ToolParam String estrategia) {
        log.info("Calculando plan de pago de deudas para usuario: {} con estrategia: {}", numeroTelefono, estrategia);
        
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
            
            List<Loan> loans = loanService.getLoansByUserId(user.getId());
            
            if (loans.isEmpty()) {
                return "No tienes préstamos registrados para crear un plan de pago";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Plan de pago de deudas (").append(estrategia != null ? estrategia : "snowball").append("):\n");
            
            // Estrategia Snowball: pagar primero las deudas más pequeñas
            if ("snowball".equalsIgnoreCase(estrategia) || estrategia == null) {
                loans.sort((l1, l2) -> l1.getPrincipal().compareTo(l2.getPrincipal()));
                result.append("Estrategia Snowball - Pagar primero las deudas más pequeñas:\n");
            } 
            // Estrategia Avalanche: pagar primero las deudas con mayor tasa de interés
            else if ("avalanche".equalsIgnoreCase(estrategia)) {
                loans.sort((l1, l2) -> l2.getInterestRate().compareTo(l1.getInterestRate()));
                result.append("Estrategia Avalanche - Pagar primero las deudas con mayor tasa de interés:\n");
            }
            
            for (int i = 0; i < loans.size(); i++) {
                Loan loan = loans.get(i);
                result.append(String.format("%d. %s: %s %s (Tasa: %s%%)\n", 
                    i + 1, loan.getDescription(), loan.getPrincipal(), user.getCurrency(), 
                    loan.getInterestRate()));
            }
            
            BigDecimal totalDeuda = loans.stream()
                .map(Loan::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal totalPagoMensual = loans.stream()
                .map(Loan::getMonthlyPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            result.append(String.format("\nTotal de deuda: %s %s\n", totalDeuda, user.getCurrency()));
            result.append(String.format("Total de pagos mensuales: %s %s\n", totalPagoMensual, user.getCurrency()));
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al calcular plan de pago de deudas", e);
            return "Error al calcular el plan de pago de deudas: " + e.getMessage();
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
