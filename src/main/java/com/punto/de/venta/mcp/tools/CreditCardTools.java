package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.CreditCard;
import com.punto.de.venta.mcp.service.CreditCardService;
import com.punto.de.venta.mcp.service.UserService;

import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreditCardTools {
    
    @Autowired
    private UserService userService;

    @Autowired
    private CreditCardService creditCardService;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Tool(name = "agregarTarjetaCredito", description = "Registra una nueva tarjeta de crédito para un usuario. Requiere el ID del usuario, nombre de la tarjeta, banco, últimos dígitos, límite de crédito y moneda.")
    public String agregarTarjetaCredito(@ToolParam Long userId, @ToolParam String name, @ToolParam(description = "Banco o emisor de la tarjeta.") String bankName, 
                                       @ToolParam(description = "Últimos 4 dígitos de la tarjeta (ej. 1234).") String lastDigits, @ToolParam(description = "Límite total de crédito asignado por el banco.") String creditLimit, @ToolParam String currency, @ToolParam(description = "Día del mes en que cierra el periodo de facturación (ej. 20).") Integer cutOffDay, @ToolParam(description = "Día del mes en que vence el pago (ej. 10).") Integer paymentDueDay) {
        log.info("Agregando tarjeta de crédito para usuario: {} con nombre: {}", userId, name);
        
        if (userId == null) {
            return "Error: El ID del usuario es requerido";
        }
        
        if (name == null || name.trim().isEmpty()) {
            return "Error: El nombre de la tarjeta es requerido";
        }
        
        if (lastDigits == null || lastDigits.trim().isEmpty()) {
            return "Error: Los últimos dígitos son requeridos";
        }

        if (cutOffDay == null) {
            return "Error: El día de corte es requerido";
        }

        if (paymentDueDay == null) {
            return "Error: El día de pago es requerido";
        }
        
        try {
            // Crear la tarjeta de crédito
            CreditCard creditCard = new CreditCard();
            creditCard.setUser(userService.getUserById(userId).get());
            creditCard.setCardName(name);
            creditCard.setLastFourDigits(lastDigits);
            creditCard.setCutOffDay(cutOffDay);
            creditCard.setPaymentDueDay(paymentDueDay);
            
            if (creditLimit != null && !creditLimit.trim().isEmpty()) {
                try {
                    creditCard.setCreditLimit(new BigDecimal(creditLimit));
                } catch (NumberFormatException e) {
                    return "Error: El límite de crédito debe ser un número válido";
                }
            }
            
            // Crear metadata con información adicional
            Map<String, String> metadata = new HashMap<>();
            if (bankName != null && !bankName.trim().isEmpty()) {
                metadata.put("bank_name", bankName);
            }
            if (currency != null && !currency.trim().isEmpty()) {
                metadata.put("currency", currency);
            }
            
            if (!metadata.isEmpty()) {
                try {
                    creditCard.setMetadata(objectMapper.writeValueAsString(metadata));
                } catch (JsonProcessingException e) {
                    log.error("Error al convertir metadata a JSON", e);
                    return "Error al procesar los datos de la tarjeta: " + e.getMessage();
                }
            }
            
            CreditCard savedCreditCard = creditCardService.createCreditCard(creditCard);
            
            return String.format("Tarjeta de crédito agregada exitosamente - ID: %s, Nombre: %s, Banco: %s, Últimos dígitos: %s", 
                savedCreditCard.getId(), savedCreditCard.getCardName(), bankName, savedCreditCard.getLastFourDigits());
            
        } catch (Exception e) {
            log.error("Error al agregar tarjeta de crédito", e);
            return "Error al agregar tarjeta de crédito: " + e.getMessage();
        }
    }
    
    @Tool(name = "listarTarjetasCredito", description = "Lista todas las tarjetas de crédito registradas para un usuario. Requiere el ID del usuario.")
    public String listarTarjetasCredito(@ToolParam Long userId) {
        log.info("Listando tarjetas de crédito para usuario: {}", userId);
        
        if (userId == null ) {
            return "Error: El ID del usuario es requerido";
        }
        
        try {
            List<CreditCard> creditCards = creditCardService.getCreditCardsByUserId(userId);
            
            if (creditCards.isEmpty()) {
                return "No tienes tarjetas de crédito registradas";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Tarjetas de crédito registradas:\n");
            
            for (CreditCard card : creditCards) {
                result.append(String.format("- %s (****%s) - Límite: %s\n", 
                    card.getCardName(), card.getLastFourDigits(), 
                    card.getCreditLimit() != null ? card.getCreditLimit() : "No definido"));
            }
            
            result.append(String.format("\nTotal de tarjetas: %d", creditCards.size()));
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error al listar tarjetas de crédito", e);
            return "Error al listar tarjetas de crédito: " + e.getMessage();
        }
    }
    
    @Tool(name = "actualizarTarjetaCredito", description = "Modifica datos de una tarjeta de crédito existente. Requiere el ID de la tarjeta y los campos a actualizar (nombre, últimos dígitos, límite, día de corte, día de pago).")
    public String actualizarTarjetaCredito(@ToolParam Long creditCardId, @ToolParam String name, 
                                          @ToolParam String lastDigits, @ToolParam String creditLimit,
                                          @ToolParam String cutOffDay, @ToolParam String paymentDueDay) {
        log.info("Actualizando tarjeta de crédito con ID: {}", creditCardId);
        
        if (creditCardId == null) {
            return "Error: El ID de la tarjeta de crédito es requerido";
        }
        
        try {
            // Obtener la tarjeta existente
            var existingCreditCard = creditCardService.getCreditCardById(creditCardId);
            if (existingCreditCard.isEmpty()) {
                return "Error: Tarjeta de crédito no encontrada";
            }
            
            CreditCard creditCard = existingCreditCard.get();
            
            // Actualizar campos permitidos
            if (name != null && !name.trim().isEmpty()) {
                creditCard.setCardName(name.trim());
            }
            
            if (lastDigits != null && !lastDigits.trim().isEmpty()) {
                creditCard.setLastFourDigits(lastDigits.trim());
            }
            
            if (creditLimit != null && !creditLimit.trim().isEmpty()) {
                try {
                    creditCard.setCreditLimit(new BigDecimal(creditLimit));
                } catch (NumberFormatException e) {
                    return "Error: El límite de crédito debe ser un número válido";
                }
            }
            
            if (cutOffDay != null && !cutOffDay.trim().isEmpty()) {
                try {
                    creditCard.setCutOffDay(Integer.parseInt(cutOffDay));
                } catch (NumberFormatException e) {
                    return "Error: El día de corte debe ser un número válido";
                }
            }
            
            if (paymentDueDay != null && !paymentDueDay.trim().isEmpty()) {
                try {
                    creditCard.setPaymentDueDay(Integer.parseInt(paymentDueDay));
                } catch (NumberFormatException e) {
                    return "Error: El día de pago debe ser un número válido";
                }
            }
            
            CreditCard updatedCreditCard = creditCardService.updateCreditCard(creditCardId, creditCard);
            
            return String.format("Tarjeta de crédito actualizada exitosamente - ID: %s, Nombre: %s", 
                updatedCreditCard.getId(), updatedCreditCard.getCardName());
            
        } catch (Exception e) {
            log.error("Error al actualizar tarjeta de crédito", e);
            return "Error al actualizar tarjeta de crédito: " + e.getMessage();
        }
    }
    
    @Tool(name = "eliminarTarjetaCredito", description = "Elimina una tarjeta de crédito (solo si no tiene pagos o transacciones vinculadas). Requiere el ID de la tarjeta.")
    public String eliminarTarjetaCredito(@ToolParam Long creditCardId) {
        log.info("Eliminando tarjeta de crédito con ID: {}", creditCardId);
        
        if (creditCardId == null) {
            return "Error: El ID de la tarjeta de crédito es requerido";
        }
        
        try {
            // Verificar si la tarjeta existe
            var existingCreditCard = creditCardService.getCreditCardById(creditCardId);
            if (existingCreditCard.isEmpty()) {
                return "Error: Tarjeta de crédito no encontrada";
            }
            
            boolean deleted = creditCardService.deleteCreditCard(creditCardId);
            
            if (deleted) {
                return "Tarjeta de crédito eliminada exitosamente";
            } else {
                return "No se pudo eliminar la tarjeta de crédito";
            }
            
        } catch (Exception e) {
            log.error("Error al eliminar tarjeta de crédito", e);
            return "Error al eliminar tarjeta de crédito: " + e.getMessage();
        }
    }
    
    @Tool(name = "obtenerFechasVencimientoProximas", description = "Lista tarjetas cuya fecha límite de pago esté próxima. Requiere el ID del usuario y el número de días hacia adelante (opcional, por defecto 30).")
    public String obtenerFechasVencimientoProximas(@ToolParam Long userId, @ToolParam String daysAhead) {
        log.info("Obteniendo fechas de vencimiento próximas para usuario: {} en los próximos {} días", userId, daysAhead);
        
        if (userId == null) {
            return "Error: El ID del usuario es requerido";
        }
        
        int days = 30; // Por defecto 30 días
        if (daysAhead != null && !daysAhead.trim().isEmpty()) {
            try {
                days = Integer.parseInt(daysAhead);
            } catch (NumberFormatException e) {
                return "Error: El número de días debe ser un número válido";
            }
        }
        
        try {
            List<CreditCard> creditCards = creditCardService.getCreditCardsByUserId(userId);
            LocalDate today = LocalDate.now();
            LocalDate futureDate = today.plusDays(days);
            
            List<Map<String, Object>> upcomingCards = new ArrayList<>();
            
            for (CreditCard card : creditCards) {
                if (card.getPaymentDueDay() != null) {
                    // Calcular la próxima fecha de pago
                    LocalDate nextDueDate = calculateNextDueDate(card.getPaymentDueDay());
                    
                    if (nextDueDate.isBefore(futureDate) || nextDueDate.isEqual(futureDate)) {
                        long daysUntilDue = ChronoUnit.DAYS.between(today, nextDueDate);
                        
                        Map<String, Object> cardInfo = new HashMap<>();
                        cardInfo.put("creditCard", card);
                        cardInfo.put("nextDueDate", nextDueDate);
                        cardInfo.put("daysUntilDue", daysUntilDue);
                        cardInfo.put("isOverdue", daysUntilDue < 0);
                        
                        upcomingCards.add(cardInfo);
                    }
                }
            }
            
            // Ordenar por días hasta el vencimiento
            upcomingCards.sort((a, b) -> {
                Long daysA = (Long) a.get("daysUntilDue");
                Long daysB = (Long) b.get("daysUntilDue");
                return daysA.compareTo(daysB);
            });
            
            if (upcomingCards.isEmpty()) {
                return String.format("No hay tarjetas con vencimiento en los próximos %d días", days);
            }
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Tarjetas con vencimiento en los próximos %d días:\n\n", days));
            
            for (Map<String, Object> cardInfo : upcomingCards) {
                CreditCard card = (CreditCard) cardInfo.get("creditCard");
                LocalDate nextDueDate = (LocalDate) cardInfo.get("nextDueDate");
                Long daysUntilDue = (Long) cardInfo.get("daysUntilDue");
                Boolean isOverdue = (Boolean) cardInfo.get("isOverdue");
                
                result.append(String.format("- %s (****%s)\n", card.getCardName(), card.getLastFourDigits()));
                result.append(String.format("  Fecha de vencimiento: %s\n", nextDueDate));
                
                if (isOverdue) {
                    result.append(String.format("  ⚠️ VENCIDA hace %d días\n", Math.abs(daysUntilDue)));
                } else {
                    result.append(String.format("  Vence en %d días\n", daysUntilDue));
                }
                result.append("\n");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error al obtener fechas de vencimiento próximas", e);
            return "Error al obtener fechas de vencimiento próximas: " + e.getMessage();
        }
    }
    
    @Tool(name = "calcularInteresSiNoPaga", description = "Calcula el interés estimado si no se paga la deuda para una fecha específica. Requiere el ID de la tarjeta y la fecha proyectada (opcional, formato YYYY-MM-DD).")
    public String calcularInteresSiNoPaga(@ToolParam Long creditCardId, @ToolParam String projectedDate) {
        log.info("Calculando interés estimado para tarjeta: {} en fecha: {}", creditCardId, projectedDate);
        
        if (creditCardId == null) {
            return "Error: El ID de la tarjeta de crédito es requerido";
        }
        
        LocalDate targetDate = LocalDate.now();
        if (projectedDate != null && !projectedDate.trim().isEmpty()) {
            try {
                targetDate = LocalDate.parse(projectedDate);
            } catch (Exception e) {
                return "Error: La fecha proyectada debe estar en formato YYYY-MM-DD";
            }
        }
        
        try {
            var creditCard = creditCardService.getCreditCardById(creditCardId);
            if (creditCard.isEmpty()) {
                return "Error: Tarjeta de crédito no encontrada";
            }
            
            CreditCard card = creditCard.get();
            
            // Simular cálculo de interés (tasa anual del 30% como ejemplo)
            BigDecimal annualInterestRate = new BigDecimal("0.30");
            BigDecimal monthlyInterestRate = annualInterestRate.divide(new BigDecimal("12"), 4, BigDecimal.ROUND_HALF_UP);
            
            // Obtener saldo actual (simulado)
            BigDecimal currentBalance = card.getCurrentBalance() != null ? card.getCurrentBalance() : BigDecimal.ZERO;
            
            // Calcular días hasta la fecha proyectada
            LocalDate today = LocalDate.now();
            long daysUntilProjected = ChronoUnit.DAYS.between(today, targetDate);
            
            if (daysUntilProjected <= 0) {
                return "Error: La fecha proyectada debe ser futura";
            }
            
            // Calcular interés estimado
            BigDecimal dailyInterestRate = monthlyInterestRate.divide(new BigDecimal("30"), 6, BigDecimal.ROUND_HALF_UP);
            BigDecimal estimatedInterest = currentBalance
                .multiply(dailyInterestRate)
                .multiply(new BigDecimal(daysUntilProjected));
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Cálculo de interés para %s (****%s):\n\n", card.getCardName(), card.getLastFourDigits()));
            result.append(String.format("Saldo actual: %s\n", currentBalance));
            result.append(String.format("Tasa de interés anual: %s%%\n", annualInterestRate.multiply(new BigDecimal("100"))));
            result.append(String.format("Días hasta la fecha proyectada: %d\n", daysUntilProjected));
            result.append(String.format("Interés estimado: %s\n", estimatedInterest));
            result.append(String.format("Monto total con intereses: %s\n", currentBalance.add(estimatedInterest)));
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error al calcular interés estimado", e);
            return "Error al calcular interés estimado: " + e.getMessage();
        }
    }
    
    @Tool(name = "recomendarEstrategiaPago", description = "Sugiere en qué orden pagar tarjetas basándose en fecha de corte, intereses y saldo. Requiere el ID del usuario.")
    public String recomendarEstrategiaPago(@ToolParam Long userId) {
        log.info("Generando recomendación de estrategia de pago para usuario: {}", userId);
        
        if (userId == null) {
            return "Error: El ID del usuario es requerido";
        }
        
        try {
            List<CreditCard> creditCards = creditCardService.getCreditCardsByUserId(userId);
            
            if (creditCards.isEmpty()) {
                return "El usuario no tiene tarjetas de crédito registradas";
            }
            
            List<Map<String, Object>> recommendations = new ArrayList<>();
            LocalDate today = LocalDate.now();
            
            for (CreditCard card : creditCards) {
                Map<String, Object> recommendation = new HashMap<>();
                recommendation.put("creditCard", card);
                
                // Calcular días hasta el vencimiento
                if (card.getPaymentDueDay() != null) {
                    LocalDate nextDueDate = calculateNextDueDate(card.getPaymentDueDay());
                    long daysUntilDue = ChronoUnit.DAYS.between(today, nextDueDate);
                    recommendation.put("daysUntilDue", daysUntilDue);
                    recommendation.put("nextDueDate", nextDueDate);
                    recommendation.put("isOverdue", daysUntilDue < 0);
                }
                
                // Prioridad basada en múltiples factores
                int priority = 0;
                String priorityReason = "";
                
                // Factor 1: Si está vencida
                if (recommendation.containsKey("isOverdue") && (Boolean) recommendation.get("isOverdue")) {
                    priority += 1000;
                    priorityReason += "Vencida; ";
                }
                
                // Factor 2: Proximidad al vencimiento (menos días = mayor prioridad)
                if (recommendation.containsKey("daysUntilDue")) {
                    long daysUntilDue = (Long) recommendation.get("daysUntilDue");
                    if (daysUntilDue <= 7) {
                        priority += 500 - daysUntilDue;
                        priorityReason += "Vence pronto; ";
                    }
                }
                
                // Factor 3: Saldo alto (mayor saldo = mayor prioridad)
                BigDecimal balance = card.getCurrentBalance() != null ? card.getCurrentBalance() : BigDecimal.ZERO;
                if (balance.compareTo(new BigDecimal("10000")) > 0) {
                    priority += 200;
                    priorityReason += "Saldo alto; ";
                } else if (balance.compareTo(new BigDecimal("5000")) > 0) {
                    priority += 100;
                    priorityReason += "Saldo medio; ";
                }
                
                recommendation.put("priority", priority);
                recommendation.put("priorityReason", priorityReason);
                recommendation.put("currentBalance", balance);
                
                recommendations.add(recommendation);
            }
            
            // Ordenar por prioridad (mayor a menor)
            recommendations.sort((a, b) -> {
                Integer priorityA = (Integer) a.get("priority");
                Integer priorityB = (Integer) b.get("priority");
                return priorityB.compareTo(priorityA);
            });
            
            StringBuilder result = new StringBuilder();
            result.append("Estrategia de pago recomendada:\n\n");
            
            for (int i = 0; i < recommendations.size(); i++) {
                Map<String, Object> rec = recommendations.get(i);
                CreditCard card = (CreditCard) rec.get("creditCard");
                Integer priority = (Integer) rec.get("priority");
                String reason = (String) rec.get("priorityReason");
                BigDecimal balance = (BigDecimal) rec.get("currentBalance");
                
                result.append(String.format("%d. %s (****%s)\n", i + 1, card.getCardName(), card.getLastFourDigits()));
                result.append(String.format("   Saldo: %s\n", balance));
                result.append(String.format("   Prioridad: %d\n", priority));
                result.append(String.format("   Razón: %s\n", reason));
                
                if (rec.containsKey("nextDueDate")) {
                    LocalDate nextDue = (LocalDate) rec.get("nextDueDate");
                    Long daysUntilDue = (Long) rec.get("daysUntilDue");
                    result.append(String.format("   Vence: %s (en %d días)\n", nextDue, daysUntilDue));
                }
                result.append("\n");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error al generar recomendación de estrategia de pago", e);
            return "Error al generar recomendación de estrategia de pago: " + e.getMessage();
        }
    }
    
    @Tool(name = "simularPlanPagoTarjeta", description = "Simula cuánto tiempo tardará en liquidarse una tarjeta con pagos fijos mensuales. Requiere el ID de la tarjeta y el monto del pago mensual.")
    public String simularPlanPagoTarjeta(@ToolParam Long creditCardId, @ToolParam String monthlyPayment) {
        log.info("Simulando plan de pago para tarjeta: {} con pago mensual: {}", creditCardId, monthlyPayment);
        
        if (creditCardId == null) {
            return "Error: El ID de la tarjeta de crédito es requerido";
        }
        
        if (monthlyPayment == null || monthlyPayment.trim().isEmpty()) {
            return "Error: El pago mensual es requerido";
        }
        
        BigDecimal payment;
        try {
            payment = new BigDecimal(monthlyPayment);
        } catch (NumberFormatException e) {
            return "Error: El pago mensual debe ser un número válido";
        }
        
        try {
            var creditCard = creditCardService.getCreditCardById(creditCardId);
            if (creditCard.isEmpty()) {
                return "Error: Tarjeta de crédito no encontrada";
            }
            
            CreditCard card = creditCard.get();
            BigDecimal currentBalance = card.getCurrentBalance() != null ? card.getCurrentBalance() : BigDecimal.ZERO;
            
            if (currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
                return "La tarjeta no tiene saldo pendiente";
            }
            
            // Simular plan de pago
            BigDecimal annualInterestRate = new BigDecimal("0.30");
            BigDecimal monthlyInterestRate = annualInterestRate.divide(new BigDecimal("12"), 4, BigDecimal.ROUND_HALF_UP);
            
            List<Map<String, Object>> paymentSchedule = new ArrayList<>();
            BigDecimal remainingBalance = currentBalance;
            int month = 1;
            
            while (remainingBalance.compareTo(BigDecimal.ZERO) > 0 && month <= 60) { // Máximo 5 años
                BigDecimal interest = remainingBalance.multiply(monthlyInterestRate);
                BigDecimal principalPayment = payment.subtract(interest);
                
                if (principalPayment.compareTo(BigDecimal.ZERO) <= 0) {
                    return "Error: El pago mensual es insuficiente para cubrir los intereses";
                }
                
                if (principalPayment.compareTo(remainingBalance) > 0) {
                    principalPayment = remainingBalance;
                }
                
                remainingBalance = remainingBalance.subtract(principalPayment);
                
                Map<String, Object> monthInfo = new HashMap<>();
                monthInfo.put("month", month);
                monthInfo.put("payment", payment);
                monthInfo.put("interest", interest);
                monthInfo.put("principalPayment", principalPayment);
                monthInfo.put("remainingBalance", remainingBalance);
                
                paymentSchedule.add(monthInfo);
                
                if (remainingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                    break;
                }
                
                month++;
            }
            
            BigDecimal totalInterest = paymentSchedule.stream()
                .map(m -> (BigDecimal) m.get("interest"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal totalAmount = payment.multiply(new BigDecimal(month));
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Plan de pago simulado para %s (****%s):\n\n", card.getCardName(), card.getLastFourDigits()));
            result.append(String.format("Saldo inicial: %s\n", currentBalance));
            result.append(String.format("Pago mensual: %s\n", payment));
            result.append(String.format("Tasa de interés anual: %s%%\n", annualInterestRate.multiply(new BigDecimal("100"))));
            result.append(String.format("Meses para liquidar: %d\n", month));
            result.append(String.format("Intereses totales: %s\n", totalInterest));
            result.append(String.format("Monto total a pagar: %s\n", totalAmount));
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error al simular plan de pago", e);
            return "Error al simular plan de pago: " + e.getMessage();
        }
    }
    
    @Tool(name = "detectarTarjetasDuplicadas", description = "Detecta si el usuario registró la misma tarjeta varias veces por error comparando últimos dígitos y banco. Requiere el ID del usuario.")
    public String detectarTarjetasDuplicadas(@ToolParam Long userId) {
        log.info("Detectando tarjetas duplicadas para usuario: {}", userId);
        
        if (userId == null) {
            return "Error: El ID del usuario es requerido";
        }
        
        try {
            List<CreditCard> creditCards = creditCardService.getCreditCardsByUserId(userId);
            
            if (creditCards.size() <= 1) {
                return "No se encontraron tarjetas duplicadas";
            }
            
            // Agrupar por últimos dígitos
            Map<String, List<CreditCard>> cardsByLastDigits = creditCards.stream()
                .filter(card -> card.getLastFourDigits() != null && !card.getLastFourDigits().trim().isEmpty())
                .collect(Collectors.groupingBy(CreditCard::getLastFourDigits));
            
            List<Map<String, Object>> duplicates = new ArrayList<>();
            
            for (Map.Entry<String, List<CreditCard>> entry : cardsByLastDigits.entrySet()) {
                if (entry.getValue().size() > 1) {
                    Map<String, Object> duplicateGroup = new HashMap<>();
                    duplicateGroup.put("lastFourDigits", entry.getKey());
                    duplicateGroup.put("cards", entry.getValue());
                    duplicateGroup.put("count", entry.getValue().size());
                    
                    // Extraer información del banco de metadata si está disponible
                    List<String> banks = entry.getValue().stream()
                        .map(card -> extractBankFromMetadata(card.getMetadata()))
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                    
                    duplicateGroup.put("banks", banks);
                    duplicateGroup.put("sameBank", banks.size() == 1);
                    
                    duplicates.add(duplicateGroup);
                }
            }
            
            if (duplicates.isEmpty()) {
                return "No se encontraron tarjetas duplicadas";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("Tarjetas duplicadas detectadas:\n\n");
            
            for (Map<String, Object> duplicate : duplicates) {
                String lastFourDigits = (String) duplicate.get("lastFourDigits");
                List<CreditCard> cards = (List<CreditCard>) duplicate.get("cards");
                Integer count = (Integer) duplicate.get("count");
                List<String> banks = (List<String>) duplicate.get("banks");
                Boolean sameBank = (Boolean) duplicate.get("sameBank");
                
                result.append(String.format("Últimos dígitos: ****%s\n", lastFourDigits));
                result.append(String.format("Cantidad de tarjetas: %d\n", count));
                
                for (CreditCard card : cards) {
                    result.append(String.format("- %s (ID: %s)\n", card.getCardName(), card.getId()));
                }
                
                if (!banks.isEmpty()) {
                    result.append(String.format("Bancos: %s\n", String.join(", ", banks)));
                    result.append(String.format("Mismo banco: %s\n", sameBank ? "Sí" : "No"));
                }
                result.append("\n");
            }
            
            result.append(String.format("Total de grupos duplicados: %d", duplicates.size()));
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error al detectar tarjetas duplicadas", e);
            return "Error al detectar tarjetas duplicadas: " + e.getMessage();
        }
    }
    
    // Métodos auxiliares
    private LocalDate calculateNextDueDate(Integer paymentDueDay) {
        LocalDate today = LocalDate.now();
        LocalDate nextDue = today.withDayOfMonth(paymentDueDay);
        
        if (nextDue.isBefore(today)) {
            nextDue = nextDue.plusMonths(1);
        }
        
        return nextDue;
    }
    
    private String extractBankFromMetadata(String metadata) {
        if (metadata == null || metadata.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Buscar "bank_name" en la metadata
            if (metadata.contains("bank_name")) {
                int startIndex = metadata.indexOf("bank_name=");
                if (startIndex != -1) {
                    startIndex += 10; // "bank_name=".length()
                    int endIndex = metadata.indexOf(",", startIndex);
                    if (endIndex == -1) {
                        endIndex = metadata.indexOf("}", startIndex);
                    }
                    if (endIndex != -1) {
                        return metadata.substring(startIndex, endIndex).trim();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error al extraer banco de metadata: {}", metadata, e);
        }
        
        return null;
    }
}
