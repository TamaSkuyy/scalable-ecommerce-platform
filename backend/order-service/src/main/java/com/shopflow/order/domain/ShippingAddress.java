package com.shopflow.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Alamat pengiriman sebagai <b>value object</b> ({@code @Embeddable}).
 *
 * <p>Kenapa tidak tabel terpisah? Karena alamat di sini tidak punya identitas sendiri -
 * ia selalu milik satu pesanan, dan tidak pernah di-query lintas pesanan. Ini juga
 * membuat riwayat pesanan tetap utuh walau user mengubah alamat defaultnya nanti.
 */
@Embeddable
public class ShippingAddress {

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Column(name = "line1", nullable = false, length = 200)
    private String line1;

    @Column(name = "line2", length = 200)
    private String line2;

    @Column(name = "city", nullable = false, length = 120)
    private String city;

    @Column(name = "postal_code", nullable = false, length = 16)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 64)
    private String country;

    protected ShippingAddress() {
        // untuk JPA
    }

    public ShippingAddress(String recipientName, String phone, String line1, String line2,
                           String city, String postalCode, String country) {
        this.recipientName = recipientName;
        this.phone = phone;
        this.line1 = line1;
        this.line2 = line2;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public String getPhone() {
        return phone;
    }

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public String getCountry() {
        return country;
    }
}
