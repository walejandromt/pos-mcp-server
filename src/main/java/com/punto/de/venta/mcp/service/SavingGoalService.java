package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.SavingGoal;
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
public class SavingGoalService {
    
    @Value("${ai.finance.api.saving-goals.url:http://localhost:8080/api/saving-goals}")
    private String savingGoalsApiUrl;
    
    private final RestTemplate restTemplate;
    
    public SavingGoalService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<SavingGoal> getAllSavingGoals() {
        log.info("Obteniendo todas las metas de ahorro desde: {}", savingGoalsApiUrl);
        ResponseEntity<List<SavingGoal>> response = restTemplate.exchange(
            savingGoalsApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<SavingGoal>>() {}
        );
        return response.getBody();
    }
    
    public Optional<SavingGoal> getSavingGoalById(Long id) {
        log.info("Obteniendo meta de ahorro con ID: {} desde: {}", id, savingGoalsApiUrl);
        try {
            SavingGoal savingGoal = restTemplate.getForObject(savingGoalsApiUrl + "/{id}", SavingGoal.class, id);
            return Optional.ofNullable(savingGoal);
        } catch (Exception e) {
            log.error("Error al obtener meta de ahorro con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public List<SavingGoal> getSavingGoalsByUserId(Long userId) {
        log.info("Obteniendo metas de ahorro para usuario: {} desde: {}", userId, savingGoalsApiUrl);
        ResponseEntity<List<SavingGoal>> response = restTemplate.exchange(
            savingGoalsApiUrl + "/user/{userId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<SavingGoal>>() {},
            userId
        );
        return response.getBody();
    }
    
    public SavingGoal createSavingGoal(SavingGoal savingGoal) {
        log.info("Creando nueva meta de ahorro: {}", savingGoal.getGoalName());
        return restTemplate.postForObject(savingGoalsApiUrl, savingGoal, SavingGoal.class);
    }
    
    public SavingGoal updateSavingGoal(Long id, SavingGoal savingGoal) {
        log.info("Actualizando meta de ahorro con ID: {}", id);
        restTemplate.put(savingGoalsApiUrl + "/{id}", savingGoal, id);
        return getSavingGoalById(id).orElse(null);
    }
    
    public boolean deleteSavingGoal(Long id) {
        log.info("Eliminando meta de ahorro con ID: {}", id);
        try {
            restTemplate.delete(savingGoalsApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar meta de ahorro con ID: {}", id, e);
            return false;
        }
    }
}
