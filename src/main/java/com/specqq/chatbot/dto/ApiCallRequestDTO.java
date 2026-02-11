package com.specqq.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

/**
 * API Call Request DTO
 *
 * <p>Represents a JSON-RPC 2.0 request for calling NapCat API endpoints via WebSocket.
 * Used for extended API integration beyond basic message sending.</p>
 *
 * <p>JSON-RPC 2.0 Format:
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "uuid-string",
 *   "action": "get_group_info",
 *   "params": {"group_id": "123456"}
 * }
 * </pre>
 * </p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public class ApiCallRequestDTO {

    /**
     * JSON-RPC version (always "2.0")
     */
    @JsonProperty("jsonrpc")
    @NotBlank(message = "JSON-RPC version is required")
    private String jsonrpc = "2.0";

    /**
     * Request ID for correlation (UUID string)
     * Used to match responses to requests
     */
    @JsonProperty("id")
    @NotBlank(message = "Request ID is required")
    private String id;

    /**
     * API action name (e.g., "get_group_info", "get_group_member_info", "delete_msg")
     */
    @JsonProperty("action")
    @NotBlank(message = "Action is required")
    private String action;

    /**
     * API parameters (key-value map)
     * Parameters vary by action
     */
    @JsonProperty("params")
    @NotNull(message = "Params cannot be null (use empty map if no params)")
    private Map<String, Object> params;

    /**
     * Default constructor
     */
    public ApiCallRequestDTO() {
        this.id = UUID.randomUUID().toString();
    }

    /**
     * Constructor with action and params
     *
     * @param action API action name
     * @param params API parameters
     */
    public ApiCallRequestDTO(String action, Map<String, Object> params) {
        this();
        this.action = action;
        this.params = params;
    }

    /**
     * Constructor with all fields
     *
     * @param id     Request ID
     * @param action API action name
     * @param params API parameters
     */
    public ApiCallRequestDTO(String id, String action, Map<String, Object> params) {
        this.jsonrpc = "2.0";
        this.id = id;
        this.action = action;
        this.params = params;
    }

    // Getters and Setters

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        return String.format("ApiCallRequestDTO{jsonrpc='%s', id='%s', action='%s', params=%s}",
                jsonrpc, id, action, params);
    }
}
