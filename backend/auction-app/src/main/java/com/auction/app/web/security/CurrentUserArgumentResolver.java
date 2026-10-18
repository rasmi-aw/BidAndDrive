package com.auction.app.web.security;

import com.auction.application.command.ProvisionUserCommand;
import com.auction.application.command.ProvisionUserUseCase;
import com.auction.domain.shared.DomainValidationException;
import com.auction.domain.user.Email;
import com.auction.domain.user.Phone;
import com.auction.domain.user.Role;
import com.auction.domain.user.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Resolves {@code @CurrentUser UserId}: subject = user id, profile from the OIDC claims
 * (name, email, phone_number) and exactly one auction role. The user is registered or updated locally.
 */
@Component
@RequiredArgsConstructor
class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final ProvisionUserUseCase provisionUser;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class) && parameter.getParameterType() == UserId.class;
    }

    @Override
    public UserId resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        if (!(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token)) {
            throw new IllegalStateException("@CurrentUser used on an endpoint that is not JWT-authenticated");
        }
        Jwt jwt = token.getToken();
        try {
            return provisionUser.handle(new ProvisionUserCommand(
                    new UserId(UUID.fromString(jwt.getSubject())),
                    fullName(jwt),
                    new Email(required(jwt, "email")),
                    new Phone(required(jwt, "phone_number")),
                    role(token)));
        } catch (DomainValidationException | IllegalArgumentException e) {
            throw new IncompleteIdentityException("token claims are not usable: " + e.getMessage());
        }
    }

    private static String fullName(Jwt jwt) {
        String name = jwt.getClaimAsString("name");
        return name == null || name.isBlank() ? required(jwt, "preferred_username") : name;
    }

    private static String required(Jwt jwt, String claim) {
        String value = jwt.getClaimAsString(claim);
        if (value == null || value.isBlank()) {
            throw new IncompleteIdentityException("token has no '" + claim + "' claim");
        }
        return value;
    }

    private static Role role(JwtAuthenticationToken token) {
        List<String> authorities = token.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        List<Role> roles = Arrays.stream(Role.values())
                .filter(role -> authorities.contains("ROLE_" + role.name()))
                .toList();
        if (roles.size() != 1) {
            throw new IncompleteIdentityException(
                    "token must carry exactly one of the realm roles " + Arrays.toString(Role.values()));
        }
        return roles.getFirst();
    }
}
