package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.Transaction;
import com.punto.de.venta.mcp.model.Loan;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.TransactionService;
import com.punto.de.venta.mcp.service.LoanService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AnalyticsTools {
    
    private final TransactionService transactionService;
    private final LoanService loanService;
    private final UserService userService;
    
    public AnalyticsTools(TransactionService transactionService, LoanService loanService, UserService userService) {
        this.transactionService = transactionService;
        this.loanService = loanService;
        this.userService = userService;
    }
    
    @Tool(name = "forecastCashFlow", description = "Proyecta el flujo de efectivo a futuro considerando ingresos/gastos programados. Requiere el número de teléfono del usuario y meses a proyectar.")
    public String forecastCashFlow(@ToolParam String numeroTelefono, @ToolParam Integer mesesProyeccion) {
        log.info("Proyectando flujo de efectivo para usuario: {} - Meses: {}", numeroTelefono, mesesProyeccion);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        if (mesesProyeccion == null || mesesProyeccion <= 0) {
            mesesProyeccion = 3; // Por defecto 3 meses
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            LocalDate fechaActual = LocalDate.now();
            
            // Obtener saldo actual (últimos 3 meses de transacciones)
            LocalDate inicioCalculo = fechaActual.minusMonths(3);
            BigDecimal ingresosRecientes = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "INCOME", inicioCalculo, fechaActual);
            BigDecimal gastosRecientes = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "EXPENSE", inicioCalculo, fechaActual);
            BigDecimal saldoActual = ingresosRecientes.subtract(gastosRecientes);
            
            // Calcular ingresos y gastos recurrentes mensuales
            List<Transaction> transaccionesRecurrentes = transactionService.getTransactionsByUserId(user.getId());
            BigDecimal ingresosRecurrentesMensuales = transaccionesRecurrentes.stream()
                .filter(t -> "INCOME".equals(t.getType()) && t.getRecurringRef() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            BigDecimal gastosRecurrentesMensuales = transaccionesRecurrentes.stream()
                .filter(t -> "EXPENSE".equals(t.getType()) && t.getRecurringRef() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calcular pagos de préstamos mensuales
            List<Loan> prestamos = loanService.getLoansByUserId(user.getId());
            BigDecimal pagosPrestamosMensuales = prestamos.stream()
                .map(Loan::getMonthlyPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Proyectar flujo de efectivo
            StringBuilder result = new StringBuilder();
            result.append(String.format("Proyección de flujo de efectivo para %d meses:\n", mesesProyeccion));
            result.append(String.format("Saldo actual: %s %s\n", saldoActual, user.getCurrency()));
            result.append(String.format("Ingresos recurrentes mensuales: %s %s\n", ingresosRecurrentesMensuales, user.getCurrency()));
            result.append(String.format("Gastos recurrentes mensuales: %s %s\n", gastosRecurrentesMensuales, user.getCurrency()));
            result.append(String.format("Pagos de préstamos mensuales: %s %s\n", pagosPrestamosMensuales, user.getCurrency()));
            
            BigDecimal flujoNetoMensual = ingresosRecurrentesMensuales.subtract(gastosRecurrentesMensuales).subtract(pagosPrestamosMensuales);
            result.append(String.format("Flujo neto mensual: %s %s\n\n", flujoNetoMensual, user.getCurrency()));
            
            BigDecimal saldoProyectado = saldoActual;
            for (int i = 1; i <= mesesProyeccion; i++) {
                saldoProyectado = saldoProyectado.add(flujoNetoMensual);
                LocalDate fechaProyeccion = fechaActual.plusMonths(i);
                result.append(String.format("Mes %d (%s): %s %s\n", 
                    i, fechaProyeccion.format(DateTimeFormatter.ofPattern("MMM yyyy")), 
                    saldoProyectado, user.getCurrency()));
            }
            
            if (flujoNetoMensual.compareTo(BigDecimal.ZERO) < 0) {
                result.append("\n⚠️ Tu flujo neto es negativo. Considera reducir gastos o aumentar ingresos.");
            } else {
                result.append("\n✅ Tu flujo neto es positivo. ¡Buen trabajo!");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al proyectar flujo de efectivo", e);
            return "Error al proyectar el flujo de efectivo: " + e.getMessage();
        }
    }
    
    @Tool(name = "comparePeriodSpending", description = "Compara gastos entre periodos (mes vs mes, quincena vs quincena). Requiere el número de teléfono del usuario.")
    public String comparePeriodSpending(@ToolParam String numeroTelefono, @ToolParam String periodo1, 
                                       @ToolParam String periodo2, @ToolParam String tipoComparacion) {
        log.info("Comparando gastos para usuario: {} - Periodo1: {}, Periodo2: {}, Tipo: {}", 
                numeroTelefono, periodo1, periodo2, tipoComparacion);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            LocalDate fechaActual = LocalDate.now();
            
            // Determinar fechas de comparación
            LocalDate inicioPeriodo1, finPeriodo1, inicioPeriodo2, finPeriodo2;
            
            if ("mes".equalsIgnoreCase(tipoComparacion) || tipoComparacion == null) {
                // Comparar mes actual vs mes anterior
                inicioPeriodo1 = fechaActual.withDayOfMonth(1);
                finPeriodo1 = fechaActual.withDayOfMonth(fechaActual.lengthOfMonth());
                inicioPeriodo2 = fechaActual.minusMonths(1).withDayOfMonth(1);
                finPeriodo2 = fechaActual.minusMonths(1).withDayOfMonth(
                    fechaActual.minusMonths(1).lengthOfMonth());
            } else if ("quincena".equalsIgnoreCase(tipoComparacion)) {
                // Comparar quincena actual vs anterior
                int diaActual = fechaActual.getDayOfMonth();
                if (diaActual <= 15) {
                    inicioPeriodo1 = fechaActual.withDayOfMonth(1);
                    finPeriodo1 = fechaActual.withDayOfMonth(15);
                    inicioPeriodo2 = fechaActual.minusMonths(1).withDayOfMonth(16);
                    finPeriodo2 = fechaActual.minusMonths(1).withDayOfMonth(
                        fechaActual.minusMonths(1).lengthOfMonth());
                } else {
                    inicioPeriodo1 = fechaActual.withDayOfMonth(16);
                    finPeriodo1 = fechaActual.withDayOfMonth(fechaActual.lengthOfMonth());
                    inicioPeriodo2 = fechaActual.withDayOfMonth(1);
                    finPeriodo2 = fechaActual.withDayOfMonth(15);
                }
            } else {
                return "Error: Tipo de comparación no válido. Use 'mes' o 'quincena'";
            }
            
            // Obtener gastos por periodo
            BigDecimal gastosPeriodo1 = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "EXPENSE", inicioPeriodo1, finPeriodo1);
            BigDecimal gastosPeriodo2 = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "EXPENSE", inicioPeriodo2, finPeriodo2);
            
            // Calcular diferencia
            BigDecimal diferencia = gastosPeriodo1.subtract(gastosPeriodo2);
            BigDecimal porcentajeCambio = gastosPeriodo2.compareTo(BigDecimal.ZERO) > 0 ?
                diferencia.multiply(BigDecimal.valueOf(100)).divide(gastosPeriodo2, 2, BigDecimal.ROUND_HALF_UP) :
                BigDecimal.ZERO;
            
            StringBuilder result = new StringBuilder();
            result.append("Comparación de gastos:\n");
            result.append(String.format("Periodo 1 (%s - %s): %s %s\n", 
                inicioPeriodo1.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                finPeriodo1.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                gastosPeriodo1, user.getCurrency()));
            result.append(String.format("Periodo 2 (%s - %s): %s %s\n", 
                inicioPeriodo2.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                finPeriodo2.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                gastosPeriodo2, user.getCurrency()));
            result.append(String.format("Diferencia: %s %s (%s%%)\n", 
                diferencia, user.getCurrency(), porcentajeCambio));
            
            if (diferencia.compareTo(BigDecimal.ZERO) > 0) {
                result.append("📈 Gastaste más en el periodo más reciente");
            } else if (diferencia.compareTo(BigDecimal.ZERO) < 0) {
                result.append("📉 Gastaste menos en el periodo más reciente");
            } else {
                result.append("➡️ Los gastos fueron iguales en ambos periodos");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al comparar gastos entre periodos", e);
            return "Error al comparar los gastos: " + e.getMessage();
        }
    }
    
    @Tool(name = "getNetWorth", description = "Calcula el patrimonio neto (ingresos - deudas). Requiere el número de teléfono del usuario.")
    public String getNetWorth(@ToolParam String numeroTelefono) {
        log.info("Calculando patrimonio neto para usuario: {}", numeroTelefono);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            LocalDate fechaActual = LocalDate.now();
            LocalDate inicioAnio = fechaActual.withDayOfYear(1);
            
            // Calcular ingresos totales del año
            BigDecimal ingresosAnio = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "INCOME", inicioAnio, fechaActual);
            
            // Calcular gastos totales del año
            BigDecimal gastosAnio = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "EXPENSE", inicioAnio, fechaActual);
            
            // Calcular deudas pendientes
            List<Loan> prestamos = loanService.getLoansByUserId(user.getId());
            BigDecimal totalDeudas = prestamos.stream()
                .map(Loan::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calcular patrimonio neto
            BigDecimal patrimonioNeto = ingresosAnio.subtract(gastosAnio).subtract(totalDeudas);
            
            StringBuilder result = new StringBuilder();
            result.append("Patrimonio Neto:\n");
            result.append(String.format("Ingresos del año: %s %s\n", ingresosAnio, user.getCurrency()));
            result.append(String.format("Gastos del año: %s %s\n", gastosAnio, user.getCurrency()));
            result.append(String.format("Deudas pendientes: %s %s\n", totalDeudas, user.getCurrency()));
            result.append(String.format("Patrimonio neto: %s %s\n", patrimonioNeto, user.getCurrency()));
            
            if (patrimonioNeto.compareTo(BigDecimal.ZERO) > 0) {
                result.append("✅ Tu patrimonio neto es positivo");
            } else if (patrimonioNeto.compareTo(BigDecimal.ZERO) < 0) {
                result.append("⚠️ Tu patrimonio neto es negativo");
            } else {
                result.append("➡️ Tu patrimonio neto es cero");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al calcular patrimonio neto", e);
            return "Error al calcular el patrimonio neto: " + e.getMessage();
        }
    }
    
    @Tool(name = "generateMonthlyReport", description = "Crea un resumen mensual con análisis detallado. Requiere el número de teléfono del usuario y el mes/año.")
    public String generateMonthlyReport(@ToolParam String numeroTelefono, @ToolParam String mes, @ToolParam String anio) {
        log.info("Generando reporte mensual para usuario: {} - Mes: {}, Año: {}", numeroTelefono, mes, anio);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            Optional<User> userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User user = userOpt.get();
            
            // Determinar fechas del reporte
            LocalDate fechaReporte;
            if (mes != null && anio != null) {
                fechaReporte = LocalDate.parse("01-" + mes + "-" + anio, 
                    DateTimeFormatter.ofPattern("dd-MM-yyyy"));
            } else {
                fechaReporte = LocalDate.now().minusMonths(1); // Mes anterior por defecto
            }
            
            LocalDate inicioMes = fechaReporte.withDayOfMonth(1);
            LocalDate finMes = fechaReporte.withDayOfMonth(fechaReporte.lengthOfMonth());
            
            // Obtener datos del mes
            BigDecimal ingresosMes = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "INCOME", inicioMes, finMes);
            BigDecimal gastosMes = transactionService.getSumAmountByUserIdAndTypeAndDateRange(
                user.getId(), "EXPENSE", inicioMes, finMes);
            
            // Obtener transacciones por categoría
            List<Transaction> transaccionesMes = transactionService.getTransactionsByUserIdAndDateRange(
                user.getId(), inicioMes, finMes);
            
            Map<String, BigDecimal> gastosPorCategoria = transaccionesMes.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .collect(Collectors.groupingBy(
                    Transaction::getCategory,
                    Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ));
            
            // Generar reporte
            StringBuilder result = new StringBuilder();
            result.append("📊 REPORTE MENSUAL\n");
            result.append("==================\n");
            result.append(String.format("Usuario: %s\n", user.getName()));
            result.append(String.format("Periodo: %s\n", fechaReporte.format(DateTimeFormatter.ofPattern("MMMM yyyy"))));
            result.append(String.format("Moneda: %s\n\n", user.getCurrency()));
            
            result.append("💰 RESUMEN FINANCIERO\n");
            result.append("-------------------\n");
            result.append(String.format("Ingresos totales: %s %s\n", ingresosMes, user.getCurrency()));
            result.append(String.format("Gastos totales: %s %s\n", gastosMes, user.getCurrency()));
            
            BigDecimal balance = ingresosMes.subtract(gastosMes);
            result.append(String.format("Balance: %s %s\n", balance, user.getCurrency()));
            
            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                result.append("✅ Mes positivo\n");
            } else {
                result.append("⚠️ Mes negativo\n");
            }
            
            result.append("\n📈 GASTOS POR CATEGORÍA\n");
            result.append("----------------------\n");
            
            if (gastosPorCategoria.isEmpty()) {
                result.append("No hay gastos registrados en este mes\n");
            } else {
                gastosPorCategoria.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .forEach(entry -> {
                        BigDecimal porcentaje = gastosMes.compareTo(BigDecimal.ZERO) > 0 ?
                            entry.getValue().multiply(BigDecimal.valueOf(100)).divide(gastosMes, 1, BigDecimal.ROUND_HALF_UP) :
                            BigDecimal.ZERO;
                        result.append(String.format("%s: %s %s (%.1f%%)\n", 
                            entry.getKey(), entry.getValue(), user.getCurrency(), porcentaje));
                    });
            }
            
            result.append("\n💡 RECOMENDACIONES\n");
            result.append("-----------------\n");
            
            if (balance.compareTo(BigDecimal.ZERO) < 0) {
                result.append("• Considera reducir gastos en las categorías más altas\n");
                result.append("• Revisa tus suscripciones recurrentes\n");
                result.append("• Establece un presupuesto para el próximo mes\n");
            } else {
                result.append("• ¡Excelente control de gastos!\n");
                result.append("• Considera aumentar tus ahorros\n");
                result.append("• Mantén este ritmo\n");
            }
            
            return result.toString();
        } catch (Exception e) {
            log.error("Error al generar reporte mensual", e);
            return "Error al generar el reporte mensual: " + e.getMessage();
        }
    }
}
