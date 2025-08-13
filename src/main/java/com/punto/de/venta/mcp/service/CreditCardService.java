package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.CreditCard;
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
public class CreditCardService {
    
    @Value("${ai.finance.api.credit-cards.url:http://localhost:8080/api/credit-cards}")
    private String creditCardsApiUrl;
    
    private final RestTemplate restTemplate;
    
    public CreditCardService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<CreditCard> getAllCreditCards() {
        log.info("Obteniendo todas las tarjetas de crédito desde: {}", creditCardsApiUrl);
        ResponseEntity<List<CreditCard>> response = restTemplate.exchange(
            creditCardsApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CreditCard>>() {}
        );
        return response.getBody();
    }
    
    public Optional<CreditCard> getCreditCardById(Long id) {
        log.info("Obteniendo tarjeta de crédito con ID: {} desde: {}", id, creditCardsApiUrl);
        try {
            CreditCard creditCard = restTemplate.getForObject(creditCardsApiUrl + "/{id}", CreditCard.class, id);
            return Optional.ofNullable(creditCard);
        } catch (Exception e) {
            log.error("Error al obtener tarjeta de crédito con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public List<CreditCard> getCreditCardsByUserId(Long userId) {
        log.info("Obteniendo tarjetas de crédito para usuario: {} desde: {}", userId, creditCardsApiUrl);
        ResponseEntity<List<CreditCard>> response = restTemplate.exchange(
            creditCardsApiUrl + "/user/{userId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CreditCard>>() {},
            userId
        );
        return response.getBody();
    }
    
    public List<CreditCard> searchCreditCardsByUserIdAndCardName(Long userId, String cardName) {
        log.info("Buscando tarjetas de crédito para usuario: {} con nombre: {} desde: {}", userId, cardName, creditCardsApiUrl);
        ResponseEntity<List<CreditCard>> response = restTemplate.exchange(
            creditCardsApiUrl + "/user/{userId}/search?cardName={cardName}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CreditCard>>() {},
            userId, cardName
        );
        return response.getBody();
    }
    
    public CreditCard createCreditCard(CreditCard creditCard) {
        log.info("Creando nueva tarjeta de crédito: {}", creditCard.getCardName());
        return restTemplate.postForObject(creditCardsApiUrl, creditCard, CreditCard.class);
    }
    
    public CreditCard updateCreditCard(Long id, CreditCard creditCard) {
        log.info("Actualizando tarjeta de crédito con ID: {}", id);
        restTemplate.put(creditCardsApiUrl + "/{id}", creditCard, id);
        return getCreditCardById(id).orElse(null);
    }
    
    public boolean deleteCreditCard(Long id) {
        log.info("Eliminando tarjeta de crédito con ID: {}", id);
        try {
            restTemplate.delete(creditCardsApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar tarjeta de crédito con ID: {}", id, e);
            return false;
        }
    }
}
