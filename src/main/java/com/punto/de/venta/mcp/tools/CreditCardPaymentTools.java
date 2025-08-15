package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.CreditCard;
import com.punto.de.venta.mcp.model.CreditCardPayment;
import com.punto.de.venta.mcp.model.Transaction;
import com.punto.de.venta.mcp.model.TransactionCategory;
import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.CreditCardPaymentService;
import com.punto.de.venta.mcp.service.CreditCardService;
import com.punto.de.venta.mcp.service.TransactionCategoryService;
import com.punto.de.venta.mcp.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CreditCardPaymentTools {
    
    private final CreditCardPaymentService creditCardPaymentService;
    private final CreditCardService creditCardService;
    private final TransactionService transactionService;
    private final TransactionCategoryService transactionCategoryService;
    
    public CreditCardPaymentTools(CreditCardPaymentService creditCardPaymentService,
                                CreditCardService creditCardService,
                                TransactionService transactionService,
                                TransactionCategoryService transactionCategoryService) {
        this.creditCardPaymentService = creditCardPaymentService;
        this.creditCardService = creditCardService;
        this.transactionService = transactionService;
        this.transactionCategoryService = transactionCategoryService;
    }
    
    @Tool(name = "agregarPagoTarjetaCredito", description = "Registra un pago hacia la tarjeta de crédito. Requiere el ID de la tarjeta, monto, fecha de pago (opcional), método de pago (opcional) y notas (opcional).")
    public String agregarPagoTarjetaCredito(@ToolParam Long creditCardId, @ToolParam String amount, 
                                           @ToolParam String paymentDate, @ToolParam String paymentMethod, @ToolParam String notes) {
        log.info("Agregando pago de tarjeta de crédito para tarjeta: {} por monto: {}", creditCardId, amount);
        
        if (creditCardId == null) {
            return "Error: El ID de la tarjeta de crédito es requerido";
        }
        
        if (amount == null || amount.trim().isEmpty()) {
            return "Error: El monto del pago es requerido";
        }
        
        try {
            // Verificar que la tarjeta existe
            var creditCard = creditCardService.getCreditCardById(creditCardId);
            if (creditCard.isEmpty()) {
                return "Error: Tarjeta de crédito no encontrada";
            }
            
            // Crear el pago
            CreditCardPayment payment = new CreditCardPayment();
            payment.setCreditCardId(creditCardId);
            
            try {
                payment.setAmountPaid(new BigDecimal(amount));
            } catch (NumberFormatException e) {
                return "Error: El monto debe ser un número válido";
            }
            
            // Procesar fecha de pago
            if (paymentDate != null && !paymentDate.trim().isEmpty()) {
                try {
                    payment.setPaymentDate(LocalDate.parse(paymentDate));
                } catch (Exception e) {
                    return "Error: La fecha de pago debe estar en formato YYYY-MM-DD";
                }
            } else {
                payment.setPaymentDate(LocalDate.now());
            }
            
            // Crear transacción asociada si se proporciona método de pago o notas
            if ((paymentMethod != null && !paymentMethod.trim().isEmpty()) || 
                (notes != null && !notes.trim().isEmpty())) {
                
                Transaction transaction = new Transaction();
                User user = new User();
                user.setId(creditCard.get().getUser().getId());
                transaction.setUser(user);
                transaction.setType("EXPENSE");
                // Obtener o crear categoría para pagos de tarjeta
                TransactionCategory category = getCategoryByName("Credit Card Payment", creditCard.get().getUser().getId());
                if (category != null) {
                    transaction.setTransactionCategory(category);
                }
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
                payment.setTransactionId(savedTransaction.getId());
            }
            
            CreditCardPayment savedPayment = creditCardPaymentService.createCreditCardPayment(payment);
            
            return String.format("Pago de tarjeta de crédito registrado exitosamente - ID: %s, Monto: %s, Fecha: %s", 
                savedPayment.getId(), savedPayment.getAmountPaid(), savedPayment.getPaymentDate());
            
        } catch (Exception e) {
            log.error("Error al agregar pago de tarjeta de crédito", e);
            return "Error al agregar pago de tarjeta de crédito: " + e.getMessage();
        }
    }
    
    @Tool(name = "listarPagosTarjetaCredito", description = "Lista pagos hechos a una tarjeta en un periodo específico. Requiere el ID de la tarjeta, fecha de inicio (opcional) y fecha de fin (opcional) en formato YYYY-MM-DD.")
    public String listarPagosTarjetaCredito(@ToolParam Long creditCardId, @ToolParam String startDate, @ToolParam String endDate) {
        log.info("Listando pagos de tarjeta de crédito: {} en rango: {} - {}", creditCardId, startDate, endDate);
        
        if (creditCardId == null) {
            return "Error: El ID de la tarjeta de crédito es requerido";
        }
        
        try {
            // Verificar que la tarjeta existe
            var creditCard = creditCardService.getCreditCardById(creditCardId);
            if (creditCard.isEmpty()) {
                return "Error: Tarjeta de crédito no encontrada";
            }
            
            List<CreditCardPayment> payments;
            
            if (startDate != null && !startDate.trim().isEmpty() && 
                endDate != null && !endDate.trim().isEmpty()) {
                
                try {
                    LocalDate start = LocalDate.parse(startDate);
                    LocalDate end = LocalDate.parse(endDate);
                    payments = creditCardPaymentService.getCreditCardPaymentsByCreditCardIdAndDateRange(creditCardId, start, end);
                } catch (Exception e) {
                    return "Error: Las fechas deben estar en formato YYYY-MM-DD";
                }
            } else {
                payments = creditCardPaymentService.getCreditCardPaymentsByCreditCardId(creditCardId);
            }
            
            if (payments.isEmpty()) {
                return "No se encontraron pagos para esta tarjeta";
            }
            
            // Calcular total de pagos
            BigDecimal totalPaid = payments.stream()
                .map(CreditCardPayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Pagos de tarjeta %s (****%s):\n\n", 
                creditCard.get().getCardName(), creditCard.get().getLastFourDigits()));
            
            for (CreditCardPayment payment : payments) {
                result.append(String.format("- Fecha: %s, Monto: %s\n", 
                    payment.getPaymentDate(), payment.getAmountPaid()));
            }
            
            result.append(String.format("\nTotal de pagos: %d\n", payments.size()));
            result.append(String.format("Monto total pagado: %s", totalPaid));
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error al listar pagos de tarjeta de crédito", e);
            return "Error al listar pagos de tarjeta de crédito: " + e.getMessage();
        }
    }
    
    @Tool(name = "calcularSaldoTarjetaCredito", description = "Calcula saldo actual considerando el límite, compras registradas y pagos. Requiere el ID de la tarjeta.")
    public String calcularSaldoTarjetaCredito(@ToolParam Long creditCardId) {
        log.info("Calculando saldo de tarjeta de crédito: {}", creditCardId);
        
        if (creditCardId == null) {
            return "Error: El ID de la tarjeta de crédito es requerido";
        }
        
        try {
            // Obtener la tarjeta
            var creditCard = creditCardService.getCreditCardById(creditCardId);
            if (creditCard.isEmpty()) {
                return "Error: Tarjeta de crédito no encontrada";
            }
            
            CreditCard card = creditCard.get();
            
            // Obtener todos los pagos realizados
            List<CreditCardPayment> payments = creditCardPaymentService.getCreditCardPaymentsByCreditCardId(creditCardId);
            BigDecimal totalPaid = payments.stream()
                .map(CreditCardPayment::getAmountPaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Obtener transacciones de gastos con tarjeta de crédito (asumiendo que se marcan con creditCardId)
            // En lugar de buscar por categoría, buscaremos por todos los gastos del usuario y filtrar por creditCardId
            List<Transaction> allTransactions = transactionService.getTransactionsByUserId(card.getUser().getId());
            List<Transaction> transactions = allTransactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()) && 
                           creditCardId.toString().equals(t.getCreditCardId()))
                .collect(Collectors.toList());
            
            BigDecimal totalSpent = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Calcular saldo disponible
            BigDecimal creditLimit = card.getCreditLimit() != null ? card.getCreditLimit() : BigDecimal.ZERO;
            BigDecimal currentBalance = totalSpent.subtract(totalPaid);
            BigDecimal availableCredit = creditLimit.subtract(currentBalance);
            BigDecimal utilizationPercentage = creditLimit.compareTo(BigDecimal.ZERO) > 0 ? 
                currentBalance.divide(creditLimit, 4, java.math.RoundingMode.HALF_UP).multiply(new BigDecimal("100")) : 
                BigDecimal.ZERO;
            
            StringBuilder result = new StringBuilder();
            result.append(String.format("Estado de cuenta - %s (****%s):\n\n", card.getCardName(), card.getLastFourDigits()));
            result.append(String.format("Límite de crédito: %s\n", creditLimit));
            result.append(String.format("Total gastado: %s\n", totalSpent));
            result.append(String.format("Total pagado: %s\n", totalPaid));
            result.append(String.format("Saldo actual: %s\n", currentBalance));
            result.append(String.format("Crédito disponible: %s\n", availableCredit));
            result.append(String.format("Porcentaje de utilización: %s%%\n", utilizationPercentage));
            
            if (currentBalance.compareTo(BigDecimal.ZERO) > 0) {
                result.append("\n⚠️ Tienes saldo pendiente en esta tarjeta");
            } else {
                result.append("\n✅ La tarjeta está al día");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("Error al calcular saldo de tarjeta de crédito", e);
            return "Error al calcular saldo de tarjeta de crédito: " + e.getMessage();
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
