package com.example.jobdispatcher.websocket;

import com.example.jobdispatcher.entity.ScheduledJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket handler for real-time job status updates.
 * This provides OPTIONAL real-time updates - HTTPS is still the primary method.
 */
@Component
public class JobStatusWebSocketHandler implements WebSocketHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(JobStatusWebSocketHandler.class);
    
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        
        logger.info("WebSocket connection established: {}", sessionId);
        
        // Send welcome message
        sendMessage(session, createMessage("CONNECTED", "WebSocket connection established", null));
    }
    
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload().toString();
        
        logger.debug("Received WebSocket message from {}: {}", sessionId, payload);
        
        try {
            // Parse client message (could be subscription requests, etc.)
            Map<String, Object> clientMessage = objectMapper.readValue(payload, Map.class);
            String type = (String) clientMessage.get("type");
            
            switch (type) {
                case "SUBSCRIBE_JOB":
                    String jobId = (String) clientMessage.get("jobId");
                    subscribeToJob(session, jobId);
                    break;
                case "UNSUBSCRIBE_JOB":
                    String unsubscribeJobId = (String) clientMessage.get("jobId");
                    unsubscribeFromJob(session, unsubscribeJobId);
                    break;
                case "PING":
                    sendMessage(session, createMessage("PONG", "Pong", null));
                    break;
                default:
                    logger.warn("Unknown WebSocket message type: {}", type);
            }
            
        } catch (Exception e) {
            logger.error("Error handling WebSocket message", e);
            sendMessage(session, createMessage("ERROR", "Invalid message format", null));
        }
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        logger.error("WebSocket transport error for session: {}", sessionId, exception);
        
        sessions.remove(sessionId);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        
        logger.info("WebSocket connection closed: {} (status: {})", sessionId, closeStatus);
    }
    
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
    
    /**
     * Broadcast job status update to all connected clients.
     */
    public void broadcastJobUpdate(ScheduledJob job) {
        if (sessions.isEmpty()) {
            return;
        }
        
        try {
            String message = createJobUpdateMessage(job);
            
            for (WebSocketSession session : sessions.values()) {
                if (session.isOpen()) {
                    sendMessage(session, message);
                }
            }
            
            logger.debug("Broadcasted job update for job: {} to {} clients", job.getJobId(), sessions.size());
            
        } catch (Exception e) {
            logger.error("Error broadcasting job update", e);
        }
    }
    
    /**
     * Send job update to specific client (if they're subscribed).
     */
    public void sendJobUpdateToClient(String clientId, ScheduledJob job) {
        // This would require tracking client subscriptions
        // For now, we'll broadcast to all clients
        broadcastJobUpdate(job);
    }
    
    /**
     * Subscribe client to job updates.
     */
    private void subscribeToJob(WebSocketSession session, String jobId) {
        // Store subscription in session attributes
        session.getAttributes().put("subscribedJobId", jobId);
        
        sendMessage(session, createMessage("SUBSCRIBED", 
            "Subscribed to job updates for: " + jobId, jobId));
        
        logger.info("Client {} subscribed to job: {}", session.getId(), jobId);
    }
    
    /**
     * Unsubscribe client from job updates.
     */
    private void unsubscribeFromJob(WebSocketSession session, String jobId) {
        session.getAttributes().remove("subscribedJobId");
        
        sendMessage(session, createMessage("UNSUBSCRIBED", 
            "Unsubscribed from job updates for: " + jobId, jobId));
        
        logger.info("Client {} unsubscribed from job: {}", session.getId(), jobId);
    }
    
    /**
     * Send message to WebSocket session.
     */
    private void sendMessage(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            }
        } catch (IOException e) {
            logger.error("Error sending WebSocket message", e);
        }
    }
    
    /**
     * Create a generic message.
     */
    private String createMessage(String type, String message, String jobId) {
        try {
            Map<String, Object> messageMap = Map.of(
                "type", type,
                "message", message,
                "timestamp", System.currentTimeMillis(),
                "jobId", jobId != null ? jobId : ""
            );
            return objectMapper.writeValueAsString(messageMap);
        } catch (Exception e) {
            logger.error("Error creating message", e);
            return "{\"error\":\"Message creation failed\"}";
        }
    }
    
    /**
     * Create job update message.
     */
    private String createJobUpdateMessage(ScheduledJob job) {
        try {
            Map<String, Object> messageMap = new HashMap<>();
            messageMap.put("type", "JOB_UPDATE");
            messageMap.put("jobId", job.getJobId());
            messageMap.put("jobName", job.getJobName());
            messageMap.put("status", job.getStatus());
            messageMap.put("jobType", job.getJobType());
            messageMap.put("submittedAt", job.getSubmittedAt());
            messageMap.put("startedAt", job.getStartedAt());
            messageMap.put("completedAt", job.getCompletedAt());
            messageMap.put("executionTimeMs", job.getExecutionTimeMs());
            messageMap.put("errorMessage", job.getErrorMessage() != null ? job.getErrorMessage() : "");
            messageMap.put("retryCount", job.getRetryCount());
            messageMap.put("timestamp", System.currentTimeMillis());
            return objectMapper.writeValueAsString(messageMap);
        } catch (Exception e) {
            logger.error("Error creating job update message", e);
            return "{\"error\":\"Job update message creation failed\"}";
        }
    }
    
    /**
     * Get number of active WebSocket connections.
     */
    public int getActiveConnectionCount() {
        return sessions.size();
    }
    
    /**
     * Get WebSocket statistics.
     */
    public Map<String, Object> getWebSocketStats() {
        return Map.of(
            "activeConnections", sessions.size(),
            "timestamp", System.currentTimeMillis()
        );
    }
}
