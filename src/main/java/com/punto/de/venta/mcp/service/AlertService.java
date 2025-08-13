package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.Alert;
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
public class AlertService {
    
    @Value("${ai.finance.api.alerts.url:http://localhost:8080/api/alerts}")
    private String alertsApiUrl;
    
    private final RestTemplate restTemplate;
    
    public AlertService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<Alert> getAllAlerts() {
        log.info("Obteniendo todas las alertas desde: {}", alertsApiUrl);
        ResponseEntity<List<Alert>> response = restTemplate.exchange(
            alertsApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Alert>>() {}
        );
        return response.getBody();
    }
    
    public Optional<Alert> getAlertById(Long id) {
        log.info("Obteniendo alerta con ID: {} desde: {}", id, alertsApiUrl);
        try {
            Alert alert = restTemplate.getForObject(alertsApiUrl + "/{id}", Alert.class, id);
            return Optional.ofNullable(alert);
        } catch (Exception e) {
            log.error("Error al obtener alerta con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public List<Alert> getAlertsByUserId(Long userId) {
        log.info("Obteniendo alertas para usuario: {} desde: {}", userId, alertsApiUrl);
        ResponseEntity<List<Alert>> response = restTemplate.exchange(
            alertsApiUrl + "/user/{userId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Alert>>() {},
            userId
        );
        return response.getBody();
    }
    
    public List<Alert> getAlertsByUserIdAndStatus(Long userId, String status) {
        log.info("Obteniendo alertas para usuario: {} con estado: {} desde: {}", userId, status, alertsApiUrl);
        ResponseEntity<List<Alert>> response = restTemplate.exchange(
            alertsApiUrl + "/user/{userId}/status/{status}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Alert>>() {},
            userId, status
        );
        return response.getBody();
    }
    
    public Alert createAlert(Alert alert) {
        log.info("Creando nueva alerta: {}", alert.getAlertType());
        return restTemplate.postForObject(alertsApiUrl, alert, Alert.class);
    }
    
    public Alert updateAlert(Long id, Alert alert) {
        log.info("Actualizando alerta con ID: {}", id);
        restTemplate.put(alertsApiUrl + "/{id}", alert, id);
        return getAlertById(id).orElse(null);
    }
    
    public boolean deleteAlert(Long id) {
        log.info("Eliminando alerta con ID: {}", id);
        try {
            restTemplate.delete(alertsApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar alerta con ID: {}", id, e);
            return false;
        }
    }
}
