package com.shopflow.payment.domain;

public enum PaymentStatus {

    /** Sudah tercatat, belum ada hasil dari payment gateway. */
    PENDING,
    SUCCEEDED,
    FAILED,
    REFUNDED
}
