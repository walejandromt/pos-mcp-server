package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.Transaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class TransactionService {
    
    @Value("${ai.finance.api.transactions.url:http://localhost:8080/api/transactions}")
    private String transactionsApiUrl;
    
    private final RestTemplate restTemplate;
    
    public TransactionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<Transaction> getAllTransactions() {
        log.info("Obteniendo todas las transacciones desde: {}", transactionsApiUrl);
        ResponseEntity<List<Transaction>> response = restTemplate.exchange(
            transactionsApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Transaction>>() {}
        );
        return response.getBody();
    }
    
    public Optional<Transaction> getTransactionById(String id) {
        log.info("Obteniendo transacción con ID: {} desde: {}", id, transactionsApiUrl);
        try {
            Transaction transaction = restTemplate.getForObject(transactionsApiUrl + "/{id}", Transaction.class, id);
            return Optional.ofNullable(transaction);
        } catch (Exception e) {
            log.error("Error al obtener transacción con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public List<Transaction> getTransactionsByUserId(String userId) {
        log.info("Obteniendo transacciones para usuario: {} desde: {}", userId, transactionsApiUrl);
        ResponseEntity<List<Transaction>> response = restTemplate.exchange(
            transactionsApiUrl + "/user/{userId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Transaction>>() {},
            userId
        );
        return response.getBody();
    }
    
    public List<Transaction> getTransactionsByUserIdAndType(String userId, String type) {
        log.info("Obteniendo transacciones para usuario: {} con tipo: {} desde: {}", userId, type, transactionsApiUrl);
        ResponseEntity<List<Transaction>> response = restTemplate.exchange(
            transactionsApiUrl + "/user/{userId}/type/{type}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Transaction>>() {},
            userId, type
        );
        return response.getBody();
    }
    
    public List<Transaction> getTransactionsByUserIdAndCategory(String userId, String category) {
        log.info("Obteniendo transacciones para usuario: {} con categoría: {} desde: {}", userId, category, transactionsApiUrl);
        ResponseEntity<List<Transaction>> response = restTemplate.exchange(
            transactionsApiUrl + "/user/{userId}/category/{category}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Transaction>>() {},
            userId, category
        );
        return response.getBody();
    }
    
    public List<Transaction> getTransactionsByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        log.info("Obteniendo transacciones para usuario: {} en rango de fechas: {} - {} desde: {}", userId, startDate, endDate, transactionsApiUrl);
        ResponseEntity<List<Transaction>> response = restTemplate.exchange(
            transactionsApiUrl + "/user/{userId}/date-range?startDate={startDate}&endDate={endDate}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Transaction>>() {},
            userId, startDate, endDate
        );
        return response.getBody();
    }
    
    public BigDecimal getSumAmountByUserIdAndTypeAndDateRange(String userId, String type, LocalDate startDate, LocalDate endDate) {
        log.info("Calculando suma de transacciones para usuario: {} con tipo: {} en rango: {} - {} desde: {}", userId, type, startDate, endDate, transactionsApiUrl);
        try {
            BigDecimal sum = restTemplate.getForObject(
                transactionsApiUrl + "/user/{userId}/sum?type={type}&startDate={startDate}&endDate={endDate}",
                BigDecimal.class,
                userId, type, startDate, endDate
            );
            return sum != null ? sum : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Error al calcular suma de transacciones", e);
            return BigDecimal.ZERO;
        }
    }
    
    public Transaction createTransaction(Transaction transaction) {
        log.info("Creando nueva transacción: {}", transaction.getDescription());
        return restTemplate.postForObject(transactionsApiUrl, transaction, Transaction.class);
    }
    
    public Transaction updateTransaction(String id, Transaction transaction) {
        log.info("Actualizando transacción con ID: {}", id);
        restTemplate.put(transactionsApiUrl + "/{id}", transaction, id);
        return getTransactionById(id).orElse(null);
    }
    
    public boolean deleteTransaction(String id) {
        log.info("Eliminando transacción con ID: {}", id);
        try {
            restTemplate.delete(transactionsApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar transacción con ID: {}", id, e);
            return false;
        }
    }
}
