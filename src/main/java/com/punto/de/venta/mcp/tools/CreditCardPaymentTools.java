package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.CreditCard;
import com.punto.de.venta.mcp.model.CreditCardPayment;
import com.punto.de.venta.mcp.model.Transaction;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.CreditCardPaymentService;
import com.punto.de.venta.mcp.service.CreditCardService;
import com.punto.de.venta.mcp.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CreditCardPaymentTools {
    
    private final CreditCardPaymentService creditCardPaymentService;
    private final CreditCardService creditCardService;
    private final TransactionService transactionService;
    
    public CreditCardPaymentTools(CreditCardPaymentService creditCardPaymentService,
                                CreditCardService creditCardService,
                                TransactionService transactionService) {
        this.creditCardPaymentService = creditCardPaymentService;
        this.creditCardService = creditCardService;
        this.transactionService = transactionService;
    }
    
    public Map<String, Object> addCreditCardPayment(String creditCardId, String amount, 
                                                   String paymentDate, String paymentMethod, String notes) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Agregando pago de tarjeta de crédito para tarjeta: {} por monto: {}", creditCardId, amount);
            
            // Validaciones básicas
            if (creditCardId == null || creditCardId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El ID de la tarjeta de crédito es requerido");
                return result;
            }
            
            if (amount == null || amount.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El monto del pago es requerido");
                return result;
            }
            
            // Verificar que la tarjeta existe
            var creditCard = creditCardService.getCreditCardById(creditCardId);
            if (creditCard.isEmpty()) {
                result.put("success", false);
                result.put("message", "Tarjeta de crédito no encontrada");
                return result;
            }
            
            // Crear el pago
            CreditCardPayment payment = new CreditCardPayment();
            payment.setCreditCardId(Long.parseLong(creditCardId));
            
            try {
                payment.setAmountPaid(new BigDecimal(amount));
            } catch (NumberFormatException e) {
                result.put("success", false);
                result.put("message", "El monto debe ser un número válido");
                return result;
            }
            
            // Procesar fecha de pago
            if (paymentDate != null && !paymentDate.trim().isEmpty()) {
                try {
                    payment.setPaymentDate(LocalDate.parse(paymentDate));
                } catch (Exception e) {
                    result.put("success", false);
                    result.put("message", "La fecha de pago debe estar en formato YYYY-MM-DD");
                    return result;
                }
            } else {
                payment.setPaymentDate(LocalDate.now());
            }
            
            // Crear transacción asociada si se proporciona método de pago o notas
            if ((paymentMethod != null && !paymentMethod.trim().isEmpty()) || 
                (notes != null && !notes.trim().isEmpty())) {
                
                Transaction transaction = new Transaction();
                User user = new User();
                user.setId(creditCard.get().getUserId().toString());
                transaction.setUser(user);
                transaction.setType("EXPENSE");
                transaction.setCategory("Credit Card Payment");
                transaction.setAmount(payment.getAmountPaid());
                transaction.setTransactionDate(payment.getPaymentDate());
                
                String description = "Pago tarjeta de crédito";
                if (paymentMethod != null && !paymentMethod.trim().isEmpty()) {
                    description += " - " + paymentMethod;
                }
                if (notes != null && !notes.trim().isEmpty()) {
                    description += " - " + notes;
                }
                transaction.setDescription(description);
                
                Transaction savedTransaction = transactionService.createTransaction(transaction);
                payment.setTransactionId(Long.parseLong(savedTransaction.getId()));
            }
            
            CreditCardPayment savedPayment = creditCardPaymentService.createCreditCardPayment(payment);
            
            result.put("success", true);
            result.put("message", "Pago de tarjeta de crédito registrado exitosamente");
            result.put("payment", savedPayment);
            
        } catch (Exception e) {
            log.error("Error al agregar pago de tarjeta de crédito", e);
            result.put("success", false);
            result.put("message", "Error al agregar pago de tarjeta de crédito: " + e.getMessage());
        }
        
        return result;
    }
    
    public Map<String, Object> listCreditCardPayments(String creditCardId, String startDate, String endDate) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Listando pagos de tarjeta de crédito: {} en rango: {} - {}", creditCardId, startDate, endDate);
            
            if (creditCardId == null || creditCardId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El ID de la tarjeta de crédito es requerido");
                return result;
            }
            
            // Verificar que la tarjeta existe
            var creditCard = creditCardService.getCreditCardById(creditCardId);
            if (creditCard.isEmpty()) {
                result.put("success", false);
                result.put("message", "Tarjeta de crédito no encontrada");
                return result;
            }
            
            List<CreditCardPayment> payments;
            
            if (startDate != null && !startDate.trim().isEmpty() && 
                endDate != null && !endDate.trim().isEmpty()) {
                
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    payments = creditCardPaymentService.getCreditCardPaymentsByCreditCardIdAndDateRange(creditCardId, start, end);
                } catch (Exception e) {
                    result.put("success", false);
                    result.put("message", "Las fechas deben estar en formato YYYY-MM-DD");
                    return result;
                }
            } else {
                payments = creditCardPaymentService.getCreditCardPaymentsByCreditCardId(creditCardId);
            }
            
            result.put("success", true);
            result.put("message", "Pagos de tarjeta de crédito obtenidos exitosamente");
            result.put("payments", payments);
            result.put("count", payments.size());
            
            // Calcular total de pagos
            BigDecimal totalPaid = payments.stream()
                .map(CreditCardPayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            result.put("totalPaid", totalPaid);
            
        } catch (Exception e) {
            log.error("Error al listar pagos de tarjeta de crédito", e);
            result.put("success", false);
            result.put("message", "Error al listar pagos de tarjeta de crédito: " + e.getMessage());
        }
        
        return result;
    }
    
    public Map<String, Object> calculateCreditCardBalance(String creditCardId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Calculando saldo de tarjeta de crédito: {}", creditCardId);
            
            if (creditCardId == null || creditCardId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El ID de la tarjeta de crédito es requerido");
                return result;
            }
            
            // Obtener la tarjeta
            var creditCard = creditCardService.getCreditCardById(creditCardId);
            if (creditCard.isEmpty()) {
                result.put("success", false);
                result.put("message", "Tarjeta de crédito no encontrada");
                return result;
            }
            
            CreditCard card = creditCard.get();
            
            // Obtener todos los pagos realizados
            List<CreditCardPayment> payments = creditCardPaymentService.getCreditCardPaymentsByCreditCardId(creditCardId);
            BigDecimal totalPaid = payments.stream()
                .map(CreditCardPayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Obtener transacciones de gastos con tarjeta de crédito (asumiendo que se marcan con una categoría específica)
            List<Transaction> transactions = transactionService.getTransactionsByUserIdAndCategory(
                card.getUserId().toString(), "Credit Card");
            
            BigDecimal totalSpent = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calcular saldo disponible
            BigDecimal creditLimit = card.getCreditLimit() != null ? card.getCreditLimit() : BigDecimal.ZERO;
            BigDecimal currentBalance = totalSpent.subtract(totalPaid);
            BigDecimal availableCredit = creditLimit.subtract(currentBalance);
            
            Map<String, Object> balanceInfo = new HashMap<>();
            balanceInfo.put("creditLimit", creditLimit);
            balanceInfo.put("totalSpent", totalSpent);
            balanceInfo.put("totalPaid", totalPaid);
            balanceInfo.put("currentBalance", currentBalance);
            balanceInfo.put("availableCredit", availableCredit);
            balanceInfo.put("utilizationPercentage", creditLimit.compareTo(BigDecimal.ZERO) > 0 ? 
                currentBalance.divide(creditLimit, 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal("100")) : 
                BigDecimal.ZERO);
            
            result.put("success", true);
            result.put("message", "Saldo de tarjeta de crédito calculado exitosamente");
            result.put("balanceInfo", balanceInfo);
            result.put("creditCard", card);
            
        } catch (Exception e) {
            log.error("Error al calcular saldo de tarjeta de crédito", e);
            result.put("success", false);
            result.put("message", "Error al calcular saldo de tarjeta de crédito: " + e.getMessage());
        }
        
        return result;
    }
}
