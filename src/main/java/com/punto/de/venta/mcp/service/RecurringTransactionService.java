package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.RecurringTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class RecurringTransactionService {
    
    @Value("${ai.finance.api.recurring-transactions.url:http://localhost:8080/api/recurring-transactions}")
    private String recurringTransactionsApiUrl;
    
    private final RestTemplate restTemplate;
    
    public RecurringTransactionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<RecurringTransaction> getAllRecurringTransactions() {
        log.info("Obteniendo todas las transacciones recurrentes desde: {}", recurringTransactionsApiUrl);
        ResponseEntity<List<RecurringTransaction>> response = restTemplate.exchange(
            recurringTransactionsApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<RecurringTransaction>>() {}
        );
        return response.getBody();
    }
    
    public Optional<RecurringTransaction> getRecurringTransactionById(String id) {
        log.info("Obteniendo transacción recurrente con ID: {} desde: {}", id, recurringTransactionsApiUrl);
        try {
            RecurringTransaction recurringTransaction = restTemplate.getForObject(recurringTransactionsApiUrl + "/{id}", RecurringTransaction.class, id);
            return Optional.ofNullable(recurringTransaction);
        } catch (Exception e) {
            log.error("Error al obtener transacción recurrente con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public List<RecurringTransaction> getRecurringTransactionsByUserId(String userId) {
        log.info("Obteniendo transacciones recurrentes para usuario: {} desde: {}", userId, recurringTransactionsApiUrl);
        ResponseEntity<List<RecurringTransaction>> response = restTemplate.exchange(
            recurringTransactionsApiUrl + "/user/{userId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<RecurringTransaction>>() {},
            userId
        );
        return response.getBody();
    }
    
    public List<RecurringTransaction> getRecurringTransactionsByUserIdAndType(String userId, String type) {
        log.info("Obteniendo transacciones recurrentes para usuario: {} con tipo: {} desde: {}", userId, type, recurringTransactionsApiUrl);
        ResponseEntity<List<RecurringTransaction>> response = restTemplate.exchange(
            recurringTransactionsApiUrl + "/user/{userId}/type/{type}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<RecurringTransaction>>() {},
            userId, type
        );
        return response.getBody();
    }
    
    public RecurringTransaction createRecurringTransaction(RecurringTransaction recurringTransaction) {
        log.info("Creando nueva transacción recurrente: {}", recurringTransaction.getDescription());
        return restTemplate.postForObject(recurringTransactionsApiUrl, recurringTransaction, RecurringTransaction.class);
    }
    
    public RecurringTransaction updateRecurringTransaction(String id, RecurringTransaction recurringTransaction) {
        log.info("Actualizando transacción recurrente con ID: {}", id);
        restTemplate.put(recurringTransactionsApiUrl + "/{id}", recurringTransaction, id);
        return getRecurringTransactionById(id).orElse(null);
    }
    
    public boolean deleteRecurringTransaction(String id) {
        log.info("Eliminando transacción recurrente con ID: {}", id);
        try {
            restTemplate.delete(recurringTransactionsApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar transacción recurrente con ID: {}", id, e);
            return false;
        }
    }
}
