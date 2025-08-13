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
    
    @Tool(name = "obtenerUsuarioPorTelefono", description = "Obtiene la información del usuario basándose en su número de teléfono. Útil para identificar al usuario antes de realizar operaciones financieras.")
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
    
    @Tool(name = "verificarExistenciaUsuario", description = "Verifica si existe un usuario con el número de teléfono proporcionado. Retorna true si existe, false en caso contrario.")
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
    
    @Tool(name = "obtenerUsuarioPorEmail", description = "Obtiene la información del usuario basándose en su correo electrónico. Útil para identificar al usuario antes de realizar operaciones financieras.")
    public String obtenerUsuarioPorEmail(@ToolParam String email) {
        log.info("Obteniendo usuario por email: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            return "Error: El email no puede estar vacío";
        }
        
        try {
            Optional<User> user = userService.getUserByEmail(email.trim());
            if (user.isPresent()) {
                User usuario = user.get();
                return String.format("Usuario encontrado - ID: %s, Nombre: %s, Email: %s, Teléfono: %s, Moneda: %s", 
                    usuario.getId(), usuario.getName(), usuario.getEmail(), usuario.getPhone(), usuario.getCurrency());
            } else {
                return "No se encontró ningún usuario con el email: " + email;
            }
        } catch (Exception e) {
            log.error("Error al obtener usuario por email: {}", email, e);
            return "Error al obtener la información del usuario: " + e.getMessage();
        }
    }
    
    @Tool(name = "verificarExistenciaUsuarioPorEmail", description = "Verifica si existe un usuario con el correo electrónico proporcionado. Retorna true si existe, false en caso contrario.")
    public String verificarExistenciaUsuarioPorEmail(@ToolParam String email) {
        log.info("Verificando existencia de usuario con email: {}", email);
        
        if (email == null || email.trim().isEmpty()) {
            return "Error: El email no puede estar vacío";
        }
        
        try {
            boolean existe = userService.existsByEmail(email.trim());
            return existe ? "El usuario existe" : "El usuario no existe";
        } catch (Exception e) {
            log.error("Error al verificar existencia de usuario por email: {}", email, e);
            return "Error al verificar la existencia del usuario: " + e.getMessage();
        }
    }
    
    @Tool(name = "crearUsuario", description = "Crea un nuevo usuario con la información proporcionada. Requiere nombre número de teléfono y correo electronico.")
    public String crearUsuario(@ToolParam String nombre, @ToolParam String numeroTelefono, @ToolParam String email, @ToolParam(required = false) String moneda) {
        log.info("Creando nuevo usuario: {} con teléfono: {}", nombre, numeroTelefono);
        
        if (nombre == null || nombre.trim().isEmpty()) {
            return "Error: El nombre no puede estar vacío";
        }
        
        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }

        if (email == null || email.trim().isEmpty()) {
            return "Error: El correo electronico no puede estar vacío";
        }
        
        try {
            // Verificar si ya existe un usuario con ese teléfono
            if (userService.existsByPhone(numeroTelefono.trim())) {
                return "Error: Ya existe un usuario con el número de teléfono: " + numeroTelefono;
            }
            
            // Verificar si ya existe un usuario con ese email
            if (userService.existsByEmail(email.trim())) {
                return "Error: Ya existe un usuario con el correo electrónico: " + email;
            }
            
            User nuevoUsuario = new User();
            nuevoUsuario.setName(nombre.trim());
            nuevoUsuario.setPhone(numeroTelefono.trim());
            nuevoUsuario.setEmail(email.trim());
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
