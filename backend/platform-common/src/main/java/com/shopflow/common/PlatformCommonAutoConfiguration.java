package com.shopflow.common;

import com.shopflow.common.api.ApiExceptionHandler;
import com.shopflow.common.security.CurrentUserArgumentResolver;
import com.shopflow.common.security.InternalApiTokenFilter;
import com.shopflow.common.security.JwtProperties;
import com.shopflow.common.web.CorrelationIdFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Auto-configuration platform-common.
 *
 * <p>Karena file ini terdaftar di
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports},
 * setiap service yang menaruh platform-common di classpath otomatis mendapat:
 * <ul>
 *   <li>{@link ApiExceptionHandler} - format error seragam</li>
 *   <li>{@link CorrelationIdFilter} - correlation id untuk log</li>
 *   <li>{@link CurrentUserArgumentResolver} - parameter {@code CurrentUser} di controller</li>
 * </ul>
 *
 * <p>Semuanya dibatasi {@code @ConditionalOnWebApplication(SERVLET)} supaya api-gateway
 * (WebFlux/Netty) tidak ikut memuat bean servlet dan gagal start.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@EnableConfigurationProperties(JwtProperties.class)
public class PlatformCommonAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApiExceptionHandler apiExceptionHandler() {
        return new ApiExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "currentUserWebMvcConfigurer")
    public WebMvcConfigurer currentUserWebMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(new CurrentUserArgumentResolver());
            }
        };
    }

    /**
     * Melindungi {@code /api/v1/internal/**} dengan token bersama.
     * Hanya aktif kalau {@code shopflow.internal.token} diisi - service yang tidak punya
     * endpoint internal (mis. user-service) tidak ikut memasang filter ini.
     */
    @Bean
    @ConditionalOnProperty(prefix = "shopflow.internal", name = "token")
    public InternalApiTokenFilter internalApiTokenFilter(
            @Value("${shopflow.internal.token}") String internalToken) {
        return new InternalApiTokenFilter(internalToken);
    }
}
