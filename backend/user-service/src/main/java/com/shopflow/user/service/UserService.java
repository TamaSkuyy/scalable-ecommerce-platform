package com.shopflow.user.service;

import com.shopflow.common.api.ConflictException;
import com.shopflow.common.api.ForbiddenException;
import com.shopflow.common.api.ResourceNotFoundException;
import com.shopflow.common.api.UnauthorizedException;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.common.security.SecurityHeaders;
import com.shopflow.user.api.dto.AuthResponse;
import com.shopflow.user.api.dto.LoginRequest;
import com.shopflow.user.api.dto.PublicUserResponse;
import com.shopflow.user.api.dto.RegisterRequest;
import com.shopflow.user.api.dto.UpdateProfileRequest;
import com.shopflow.user.api.dto.UserResponse;
import com.shopflow.user.domain.UserAccount;
import com.shopflow.user.domain.UserAccountRepository;
import com.shopflow.user.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UserService(UserAccountRepository repository,
                       PasswordEncoder passwordEncoder,
                       TokenService tokenService) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        // Catatan: ada race condition kecil di sini (dua request bersamaan).
        // Unique constraint di database adalah jaring pengaman terakhirnya.
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Email " + email + " sudah terdaftar");
        }

        UserAccount account = new UserAccount(
                email,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                SecurityHeaders.ROLE_CUSTOMER);

        UserAccount saved = repository.save(account);
        log.info("User baru terdaftar: id={} email={}", saved.getId(), saved.getEmail());
        return UserResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        UserAccount account = repository.findByEmailIgnoreCase(email)
                // Pesan sengaja sama untuk "email tidak ada" dan "password salah",
                // supaya penyerang tidak bisa memakai endpoint ini untuk memetakan email terdaftar.
                .orElseThrow(() -> new UnauthorizedException("Email atau password salah"));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new UnauthorizedException("Email atau password salah");
        }
        if (!account.isEnabled()) {
            throw new ForbiddenException("Akun ini sedang dinonaktifkan");
        }

        TokenService.IssuedToken token = tokenService.issue(account);
        return AuthResponse.of(token.value(), token.expiresInSeconds(), UserResponse.from(account));
    }

    @Transactional(readOnly = true)
    public UserResponse currentProfile(CurrentUser currentUser) {
        return UserResponse.from(requireAccount(currentUser));
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id)));
    }

    @Transactional(readOnly = true)
    public PublicUserResponse findPublicProfile(Long id) {
        UserAccount account = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
        return new PublicUserResponse(account.getId(), account.getFullName());
    }

    @Transactional
    public UserResponse updateProfile(CurrentUser currentUser, UpdateProfileRequest request) {
        UserAccount account = requireAccount(currentUser);
        account.setFullName(request.fullName().trim());
        return UserResponse.from(repository.save(account));
    }

    private UserAccount requireAccount(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAuthenticated()) {
            throw new UnauthorizedException("Kamu harus login untuk mengakses resource ini");
        }
        return repository.findById(currentUser.id())
                .orElseThrow(() -> new UnauthorizedException("Sesi tidak valid, silakan login ulang"));
    }

    private String normalizeEmail(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }
}
