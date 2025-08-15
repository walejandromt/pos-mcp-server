package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.Budget;
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
public class BudgetService {
    
    @Value("${ai.finance.api.budgets.url:http://localhost:8080/api/budgets}")
    private String budgetsApiUrl;
    
    private final RestTemplate restTemplate;
    
    public BudgetService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<Budget> getAllBudgets() {
        log.info("Obteniendo todos los presupuestos desde: {}", budgetsApiUrl);
        ResponseEntity<List<Budget>> response = restTemplate.exchange(
            budgetsApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Budget>>() {}
        );
        return response.getBody();
    }
    
    public Optional<Budget> getBudgetById(Long id) {
        log.info("Obteniendo presupuesto con ID: {} desde: {}", id, budgetsApiUrl);
        try {
            Budget budget = restTemplate.getForObject(budgetsApiUrl + "/{id}", Budget.class, id);
            return Optional.ofNullable(budget);
        } catch (Exception e) {
            log.error("Error al obtener presupuesto con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public List<Budget> getBudgetsByUserId(Long userId) {
        log.info("Obteniendo presupuestos para usuario: {} desde: {}", userId, budgetsApiUrl);
        ResponseEntity<List<Budget>> response = restTemplate.exchange(
            budgetsApiUrl + "/user/{userId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Budget>>() {},
            userId
        );
        return response.getBody();
    }
    
    public List<Budget> getBudgetsByUserIdAndCategory(Long userId, String category) {
        log.info("Obteniendo presupuestos para usuario: {} con categoría: {} desde: {}", userId, category, budgetsApiUrl);
        ResponseEntity<List<Budget>> response = restTemplate.exchange(
            budgetsApiUrl + "/user/{userId}/category/{category}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Budget>>() {},
            userId, category
        );
        return response.getBody();
    }
    
    public Budget createBudget(Budget budget) {
        log.info("Creando nuevo presupuesto para categoría: {}", budget.getTransactionCategory().getCategoryName());
        return restTemplate.postForObject(budgetsApiUrl, budget, Budget.class);
    }
    
    public Budget updateBudget(Long id, Budget budget) {
        log.info("Actualizando presupuesto con ID: {}", id);
        restTemplate.put(budgetsApiUrl + "/{id}", budget, id);
        return getBudgetById(id).orElse(null);
    }
    
    public boolean deleteBudget(Long id) {
        log.info("Eliminando presupuesto con ID: {}", id);
        try {
            restTemplate.delete(budgetsApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar presupuesto con ID: {}", id, e);
            return false;
        }
    }
}
