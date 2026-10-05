package com.shopflow.product.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlugAndActiveTrue(String slug);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    /**
     * Pencarian katalog.
     *
     * <p>Trik {@code :q = ''} supaya tidak perlu menangani parameter NULL di SQL -
     * satu query melayani "tanpa filter" sekaligus "dengan filter".
     *
     * <p>{@code join fetch p.category} menghindari N+1 query: tanpa ini, menampilkan
     * 12 produk berarti 1 + 12 query tambahan.
     */
    @Query(value = """
            select p from Product p
            join fetch p.category c
            where p.active = true
              and (lower(p.name) like lower(concat('%', :q, '%'))
                   or lower(p.sku) like lower(concat('%', :q, '%')))
              and (:categorySlug = '' or c.slug = :categorySlug)
            """,
            countQuery = """
            select count(p) from Product p
            join p.category c
            where p.active = true
              and (lower(p.name) like lower(concat('%', :q, '%'))
                   or lower(p.sku) like lower(concat('%', :q, '%')))
              and (:categorySlug = '' or c.slug = :categorySlug)
            """)
    Page<Product> search(@Param("q") String q,
                         @Param("categorySlug") String categorySlug,
                         Pageable pageable);

    @Query("select p from Product p join fetch p.category where p.id = :id")
    Optional<Product> findDetailById(@Param("id") Long id);

    /**
     * Ambil baris dengan kunci tulis (SELECT ... FOR UPDATE).
     *
     * <p>Dipakai HANYA saat mengubah stok: kita mau menyerialkan perubahan stok produk
     * yang sama. Kuncinya di level baris, jadi produk lain tetap bisa diakses bersamaan.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
