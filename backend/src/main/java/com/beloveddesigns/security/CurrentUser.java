package com.beloveddesigns.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injecte l'entité {@link com.beloveddesigns.user.entity.User} du client connecté dans un paramètre
 * de contrôleur. Résolu par {@link CurrentUserArgumentResolver}.
 *
 * <p>Pas de {@code @AuthenticationPrincipal} ici : le principal posé par le filtre JWT est un
 * {@code UserDetails} de Spring Security, pas notre entité — Spring injectait donc {@code null}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
