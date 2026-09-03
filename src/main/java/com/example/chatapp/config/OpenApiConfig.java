package com.example.chatapp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Real-Time Collaborative Communication Platform API")
                        .version("1.0.0")
                        .description("""
                                Backend-only REST APIs and WebSocket/STOMP specifications for Real-Time Collaborative Chat.
                                
                                ### WebSocket / STOMP Destinations:
                                - **Handshake Endpoint**: `/ws` (supports standard WS and SockJS)
                                - **Application Prefix**: `/app`
                                - **Broker Prefix**: `/topic`
                                
                                #### Client Send Destinations:
                                - Send Message: `/app/rooms/{roomId}/send` (Body: `WebSocketMessageRequest`)
                                - Start Typing: `/app/rooms/{roomId}/typing` (Body: `WebSocketTypingRequest`)
                                - Stop Typing: `/app/rooms/{roomId}/stop-typing` (Body: `WebSocketTypingRequest`)
                                
                                #### Client Topic Subscriptions:
                                - Room Messages: `/topic/rooms/{roomId}/messages`
                                - Room Presence: `/topic/rooms/{roomId}/presence`
                                - Room Events: `/topic/rooms/{roomId}/events`
                                - Global Presence: `/topic/presence`
                                """)
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://spring.io/")))
                .tags(List.of(
                        new Tag().name("Users").description("Operations related to user management"),
                        new Tag().name("Chat Rooms").description("Operations for creating, managing, joining and leaving chat rooms"),
                        new Tag().name("Messages").description("Operations for querying, updating, and soft-deleting chat messages")
                ));
    }
}
