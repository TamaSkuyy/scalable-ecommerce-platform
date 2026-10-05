package com.shopflow.user.api;

import com.shopflow.common.security.CurrentUser;
import com.shopflow.user.api.dto.PublicUserResponse;
import com.shopflow.user.api.dto.UpdateProfileRequest;
import com.shopflow.user.api.dto.UserResponse;
import com.shopflow.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Perhatikan: parameter {@link CurrentUser} TIDAK dianotasi apa pun.
 * {@code CurrentUserArgumentResolver} (dari platform-common) yang mengisinya dari
 * header internal yang dikirim Gateway. Controller tetap bersih dari detail HTTP.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Profil pengguna")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Profil user yang sedang login")
    public UserResponse me(CurrentUser currentUser) {
        return userService.currentProfile(currentUser);
    }

    @PatchMapping("/me")
    @Operation(summary = "Ubah profil user yang sedang login")
    public UserResponse updateMe(CurrentUser currentUser, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUser, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Profil publik (hanya nama)")
    public PublicUserResponse publicProfile(@PathVariable Long id) {
        return userService.findPublicProfile(id);
    }
}
