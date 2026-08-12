package com.heimdall.backend.service;

import com.heimdall.backend.entity.Role;
import com.heimdall.backend.entity.User;
import com.heimdall.backend.repository.UserRepository;
import com.heimdall.backend.security.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final String adminEmail;
    private final String allowedDomains;

    public CustomOidcUserService(
            UserRepository userRepository,
            @Value("${heimdall.security.admin-email}") String adminEmail,
            @Value("${heimdall.security.allowed-domains}") String allowedDomains) {
        this.userRepository = userRepository;
        this.adminEmail = adminEmail;
        this.allowedDomains = allowedDomains;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        if (email == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error("email_not_found"), "Email not found from OAuth2 provider");
        }

        // Domain restriction check
        if (allowedDomains != null && !allowedDomains.trim().isEmpty()) {
            String domain = email.substring(email.indexOf("@") + 1);
            List<String> domains = Arrays.asList(allowedDomains.split(","));
            if (!domains.contains(domain)) {
                throw new OAuth2AuthenticationException(new OAuth2Error("unauthorized_domain"), "Domain not allowed: " + domain);
            }
        }

        String name = oidcUser.getFullName();
        if (name == null) {
            name = oidcUser.getAttribute("name");
        }
        String avatarUrl = oidcUser.getPicture();
        if (avatarUrl == null) {
            avatarUrl = oidcUser.getAttribute("picture");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            
            // Assign ADMIN role if it matches the configured admin email, else VIEWER
            if (email.equalsIgnoreCase(adminEmail)) {
                newUser.setRole(Role.ADMIN);
            } else {
                newUser.setRole(Role.VIEWER);
            }
            return newUser;
        });

        // Update name and avatar in case they changed
        user.setName(name);
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);

        return oidcUser;
    }
}
