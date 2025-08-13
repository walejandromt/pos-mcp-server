package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.CreditCard;
import com.punto.de.venta.mcp.service.CreditCardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CreditCardTools {
    
    private final CreditCardService creditCardService;
    
    public CreditCardTools(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }
    
    public Map<String, Object> addCreditCard(String userId, String name, String bankName, 
                                           String lastDigits, String creditLimit, String currency) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Agregando tarjeta de crédito para usuario: {} con nombre: {}", userId, name);
            
            // Validaciones básicas
            if (userId == null || userId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El ID del usuario es requerido");
                return result;
            }
            
            if (name == null || name.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El nombre de la tarjeta es requerido");
                return result;
            }
            
            if (lastDigits == null || lastDigits.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "Los últimos dígitos son requeridos");
                return result;
            }
            
            // Crear la tarjeta de crédito
            CreditCard creditCard = new CreditCard();
            creditCard.setUserId(Long.parseLong(userId));
            creditCard.setCardName(name);
            creditCard.setLastFourDigits(lastDigits);
            
            if (creditLimit != null && !creditLimit.trim().isEmpty()) {
                try {
                    creditCard.setCreditLimit(new BigDecimal(creditLimit));
                } catch (NumberFormatException e) {
                    result.put("success", false);
                    result.put("message", "El límite de crédito debe ser un número válido");
                    return result;
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
                creditCard.setMetadata(metadata.toString());
            }
            
            CreditCard savedCreditCard = creditCardService.createCreditCard(creditCard);
            
            result.put("success", true);
            result.put("message", "Tarjeta de crédito agregada exitosamente");
            result.put("creditCard", savedCreditCard);
            
        } catch (Exception e) {
            log.error("Error al agregar tarjeta de crédito", e);
            result.put("success", false);
            result.put("message", "Error al agregar tarjeta de crédito: " + e.getMessage());
        }
        
        return result;
    }
    
    public Map<String, Object> listCreditCards(String userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Listando tarjetas de crédito para usuario: {}", userId);
            
            if (userId == null || userId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El ID del usuario es requerido");
                return result;
            }
            
            List<CreditCard> creditCards = creditCardService.getCreditCardsByUserId(userId);
            
            result.put("success", true);
            result.put("message", "Tarjetas de crédito obtenidas exitosamente");
            result.put("creditCards", creditCards);
            result.put("count", creditCards.size());
            
        } catch (Exception e) {
            log.error("Error al listar tarjetas de crédito", e);
            result.put("success", false);
            result.put("message", "Error al listar tarjetas de crédito: " + e.getMessage());
        }
        
        return result;
    }
    
    public Map<String, Object> updateCreditCard(String creditCardId, Map<String, Object> fields) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Actualizando tarjeta de crédito con ID: {}", creditCardId);
            
            if (creditCardId == null || creditCardId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El ID de la tarjeta de crédito es requerido");
                return result;
            }
            
            // Obtener la tarjeta existente
            var existingCreditCard = creditCardService.getCreditCardById(creditCardId);
            if (existingCreditCard.isEmpty()) {
                result.put("success", false);
                result.put("message", "Tarjeta de crédito no encontrada");
                return result;
            }
            
            CreditCard creditCard = existingCreditCard.get();
            
            // Actualizar campos permitidos
            if (fields.containsKey("name") && fields.get("name") != null) {
                creditCard.setCardName(fields.get("name").toString());
            }
            
            if (fields.containsKey("last_digits") && fields.get("last_digits") != null) {
                creditCard.setLastFourDigits(fields.get("last_digits").toString());
            }
            
            if (fields.containsKey("credit_limit") && fields.get("credit_limit") != null) {
                try {
                    creditCard.setCreditLimit(new BigDecimal(fields.get("credit_limit").toString()));
                } catch (NumberFormatException e) {
                    result.put("success", false);
                    result.put("message", "El límite de crédito debe ser un número válido");
                    return result;
                }
            }
            
            if (fields.containsKey("cut_off_day") && fields.get("cut_off_day") != null) {
                try {
                    creditCard.setCutOffDay(Integer.parseInt(fields.get("cut_off_day").toString()));
                } catch (NumberFormatException e) {
                    result.put("success", false);
                    result.put("message", "El día de corte debe ser un número válido");
                    return result;
                }
            }
            
            if (fields.containsKey("payment_due_day") && fields.get("payment_due_day") != null) {
                try {
                    creditCard.setPaymentDueDay(Integer.parseInt(fields.get("payment_due_day").toString()));
                } catch (NumberFormatException e) {
                    result.put("success", false);
                    result.put("message", "El día de pago debe ser un número válido");
                    return result;
                }
            }
            
            CreditCard updatedCreditCard = creditCardService.updateCreditCard(creditCardId, creditCard);
            
            result.put("success", true);
            result.put("message", "Tarjeta de crédito actualizada exitosamente");
            result.put("creditCard", updatedCreditCard);
            
        } catch (Exception e) {
            log.error("Error al actualizar tarjeta de crédito", e);
            result.put("success", false);
            result.put("message", "Error al actualizar tarjeta de crédito: " + e.getMessage());
        }
        
        return result;
    }
    
    public Map<String, Object> deleteCreditCard(String creditCardId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            log.info("Eliminando tarjeta de crédito con ID: {}", creditCardId);
            
            if (creditCardId == null || creditCardId.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "El ID de la tarjeta de crédito es requerido");
                return result;
            }
            
            // Verificar si la tarjeta existe
            var existingCreditCard = creditCardService.getCreditCardById(creditCardId);
            if (existingCreditCard.isEmpty()) {
                result.put("success", false);
                result.put("message", "Tarjeta de crédito no encontrada");
                return result;
            }
            
            boolean deleted = creditCardService.deleteCreditCard(creditCardId);
            
            if (deleted) {
                result.put("success", true);
                result.put("message", "Tarjeta de crédito eliminada exitosamente");
            } else {
                result.put("success", false);
                result.put("message", "No se pudo eliminar la tarjeta de crédito");
            }
            
        } catch (Exception e) {
            log.error("Error al eliminar tarjeta de crédito", e);
            result.put("success", false);
            result.put("message", "Error al eliminar tarjeta de crédito: " + e.getMessage());
        }
        
        return result;
    }
}
