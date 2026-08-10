package com.heimdall.backend.service;

import com.heimdall.backend.entity.Role;
import com.heimdall.backend.entity.User;
import com.heimdall.backend.repository.UserRepository;
import com.heimdall.backend.security.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final String adminEmail;
    private final String allowedDomains;

    public CustomOAuth2UserService(
            UserRepository userRepository,
            @Value("${heimdall.security.admin-email}") String adminEmail,
            @Value("${heimdall.security.allowed-domains}") String allowedDomains) {
        this.userRepository = userRepository;
        this.adminEmail = adminEmail;
        this.allowedDomains = allowedDomains;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
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

        String name = oauth2User.getAttribute("name");
        String avatarUrl = oauth2User.getAttribute("picture");

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

        return new CustomOAuth2User(oauth2User, user);
    }
}
