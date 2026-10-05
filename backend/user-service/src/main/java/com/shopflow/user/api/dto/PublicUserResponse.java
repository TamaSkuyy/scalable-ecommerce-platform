package com.shopflow.user.api.dto;

/** Info publik user - sengaja hanya nama. Jangan pernah membocorkan email ke endpoint publik. */
public record PublicUserResponse(Long id, String fullName) {
}
