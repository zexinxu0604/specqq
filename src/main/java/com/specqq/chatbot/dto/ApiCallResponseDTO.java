package com.specqq.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * API Call Response DTO
 *
 * <p>Represents a response from NapCat API call (WebSocket or HTTP).
 * Contains status, return code, data payload, and execution metadata.</p>
 *
 * <p>Response Format:
 * <pre>
 * {
 *   "status": "ok",
 *   "retcode": 0,
 *   "data": {...},
 *   "executionTimeMs": 25
 * }
 * </pre>
 * </p>
 *
 * @author Claude Code
 * @since 2026-02-11
 */
public class ApiCallResponseDTO {

    /**
     * Response status ("ok" or "failed")
     */
    @JsonProperty("status")
    private String status;

    /**
     * Return code (0 = success, non-zero = error)
     */
    @JsonProperty("retcode")
    private Integer retcode;

    /**
     * Response data payload (varies by API action)
     */
    @JsonProperty("data")
    private Object rawData;

    /**
     * Error message (if status = "failed")
     */
    @JsonProperty("message")
    private String message;

    /**
     * Execution time in milliseconds (for performance monitoring)
     */
    @JsonProperty("executionTimeMs")
    private Long executionTimeMs;

    /**
     * Request ID (for correlation with request)
     */
    @JsonProperty("id")
    private String id;

    /**
     * Echo field (NapCat uses this instead of id for WebSocket responses)
     */
    @JsonProperty("echo")
    private String echo;

    /**
     * Default constructor
     */
    public ApiCallResponseDTO() {
    }

    /**
     * Constructor for successful response
     *
     * @param data            Response data
     * @param executionTimeMs Execution time in milliseconds
     */
    public ApiCallResponseDTO(Map<String, Object> data, Long executionTimeMs) {
        this.status = "ok";
        this.retcode = 0;
        this.rawData = data;
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Constructor for error response
     *
     * @param retcode         Error return code
     * @param message         Error message
     * @param executionTimeMs Execution time in milliseconds
     */
    public ApiCallResponseDTO(Integer retcode, String message, Long executionTimeMs) {
        this.status = "failed";
        this.retcode = retcode;
        this.message = message;
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * Check if response is successful
     *
     * @return true if status is "ok" and retcode is 0, false otherwise
     */
    public boolean isSuccess() {
        return "ok".equals(status) && (retcode == null || retcode == 0);
    }

    /**
     * Check if response is an error
     *
     * @return true if status is "failed" or retcode is non-zero, false otherwise
     */
    public boolean isError() {
        return !isSuccess();
    }

    // Getters and Setters

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRetcode() {
        return retcode;
    }

    public void setRetcode(Integer retcode) {
        this.retcode = retcode;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getData() {
        if (rawData instanceof Map) {
            return (Map<String, Object>) rawData;
        }
        return null;
    }

    @JsonProperty("data")
    public void setData(Object data) {
        this.rawData = data;
    }

    @JsonIgnore
    public Object getRawData() {
        return rawData;
    }

    @JsonIgnore
    public void setRawData(Object rawData) {
        this.rawData = rawData;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public String getId() {
        // Return echo if id is null (NapCat uses echo field)
        return id != null ? id : echo;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEcho() {
        return echo;
    }

    public void setEcho(String echo) {
        this.echo = echo;
    }

    @Override
    public String toString() {
        return String.format("ApiCallResponseDTO{status='%s', retcode=%d, data=%s, message='%s', executionTimeMs=%d, id='%s'}",
                status, retcode, rawData, message, executionTimeMs, id);
    }
}
