package com.punto.de.venta.mcp;

import com.punto.de.venta.mcp.tools.*;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
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
            AlertTools alertTools,
            AnalyticsTools analyticsTools,
            OptimizationTools optimizationTools,
            CurrencyTools currencyTools) {
        
        List<ToolCallback> allTools = new ArrayList<>();
        
        allTools.addAll(Arrays.asList(ToolCallbacks.from(estadoCuentaTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(userTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(transactionTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(recurringTransactionTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(loanTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(budgetTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(savingGoalTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(alertTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(analyticsTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(optimizationTools)));
        allTools.addAll(Arrays.asList(ToolCallbacks.from(currencyTools)));
        
        return allTools;
    }
}
