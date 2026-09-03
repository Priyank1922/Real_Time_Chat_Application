package com.example.chatapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Prefix for messages routed to message-handling methods (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");
        // Prefix for broker-backed topics
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        String[] origins = allowedOrigins.split(",");

        // Standard WebSocket endpoint
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins);

        // Fallback endpoint with SockJS
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins)
                .withSockJS();
    }
}
