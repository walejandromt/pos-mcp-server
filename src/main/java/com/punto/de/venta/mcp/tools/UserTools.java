package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.model.User;
import com.punto.de.venta.mcp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class UserTools {
    
    private final UserService userService;
    
    public UserTools(UserService userService) {
        this.userService = userService;
    }
    
    @Tool(name = "getUserByPhone", description = "Retrieves user information based on their phone number. Useful for identifying the user before performing financial operations.")
    public String obtenerUsuarioPorTelefono(@ToolParam String numeroTelefono) {
        log.info("Obteniendo usuario por número de teléfono: {}", numeroTelefono);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            Optional<User> user = userService.getUserByPhone(numeroTelefono.trim());
            if (user.isPresent()) {
                User usuario = user.get();
                return String.format("Usuario encontrado - ID: %s, Nombre: %s, Teléfono: %s, Moneda: %s", 
                    usuario.getId(), usuario.getName(), usuario.getPhone(), usuario.getCurrency());
            } else {
                return "No se encontró ningún usuario con el número de teléfono: " + numeroTelefono;
            }
        } catch (Exception e) {
            log.error("Error al obtener usuario por teléfono: {}", numeroTelefono, e);
            return "Error al obtener la información del usuario: " + e.getMessage();
        }
    }
    
    @Tool(name = "checkUserExists", description = "Verifies if a user exists with the provided phone number. Returns true if exists, false otherwise.")
    public String verificarExistenciaUsuario(@ToolParam String numeroTelefono) {
        log.info("Verificando existencia de usuario con número de teléfono: {}", numeroTelefono);
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            boolean existe = userService.existsByPhone(numeroTelefono.trim());
            return existe ? "El usuario existe" : "El usuario no existe";
        } catch (Exception e) {
            log.error("Error al verificar existencia de usuario: {}", numeroTelefono, e);
            return "Error al verificar la existencia del usuario: " + e.getMessage();
        }
    }
    
    @Tool(name = "createNewUser", description = "Creates a new user account with the provided information. This tool should be used when a user is not registered in the system and needs to create a new account. Requires name and phone number, currency is optional (defaults to MXN).")
    public String crearUsuario(@ToolParam String nombre, @ToolParam String numeroTelefono, @ToolParam String moneda) {
        log.info("Creando nuevo usuario: {} con teléfono: {}", nombre, numeroTelefono);
        
        if (nombre == null || nombre.trim().isEmpty()) {
            return "Error: El nombre no puede estar vacío";
        }
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }
        
        try {
            // Verificar si ya existe un usuario con ese teléfono
            if (userService.existsByPhone(numeroTelefono.trim())) {
                return "Error: Ya existe un usuario con el número de teléfono: " + numeroTelefono;
            }
            
            User nuevoUsuario = new User();
            nuevoUsuario.setName(nombre.trim());
            nuevoUsuario.setPhone(numeroTelefono.trim());
            nuevoUsuario.setCurrency(moneda != null ? moneda.trim() : "MXN");
            
            User usuarioCreado = userService.createUser(nuevoUsuario);
            return String.format("Usuario creado exitosamente - ID: %s, Nombre: %s, Teléfono: %s, Moneda: %s", 
                usuarioCreado.getId(), usuarioCreado.getName(), usuarioCreado.getPhone(), usuarioCreado.getCurrency());
        } catch (Exception e) {
            log.error("Error al crear usuario: {}", nombre, e);
            return "Error al crear el usuario: " + e.getMessage();
        }
    }
}
