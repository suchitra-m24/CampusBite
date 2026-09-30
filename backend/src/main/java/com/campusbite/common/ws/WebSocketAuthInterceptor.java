package com.campusbite.common.ws;

import com.campusbite.common.security.JwtService;
import com.campusbite.common.security.SecurityUser;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/** Authenticates STOMP CONNECT frames with the same short-lived JWT used by REST APIs
 * and authorizes subscriptions to private/staff order topics. */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public WebSocketAuthInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            throw new MessagingException("Invalid STOMP message");
        }
        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            Authentication authentication = (Authentication) accessor.getUser();
            if (authentication == null || !authentication.isAuthenticated()) {
                throw new MessagingException("Authentication required");
            }
            authorizeSubscription(authentication, accessor.getDestination());
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new MessagingException("Authentication required");
        }

        String token = header.substring(7).trim();
        if (token.isEmpty() || !jwtService.isTokenValid(token)) {
            throw new MessagingException("Invalid or expired access token");
        }

        String loginId;
        SecurityUser user;
        try {
            loginId = jwtService.extractLoginId(token);
            user = (SecurityUser) userDetailsService.loadUserByUsername(loginId);
        } catch (Exception ex) {
            throw new MessagingException("Invalid access token");
        }

        if (!user.isEnabled()) {
            throw new MessagingException("Account is disabled");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        accessor.setUser(authentication);
    }

    private void authorizeSubscription(Authentication authentication, String destination) {
        if (destination == null || !destination.startsWith("/topic/")) {
            throw new MessagingException("Subscription destination is not allowed");
        }

        SecurityUser user = (SecurityUser) authentication.getPrincipal();
        String role = user.getUser().getRole().name();

        if ("/topic/menu".equals(destination) || "/topic/kitchen-load".equals(destination)) {
            return;
        }

        if ("/topic/orders/staff".equals(destination)) {
            if ("STAFF".equals(role) || "ADMIN".equals(role)) return;
            throw new MessagingException("Staff order stream is restricted");
        }

        String prefix = "/topic/orders/user/";
        if (destination.startsWith(prefix)) {
            String requestedUserId = destination.substring(prefix.length());
            boolean ownTopic = String.valueOf(user.getId()).equals(requestedUserId);
            if (ownTopic || "STAFF".equals(role) || "ADMIN".equals(role)) return;
            throw new MessagingException("Order stream is restricted to the order owner");
        }

        throw new MessagingException("Subscription destination is not allowed");
    }
}
