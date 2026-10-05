package com.shopflow.notification.api;

import com.shopflow.common.api.UnauthorizedException;
import com.shopflow.common.security.CurrentUser;
import com.shopflow.notification.api.dto.NotificationResponse;
import com.shopflow.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notifikasi user")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/me")
    @Operation(summary = "Semua notifikasi user yang login (terbaru dulu)")
    public List<NotificationResponse> mine(CurrentUser currentUser) {
        return notificationService.listMine(requireUser(currentUser).id());
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Tandai notifikasi sudah dibaca")
    public void markRead(CurrentUser currentUser, @PathVariable Long id) {
        notificationService.markRead(id, requireUser(currentUser).id());
    }

    private CurrentUser requireUser(CurrentUser currentUser) {
        if (currentUser == null || !currentUser.isAuthenticated()) {
            throw new UnauthorizedException("Kamu harus login untuk melihat notifikasi");
        }
        return currentUser;
    }
}
