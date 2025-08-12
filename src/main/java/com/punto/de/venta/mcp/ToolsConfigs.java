package com.punto.de.venta.mcp;

import com.punto.de.venta.mcp.tools.*;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class ToolsConfigs {

    @Bean
    public List<ToolCallback> findTools(
            EstadoCuentaTools estadoCuentaTools,
            UserTools userTools,
            TransactionTools transactionTools,
            RecurringTransactionTools recurringTransactionTools,
            LoanTools loanTools,
            BudgetTools budgetTools,
            SavingGoalTools savingGoalTools,
            AlertTools alertTools) {
        return List.of(
            ToolCallbacks.from(estadoCuentaTools)[0],
            ToolCallbacks.from(userTools)[0],
            ToolCallbacks.from(transactionTools)[0],
            ToolCallbacks.from(recurringTransactionTools)[0],
            ToolCallbacks.from(loanTools)[0],
            ToolCallbacks.from(budgetTools)[0],
            ToolCallbacks.from(savingGoalTools)[0],
            ToolCallbacks.from(alertTools)[0]
        );
    }
}
