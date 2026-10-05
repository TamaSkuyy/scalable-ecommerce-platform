package com.shopflow.common.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Mengubah header internal menjadi parameter {@link CurrentUser} di controller.
 *
 * <p>Efeknya: kode bisnis tidak pernah menyentuh {@code HttpServletRequest} /
 * {@code SecurityContext} secara langsung -> mudah di-unit-test dan tidak terikat framework.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentUser.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            return CurrentUser.ANONYMOUS;
        }
        return CurrentUser.of(
                request.getHeader(SecurityHeaders.USER_ID),
                request.getHeader(SecurityHeaders.USER_EMAIL),
                request.getHeader(SecurityHeaders.USER_ROLES));
    }
}
