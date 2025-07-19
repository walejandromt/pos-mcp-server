package com.punto.de.venta.mcp;

import com.punto.de.venta.mcp.tools.EstadoCuentaTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolsConfigs {

    @Bean
    public List<ToolCallback> findTools(EstadoCuentaTools estadoCuentaTools) {
        return List.of(ToolCallbacks.from(estadoCuentaTools));
    }
}
