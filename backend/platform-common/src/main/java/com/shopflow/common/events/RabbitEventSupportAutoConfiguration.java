package com.shopflow.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Konfigurasi RabbitMQ yang dipakai bersama.
 *
 * <p>Aktif hanya di service yang memakai Spring AMQP ({@code Jackson2JsonMessageConverter}
 * ada di classpath), jadi user-service / api-gateway / product-service tidak terpengaruh.
 */
@AutoConfiguration
@ConditionalOnClass(Jackson2JsonMessageConverter.class)
public class RabbitEventSupportAutoConfiguration {

    /**
     * Paket yang boleh dideserialisasi dari pesan broker.
     *
     * <p><b>Ini bukan formalitas.</b> {@code Jackson2JsonMessageConverter} mengirim header
     * {@code __TypeId__} berisi nama kelas asal, lalu memakai header itu untuk memilih kelas
     * tujuan saat menerima. Kalau aplikasi menerima kelas apa pun, siapa pun yang bisa
     * menulis ke broker (atau menyusup lewat satu konsumen) dapat memaksa aplikasi membuat
     * instance kelas sembarang — ini kelas kerentanan <i>deserialization gadget chain</i>.
     *
     * <p>Karena itu Spring AMQP menolak semua kelas di luar daftar ini dengan pesan:
     * <pre>
     *   The class 'com.shopflow.common.events.X' is not in the trusted packages: [java.util, java.lang]
     * </pre>
     * Solusinya adalah menuliskan paket kita secara eksplisit — <b>bukan</b> memakai
     * {@code "*"} yang mematikan seluruh proteksi.
     */
    public static final String[] TRUSTED_PACKAGES = {
            "com.shopflow.common.events",
            "java.util",
            "java.lang"
    };

    @Bean
    @ConditionalOnMissingBean(MessageConverter.class)
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages(TRUSTED_PACKAGES);
        converter.setJavaTypeMapper(typeMapper);

        return converter;
    }
}
