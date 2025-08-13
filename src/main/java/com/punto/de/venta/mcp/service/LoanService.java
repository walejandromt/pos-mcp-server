package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.Loan;
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
public class LoanService {
    
    @Value("${ai.finance.api.loans.url:http://localhost:8080/api/loans}")
    private String loansApiUrl;
    
    private final RestTemplate restTemplate;
    
    public LoanService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<Loan> getAllLoans() {
        log.info("Obteniendo todos los préstamos desde: {}", loansApiUrl);
        ResponseEntity<List<Loan>> response = restTemplate.exchange(
            loansApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Loan>>() {}
        );
        return response.getBody();
    }
    
    public Optional<Loan> getLoanById(Long id) {
        log.info("Obteniendo préstamo con ID: {} desde: {}", id, loansApiUrl);
        try {
            Loan loan = restTemplate.getForObject(loansApiUrl + "/{id}", Loan.class, id);
            return Optional.ofNullable(loan);
        } catch (Exception e) {
            log.error("Error al obtener préstamo con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public List<Loan> getLoansByUserId(Long userId) {
        log.info("Obteniendo préstamos para usuario: {} desde: {}", userId, loansApiUrl);
        ResponseEntity<List<Loan>> response = restTemplate.exchange(
            loansApiUrl + "/user/{userId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<Loan>>() {},
            userId
        );
        return response.getBody();
    }
    
    public Loan createLoan(Loan loan) {
        log.info("Creando nuevo préstamo: {}", loan.getDescription());
        return restTemplate.postForObject(loansApiUrl, loan, Loan.class);
    }
    
    public Loan updateLoan(Long id, Loan loan) {
        log.info("Actualizando préstamo con ID: {}", id);
        restTemplate.put(loansApiUrl + "/{id}", loan, id);
        return getLoanById(id).orElse(null);
    }
    
    public boolean deleteLoan(Long id) {
        log.info("Eliminando préstamo con ID: {}", id);
        try {
            restTemplate.delete(loansApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar préstamo con ID: {}", id, e);
            return false;
        }
    }
}
