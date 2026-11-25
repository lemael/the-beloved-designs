package com.beloveddesigns.security;

import com.beloveddesigns.except.ErrorMessages;
import com.beloveddesigns.except.ResourceNotFoundException;
import com.beloveddesigns.user.entity.User;
import com.beloveddesigns.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Résout {@code @CurrentUser User user} : retrouve l'entité à partir du numéro WhatsApp
 * (nom d'utilisateur du jeton JWT authentifié).
 */
@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public User resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Ne devrait pas arriver (routes protégées par SecurityConfig) : on refuse plutôt que d'injecter null.
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException(ErrorMessages.AUTHENTICATION_REQUIRED);
        }
        return userRepository.findByPhone(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.AUTHENTICATED_USER_NOT_FOUND));
    }
}
