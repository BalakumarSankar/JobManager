package com.example.jobdispatcher.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.jobdispatcher.websocket.JobStatusWebSocketHandler;

/**
 * WebSocket configuration for real-time job status updates.
 * This is OPTIONAL - HTTPS is the primary communication method.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private JobStatusWebSocketHandler jobStatusWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // WebSocket endpoint for job status updates
        registry.addHandler(jobStatusWebSocketHandler, "/ws/job-updates")
                .setAllowedOrigins("*"); // Configure appropriately for production
    }
}

