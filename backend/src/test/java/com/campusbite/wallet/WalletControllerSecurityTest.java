package com.campusbite.wallet;

import com.campusbite.common.security.SecurityUser;
import com.campusbite.user.Role;
import com.campusbite.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.campusbite.common.config.SecurityConfig;
import com.campusbite.common.security.CustomUserDetailsService;
import com.campusbite.common.security.JwtAuthenticationFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@Import(SecurityConfig.class)
class WalletControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @MockBean
    private WalletTransactionRepository transactionRepository;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UrlBasedCorsConfigurationSource corsConfigurationSource;

    @Test
    void teacherCannotAccessWalletEndpoint() throws Exception {
        User teacher = new User("Teacher", "teacher@example.com", "TCH001", "hash", Role.TEACHER);
        teacher.setId(20L);
        SecurityUser principal = new SecurityUser(teacher);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        mockMvc.perform(get("/api/student/wallet")
                        .with(authentication(authentication)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(walletService, transactionRepository);
    }

    @Test
    void studentCanAccessWalletEndpointWithApplicationPrincipal() throws Exception {
        User student = new User("Student", "student@example.com", "STU001", "hash", Role.STUDENT);
        student.setId(10L);
        SecurityUser principal = new SecurityUser(student);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());

        when(walletService.getOrCreate(any(User.class)))
                .thenReturn(new Wallet(student, new BigDecimal("125.00")));

        mockMvc.perform(get("/api/student/wallet")
                        .with(authentication(authentication)))
                .andExpect(status().isOk());

        verify(walletService).getOrCreate(student);
    }
}
