package com.punto.de.venta.mcp.service;

import com.punto.de.venta.mcp.model.User;
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
public class UserService {
    
    @Value("${ai.finance.api.users.url:http://localhost:8080/api/users}")
    private String usersApiUrl;
    
    private final RestTemplate restTemplate;
    
    public UserService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    public List<User> getAllUsers() {
        log.info("Obteniendo todos los usuarios desde: {}", usersApiUrl);
        ResponseEntity<List<User>> response = restTemplate.exchange(
            usersApiUrl,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<User>>() {}
        );
        return response.getBody();
    }
    
    public Optional<User> getUserById(String id) {
        log.info("Obteniendo usuario con ID: {} desde: {}", id, usersApiUrl);
        try {
            User user = restTemplate.getForObject(usersApiUrl + "/{id}", User.class, id);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            log.error("Error al obtener usuario con ID: {}", id, e);
            return Optional.empty();
        }
    }
    
    public Optional<User> getUserByPhone(String phone) {
        log.info("Obteniendo usuario con teléfono: {} desde: {}", phone, usersApiUrl);
        try {
            User user = restTemplate.getForObject(usersApiUrl + "/phone/{phone}", User.class, phone);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            log.error("Error al obtener usuario con teléfono: {}", phone, e);
            return Optional.empty();
        }
    }
    
    public Optional<User> getUserByEmail(String email) {
        log.info("Obteniendo usuario con email: {} desde: {}", email, usersApiUrl);
        try {
            User user = restTemplate.getForObject(usersApiUrl + "/email/{email}", User.class, email);
            return Optional.ofNullable(user);
        } catch (Exception e) {
            log.error("Error al obtener usuario con email: {}", email, e);
            return Optional.empty();
        }
    }
    
    public User createUser(User user) {
        log.info("Creando nuevo usuario: {}", user.getName());
        return restTemplate.postForObject(usersApiUrl, user, User.class);
    }
    
    public User updateUser(String id, User user) {
        log.info("Actualizando usuario con ID: {}", id);
        restTemplate.put(usersApiUrl + "/{id}", user, id);
        return getUserById(id).orElse(null);
    }
    
    public boolean deleteUser(String id) {
        log.info("Eliminando usuario con ID: {}", id);
        try {
            restTemplate.delete(usersApiUrl + "/{id}", id);
            return true;
        } catch (Exception e) {
            log.error("Error al eliminar usuario con ID: {}", id, e);
            return false;
        }
    }
    
    public boolean existsByPhone(String phone) {
        log.info("Verificando si existe usuario con teléfono: {}", phone);
        try {
            Boolean exists = restTemplate.getForObject(usersApiUrl + "/exists/phone/{phone}", Boolean.class, phone);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Error al verificar existencia de usuario con teléfono: {}", phone, e);
            return false;
        }
    }
    
    public boolean existsByEmail(String email) {
        log.info("Verificando si existe usuario con email: {}", email);
        try {
            Boolean exists = restTemplate.getForObject(usersApiUrl + "/exists/email/{email}", Boolean.class, email);
            return exists != null && exists;
        } catch (Exception e) {
            log.error("Error al verificar existencia de usuario con email: {}", email, e);
            return false;
        }
    }
}
