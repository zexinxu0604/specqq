package com.specqq.chatbot.handler;

import com.specqq.chatbot.dto.MessageReceiveDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Calculator Handler
 *
 * <p>T078: Evaluates mathematical expressions</p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
@Slf4j
@Component
@HandlerMetadata(
        handlerType = "CALCULATOR",
        name = "Calculator",
        description = "Evaluates mathematical expressions and returns results",
        category = "Utilities",
        params = {
                @HandlerParam(
                        name = "precision",
                        displayName = "Decimal Precision",
                        type = "number",
                        required = false,
                        defaultValue = "2",
                        description = "Number of decimal places in result (0-10)"
                ),
                @HandlerParam(
                        name = "showSteps",
                        displayName = "Show Calculation Steps",
                        type = "boolean",
                        required = false,
                        defaultValue = "false",
                        description = "Whether to show intermediate calculation steps"
                )
        }
)
public class CalculatorHandler extends BaseHandler {

    private static final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
    private static final int MAX_PRECISION = 10;
    private static final int MIN_PRECISION = 0;

    @Override
    protected String process(MessageReceiveDTO message, Object params) {
        // Extract parameters
        int precision = 2;
        boolean showSteps = false;

        if (params instanceof Map) {
            Map<String, Object> paramMap = (Map<String, Object>) params;

            // Extract precision
            Object precisionObj = paramMap.get("precision");
            if (precisionObj instanceof Number) {
                precision = ((Number) precisionObj).intValue();
            } else if (precisionObj instanceof String) {
                try {
                    precision = Integer.parseInt((String) precisionObj);
                } catch (NumberFormatException e) {
                    log.warn("Invalid precision value: {}, using default: 2", precisionObj);
                }
            }

            // Validate precision range
            if (precision < MIN_PRECISION) {
                precision = MIN_PRECISION;
            } else if (precision > MAX_PRECISION) {
                precision = MAX_PRECISION;
            }

            // Extract showSteps
            Object showStepsObj = paramMap.get("showSteps");
            if (showStepsObj instanceof Boolean) {
                showSteps = (Boolean) showStepsObj;
            } else if (showStepsObj instanceof String) {
                showSteps = Boolean.parseBoolean((String) showStepsObj);
            }
        }

        String expression = message.getMessageContent();

        // Validate input
        if (expression == null || expression.trim().isEmpty()) {
            throw new IllegalArgumentException("Expression cannot be empty");
        }

        // Clean and validate expression
        String cleanedExpression = cleanExpression(expression);

        log.info("Evaluating expression: original={}, cleaned={}, precision={}",
                expression, cleanedExpression, precision);

        try {
            // Evaluate expression
            double result = evaluateExpression(cleanedExpression);

            // Format result with specified precision
            String formattedResult = formatResult(result, precision);

            // Build response
            StringBuilder response = new StringBuilder();
            response.append("计算结果:\n");
            response.append(cleanedExpression).append(" = ").append(formattedResult);

            if (showSteps) {
                response.append("\n\n计算步骤:\n");
                response.append(generateSteps(cleanedExpression, result));
            }

            return response.toString();

        } catch (ScriptException e) {
            log.error("Failed to evaluate expression: {}", cleanedExpression, e);
            throw new IllegalArgumentException("Invalid mathematical expression: " + e.getMessage());
        } catch (Exception e) {
            log.error("Calculation error", e);
            throw new RuntimeException("Calculation failed: " + e.getMessage());
        }
    }

    /**
     * Clean and validate mathematical expression
     *
     * @param expression Raw expression
     * @return Cleaned expression
     */
    private String cleanExpression(String expression) {
        // Remove whitespace
        String cleaned = expression.replaceAll("\\s+", "");

        // Replace common Unicode math symbols with ASCII equivalents
        cleaned = cleaned.replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("（", "(")
                .replace("）", ")");

        // Validate allowed characters (numbers, operators, parentheses, decimal point)
        if (!cleaned.matches("[0-9+\\-*/().]+")) {
            throw new IllegalArgumentException("Expression contains invalid characters. Only numbers and operators (+, -, *, /, parentheses) are allowed.");
        }

        // Check for balanced parentheses
        int openCount = 0;
        for (char c : cleaned.toCharArray()) {
            if (c == '(') openCount++;
            if (c == ')') openCount--;
            if (openCount < 0) {
                throw new IllegalArgumentException("Unbalanced parentheses in expression");
            }
        }
        if (openCount != 0) {
            throw new IllegalArgumentException("Unbalanced parentheses in expression");
        }

        return cleaned;
    }

    /**
     * Evaluate mathematical expression using JavaScript engine
     *
     * @param expression Mathematical expression
     * @return Evaluation result
     * @throws ScriptException if expression is invalid
     */
    private double evaluateExpression(String expression) throws ScriptException {
        ScriptEngine engine = scriptEngineManager.getEngineByName("JavaScript");

        if (engine == null) {
            throw new RuntimeException("JavaScript engine not available");
        }

        // Evaluate expression
        Object result = engine.eval(expression);

        // Convert result to double
        if (result instanceof Number) {
            double value = ((Number) result).doubleValue();

            // Check for special values
            if (Double.isNaN(value)) {
                throw new IllegalArgumentException("Result is not a number (NaN)");
            }
            if (Double.isInfinite(value)) {
                throw new IllegalArgumentException("Result is infinite (division by zero?)");
            }

            return value;
        }

        throw new IllegalArgumentException("Expression did not evaluate to a number");
    }

    /**
     * Format result with specified precision
     *
     * @param result    Calculation result
     * @param precision Number of decimal places
     * @return Formatted result string
     */
    private String formatResult(double result, int precision) {
        BigDecimal bd = BigDecimal.valueOf(result);
        bd = bd.setScale(precision, RoundingMode.HALF_UP);

        // Remove trailing zeros for cleaner display
        String formatted = bd.stripTrailingZeros().toPlainString();

        // Handle case where stripping zeros removes decimal point (e.g., "10.00" -> "10")
        if (formatted.equals("-0")) {
            formatted = "0";
        }

        return formatted;
    }

    /**
     * Generate calculation steps (simplified)
     *
     * @param expression Original expression
     * @param result     Final result
     * @return Steps description
     */
    private String generateSteps(String expression, double result) {
        // This is a simplified implementation
        // A full implementation would parse the expression tree and show each step

        StringBuilder steps = new StringBuilder();
        steps.append("1. 原始表达式: ").append(expression).append("\n");

        // Identify operations
        boolean hasParentheses = expression.contains("(");
        boolean hasMultDiv = expression.contains("*") || expression.contains("/");
        boolean hasAddSub = expression.contains("+") || expression.contains("-");

        if (hasParentheses) {
            steps.append("2. 先计算括号内的表达式\n");
        }
        if (hasMultDiv) {
            steps.append(hasParentheses ? "3" : "2").append(". 执行乘除运算\n");
        }
        if (hasAddSub) {
            int stepNum = (hasParentheses ? 2 : 1) + (hasMultDiv ? 1 : 0) + 1;
            steps.append(stepNum).append(". 执行加减运算\n");
        }

        steps.append("最终结果: ").append(result);

        return steps.toString();
    }

    /**
     * Example usage:
     *
     * <pre>
     * Input: "2 + 3 * 4"
     * Output: "2 + 3 * 4 = 14"
     *
     * Input: "(10 + 5) * 2 - 8"
     * Output: "(10 + 5) * 2 - 8 = 22"
     *
     * Input: "100 / 3" (with precision=4)
     * Output: "100 / 3 = 33.3333"
     * </pre>
     */
}
