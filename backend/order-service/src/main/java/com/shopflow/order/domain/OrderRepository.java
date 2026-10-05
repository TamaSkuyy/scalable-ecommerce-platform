package com.shopflow.order.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * {@code left join fetch} sekaligus menghindari N+1 saat menampilkan detail pesanan.
     * Karena items adalah koleksi, {@code distinct} wajib dipakai.
     */
    @Query("select distinct o from Order o left join fetch o.items where o.orderNumber = :orderNumber")
    Optional<Order> findDetailByOrderNumber(@Param("orderNumber") String orderNumber);

    Optional<Order> findByOrderNumber(String orderNumber);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
