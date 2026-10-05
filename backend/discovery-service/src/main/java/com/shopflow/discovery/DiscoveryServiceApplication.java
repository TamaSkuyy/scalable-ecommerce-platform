package com.shopflow.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Service Registry.
 *
 * <p>Masalah yang diselesaikan: kalau user-service punya 3 replika dengan IP/port yang
 * berubah-ubah, bagaimana api-gateway tahu harus memanggil yang mana? Jawabannya:
 * setiap service mendaftarkan diri ke Eureka ("saya user-service, saya di 10.0.1.7:8081,
 * saya sehat"), dan pemanggil cukup memakai nama logis `http://user-service`.
 *
 * <p>Jalankan: {@code mvn -pl discovery-service spring-boot:run}
 * lalu buka http://localhost:8761
 */
@SpringBootApplication
@EnableEurekaServer
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }
}
