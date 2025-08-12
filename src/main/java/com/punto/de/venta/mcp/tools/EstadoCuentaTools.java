package com.punto.de.venta.mcp.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EstadoCuentaTools {

    @Tool(name = "Send account statement", description = "Gets the account statement by month, year, and account ID. Requires a verification token sent by SMS. If the token is null, a new one is sent via SMS and an error is returned until the token is provided.")
    public String consultar(
            @ToolParam String month,
            @ToolParam String year,
            @ToolParam Long accountId,
            @ToolParam String token) {
        // Lógica para llamar a tu API
        log.info("parametros mes: {} anio: {} token: {} IdCuenta: {}", month, year, token, accountId);

        if(accountId == null){
            return "El id de la cuenta no puede ser null, buscala por el nombre";
        }

        if(token == null || token.isEmpty()){
            return "Hace falta el token ya se envio por sms";
        }

        return "Tu estado de cuenta se envio por correo";
    }

}
