package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.CreditCardPayment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class CreditCardPaymentService {
    
    @Value("${ai.finance.api.credit-card-payments.url:http://localhost:8080/api/credit-card-payments}")
    private String creditCardPaymentsApiUrl;
    
    private final RestTemplate restTemplate;
    
    public CreditCardPaymentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<CreditCardPayment> getAllCreditCardPayments() {
        log.info("Obteniendo todos los pagos de tarjetas de crédito desde: {}", creditCardPaymentsApiUrl);
        ResponseEntity<List<CreditCardPayment>> response = restTemplate.exchange(
            creditCardPaymentsApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CreditCardPayment>>() {}
        );
        return response.getBody();
    }
    
    public Optional<CreditCardPayment> getCreditCardPaymentById(String id) {
        log.info("Obteniendo pago de tarjeta de crédito con ID: {} desde: {}", id, creditCardPaymentsApiUrl);
        try {
            CreditCardPayment payment = restTemplate.getForObject(creditCardPaymentsApiUrl + "/{id}", CreditCardPayment.class, id);
            return Optional.ofNullable(payment);
        } catch (Exception e) {
            log.error("Error al obtener pago de tarjeta de crédito con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public List<CreditCardPayment> getCreditCardPaymentsByCreditCardId(String creditCardId) {
        log.info("Obteniendo pagos para tarjeta de crédito: {} desde: {}", creditCardId, creditCardPaymentsApiUrl);
        ResponseEntity<List<CreditCardPayment>> response = restTemplate.exchange(
            creditCardPaymentsApiUrl + "/credit-card/{creditCardId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CreditCardPayment>>() {},
            creditCardId
        );
        return response.getBody();
    }
    
    public List<CreditCardPayment> getCreditCardPaymentsByCreditCardIdAndDateRange(String creditCardId, LocalDate startDate, LocalDate endDate) {
        log.info("Obteniendo pagos para tarjeta de crédito: {} en rango de fechas: {} - {} desde: {}", creditCardId, startDate, endDate, creditCardPaymentsApiUrl);
        ResponseEntity<List<CreditCardPayment>> response = restTemplate.exchange(
            creditCardPaymentsApiUrl + "/credit-card/{creditCardId}/date-range?startDate={startDate}&endDate={endDate}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CreditCardPayment>>() {},
            creditCardId, startDate, endDate
        );
        return response.getBody();
    }
    
    public List<CreditCardPayment> getCreditCardPaymentsByTransactionId(String transactionId) {
        log.info("Obteniendo pagos para transacción: {} desde: {}", transactionId, creditCardPaymentsApiUrl);
        ResponseEntity<List<CreditCardPayment>> response = restTemplate.exchange(
            creditCardPaymentsApiUrl + "/transaction/{transactionId}",
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<CreditCardPayment>>() {},
            transactionId
        );
        return response.getBody();
    }
    
    public CreditCardPayment createCreditCardPayment(CreditCardPayment payment) {
        log.info("Creando nuevo pago de tarjeta de crédito por monto: {}", payment.getAmountPaid());
        return restTemplate.postForObject(creditCardPaymentsApiUrl, payment, CreditCardPayment.class);
    }
    
    public CreditCardPayment updateCreditCardPayment(String id, CreditCardPayment payment) {
        log.info("Actualizando pago de tarjeta de crédito con ID: {}", id);
        restTemplate.put(creditCardPaymentsApiUrl + "/{id}", payment, id);
        return getCreditCardPaymentById(id).orElse(null);
    }
    
    public boolean deleteCreditCardPayment(String id) {
        log.info("Eliminando pago de tarjeta de crédito con ID: {}", id);
        try {
            restTemplate.delete(creditCardPaymentsApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar pago de tarjeta de crédito con ID: {}", id, e);
            return false;
        }
    }
}
