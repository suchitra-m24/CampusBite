package com.campusbite.user;

import com.campusbite.common.security.SecurityUser;
import com.campusbite.trust.TrustScoreDto;
import com.campusbite.trust.TrustScoreService;
import com.campusbite.auth.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RestController
public class UserController {

    private final UserRepository userRepository;
    private final TrustScoreService trustScoreService;
    private final AuthService authService;

    public UserController(UserRepository userRepository, TrustScoreService trustScoreService, AuthService authService) {
        this.userRepository = userRepository;
        this.trustScoreService = trustScoreService;
        this.authService = authService;
    }

    @GetMapping("/api/me")
    public UserDto me(@AuthenticationPrincipal SecurityUser principal) {
        return UserDto.from(principal.getUser());
    }

    @GetMapping("/api/student/trust-score")
    public TrustScoreDto myTrustScore(@AuthenticationPrincipal SecurityUser principal) {
        return TrustScoreDto.from(trustScoreService.getFor(principal.getUser()));
    }

    @GetMapping("/api/admin/users")
    public List<UserDto> allUsers() {
        return userRepository.findAll().stream().map(UserDto::from).toList();
    }

    @Transactional
    @PatchMapping("/api/admin/users/{userId}/deactivate")
    public UserDto deactivate(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.campusbite.common.exception.ResourceNotFoundException("User not found"));
        user.setActive(false);
        authService.revokeAllForUser(user);
        return UserDto.from(userRepository.save(user));
    }

    @Transactional
    @PatchMapping("/api/admin/users/{userId}/activate")
    public UserDto activate(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new com.campusbite.common.exception.ResourceNotFoundException("User not found"));
        user.setActive(true);
        return UserDto.from(userRepository.save(user));
    }
}
