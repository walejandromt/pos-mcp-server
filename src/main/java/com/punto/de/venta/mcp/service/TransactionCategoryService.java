package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.TransactionCategory;
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
public class TransactionCategoryService {
    
    @Value("${ai.finance.api.transaction-categories.url:http://localhost:8080/api/transaction-categories}")
    private String transactionCategoriesApiUrl;
    
    private final RestTemplate restTemplate;
    
    public TransactionCategoryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<TransactionCategory> getAllTransactionCategories() {
        log.info("Obteniendo todas las categorías de transacciones desde: {}", transactionCategoriesApiUrl);
        ResponseEntity<List<TransactionCategory>> response = restTemplate.exchange(
            transactionCategoriesApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<TransactionCategory>>() {}
        );
        return response.getBody();
    }
    
    public Optional<TransactionCategory> getTransactionCategoryById(Long id) {
        log.info("Obteniendo categoría de transacción con ID: {} desde: {}", id, transactionCategoriesApiUrl);
        try {
            TransactionCategory category = restTemplate.getForObject(transactionCategoriesApiUrl + "/{id}", TransactionCategory.class, id);
            return Optional.ofNullable(category);
        } catch (Exception e) {
            log.error("Error al obtener categoría de transacción con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public List<TransactionCategory> getTransactionCategoriesByUserId(Long userId) {
        log.info("Obteniendo categorías de transacciones para usuario: {} desde: {}", userId, transactionCategoriesApiUrl);
        ResponseEntity<List<TransactionCategory>> response = restTemplate.exchange(
            transactionCategoriesApiUrl + "/user/{userId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<TransactionCategory>>() {},
            userId
        );
        return response.getBody();
    }
    
    public List<TransactionCategory> searchTransactionCategoriesByUserIdAndCategoryName(Long userId, String categoryName) {
        log.info("Buscando categorías de transacciones para usuario: {} con nombre: {} desde: {}", userId, categoryName, transactionCategoriesApiUrl);
        ResponseEntity<List<TransactionCategory>> response = restTemplate.exchange(
            transactionCategoriesApiUrl + "/user/{userId}/search?categoryName={categoryName}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<TransactionCategory>>() {},
            userId, categoryName
        );
        return response.getBody();
    }
    
    public List<TransactionCategory> getTransactionCategoriesByParentCategoryId(Long parentCategoryId) {
        log.info("Obteniendo categorías de transacciones con categoría padre: {} desde: {}", parentCategoryId, transactionCategoriesApiUrl);
        ResponseEntity<List<TransactionCategory>> response = restTemplate.exchange(
            transactionCategoriesApiUrl + "/parent/{parentCategoryId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<TransactionCategory>>() {},
            parentCategoryId
        );
        return response.getBody();
    }
    
    public TransactionCategory createTransactionCategory(TransactionCategory category) {
        log.info("Creando nueva categoría de transacción: {}", category.getCategoryName());
        return restTemplate.postForObject(transactionCategoriesApiUrl, category, TransactionCategory.class);
    }
    
    public TransactionCategory updateTransactionCategory(Long id, TransactionCategory category) {
        log.info("Actualizando categoría de transacción con ID: {}", id);
        restTemplate.put(transactionCategoriesApiUrl + "/{id}", category, id);
        return getTransactionCategoryById(id).orElse(null);
    }
    
    public boolean deleteTransactionCategory(Long id) {
        log.info("Eliminando categoría de transacción con ID: {}", id);
        try {
            restTemplate.delete(transactionCategoriesApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar categoría de transacción con ID: {}", id, e);
            return false;
        }
    }
}
