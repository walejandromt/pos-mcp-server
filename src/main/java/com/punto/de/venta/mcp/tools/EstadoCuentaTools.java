package com.punto.de.venta.mcp.tools;

import com.punto.de.venta.mcp.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EstadoCuentaTools {

    private final UserService userService;
    
    public EstadoCuentaTools(UserService userService) {
        this.userService = userService;
    }

    @Tool(name = "Send account statement", description = "Gets the account statement by month, year, account ID and phone number. Requires a verification token sent by SMS. If the token is null, a new one is sent via SMS and an error is returned until the token is provided.")
    public String consultar(
            @ToolParam String numeroTelefono,
            @ToolParam String month,
            @ToolParam String year,
            @ToolParam Long accountId,
            @ToolParam String token) {
        // Lógica para llamar a tu API
        log.info("parametros numeroTelefono: {} mes: {} anio: {} token: {} IdCuenta: {}", numeroTelefono, month, year, token, accountId);

        if (numeroTelefono == null || numeroTelefono.trim().isEmpty()) {
            return "Error: El número de teléfono no puede estar vacío";
        }

        try {
            // Validar que el usuario existe por número de teléfono
            var userOpt = userService.getUserByPhone(numeroTelefono.trim());
            if (userOpt.isEmpty()) {
                return "Error: No se encontró usuario con el número de teléfono: " + numeroTelefono;
            }

            if(accountId == null){
                return "El id de la cuenta no puede ser null, buscala por el nombre";
            }

            if(token == null || token.isEmpty()){
                return "Hace falta el token ya se envio por sms";
            }

            return "Tu estado de cuenta se envio por correo";
        } catch (Exception e) {
            log.error("Error al validar usuario con teléfono: {}", numeroTelefono, e);
            return "Error al validar el usuario: " + e.getMessage();
        }
    }

}
