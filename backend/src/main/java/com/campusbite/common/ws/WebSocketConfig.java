package com.campusbite.common.ws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/** Authenticated STOMP-over-WebSocket broker for live menu, kitchen-load and order updates. */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor authInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(WebSocketAuthInterceptor authInterceptor,
                           @Value("${campusbite.cors.allowed-origins}") String allowedOrigins) {
        this.authInterceptor = authInterceptor;
        this.allowedOrigins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toArray(String[]::new);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authInterceptor);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }
}
