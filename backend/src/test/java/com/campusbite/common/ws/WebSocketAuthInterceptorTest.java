package com.campusbite.common.ws;

import com.campusbite.common.security.JwtService;
import com.campusbite.common.security.SecurityUser;
import com.campusbite.user.Role;
import com.campusbite.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WebSocketAuthInterceptorTest {

    private JwtService jwtService;
    private UserDetailsService userDetailsService;
    private WebSocketAuthInterceptor interceptor;
    private SecurityUser student;
    private SecurityUser staff;

    @BeforeEach
    void setUp() {
        jwtService = mock(JwtService.class);
        userDetailsService = mock(UserDetailsService.class);
        interceptor = new WebSocketAuthInterceptor(jwtService, userDetailsService);

        User studentUser = new User("Student", "student@example.com", "STU001", "hash", Role.STUDENT);
        studentUser.setId(10L);
        User staffUser = new User("Staff", "staff@example.com", "STAFF001", "hash", Role.STAFF);
        staffUser.setId(20L);
        student = new SecurityUser(studentUser);
        staff = new SecurityUser(staffUser);

        when(jwtService.isTokenValid("student-token")).thenReturn(true);
        when(jwtService.extractLoginId("student-token")).thenReturn("STU001");
        when(userDetailsService.loadUserByUsername("STU001")).thenReturn(student);
    }

    @Test
    void connectRequiresAndAuthenticatesJwt() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer student-token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Message<?> result = assertDoesNotThrow(() -> interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class)));
        StompHeaderAccessor authenticated = StompHeaderAccessor.wrap(result);
        assertDoesNotThrow(() -> authenticated.getUser().getName());
    }

    @Test
    void deactivatedUserCannotConnectEvenWithValidJwt() {
        student.getUser().setActive(false);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer student-token");
        accessor.setLeaveMutable(true);
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Exception exception = assertThrows(Exception.class, () -> interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class)));
        assertEquals("Account is disabled", exception.getCause() != null ? exception.getCause().getMessage() : exception.getMessage());
    }

    @Test
    void studentCannotSubscribeToStaffOrders() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                student, null, student.getAuthorities()));
        accessor.setDestination("/topic/orders/staff");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(Exception.class, () -> interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class)));
    }

    @Test
    void studentCannotSubscribeToAnotherUsersOrders() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                student, null, student.getAuthorities()));
        accessor.setDestination("/topic/orders/user/999");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThrows(Exception.class, () -> interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class)));
    }

    @Test
    void studentCanSubscribeToOwnOrderTopic() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setUser(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                student, null, student.getAuthorities()));
        accessor.setDestination("/topic/orders/user/10");
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertDoesNotThrow(() -> interceptor.preSend(message, mock(org.springframework.messaging.MessageChannel.class)));
    }
}
