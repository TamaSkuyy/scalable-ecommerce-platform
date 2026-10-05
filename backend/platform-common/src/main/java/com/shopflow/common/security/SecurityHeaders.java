package com.shopflow.common.security;

/**
 * Nama header "internal" yang dipakai Gateway untuk meneruskan identitas ke service hilir.
 *
 * <p><b>PENTING (keamanan):</b> header ini hanya boleh datang dari Gateway. Karena itu di
 * docker-compose port service TIDAK dipublikasikan ke host, hanya Gateway yang boleh
 * diakses dari luar. Di produksi (Kubernetes) pakai NetworkPolicy + ClusterIP.
 *
 * <p>Alternatif yang lebih kuat (latihan lanjutan): setiap service memverifikasi JWT-nya
 * sendiri (defense in depth) atau memakai mTLS antar-service.
 */
public final class SecurityHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_EMAIL = "X-User-Email";
    public static final String USER_ROLES = "X-User-Roles";

    /**
     * Token bersama untuk endpoint {@code /api/v1/internal/**} yang hanya boleh dipanggil
     * service lain (mis. order-service memanggil product-service untuk menahan stok).
     *
     * <p>Bukan pengganti mTLS, tapi jauh lebih baik daripada "percaya saja karena
     * masih satu jaringan". Upgrade berikutnya: JWT service-to-service berumur pendek
     * (client credentials) atau service mesh dengan mTLS.
     */
    public static final String INTERNAL_TOKEN = "X-Internal-Token";

    public static final String AUTHORIZATION = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_CUSTOMER = "CUSTOMER";

    private SecurityHeaders() {
    }
}
