package com.hungrybox.user;

import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.security.JwtService;
import com.hungrybox.common.web.ApiException;
import com.hungrybox.user.UserDtos.AuthResponse;
import com.hungrybox.user.UserDtos.LoginRequest;
import com.hungrybox.user.UserDtos.RegisterRequest;
import com.hungrybox.user.UserDtos.UpdateProfileRequest;
import com.hungrybox.user.UserDtos.UserView;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (users.existsByEmail(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        long id;
        try {
            id = users.insert(request.fullName().trim(), email, passwordEncoder.encode(request.password()),
                    request.phone(), AuthenticatedUser.ROLE_CUSTOMER);
        } catch (DuplicateKeyException e) {
            throw ApiException.conflict("An account with this email already exists");
        }
        return authResponse(getUser(id));
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email()))
                .filter(found -> passwordEncoder.matches(request.password(), found.passwordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        return authResponse(user);
    }

    public UserView profile(long userId) {
        return UserView.from(getUser(userId));
    }

    public UserView updateProfile(long userId, UpdateProfileRequest request) {
        getUser(userId);
        users.updateProfile(userId, request.fullName().trim(), request.phone(), request.address());
        return UserView.from(getUser(userId));
    }

    public List<UserView> listAll() {
        return users.findAll().stream().map(UserView::from).toList();
    }

    /** Creates the bootstrap admin account if it does not exist yet. */
    public boolean ensureAdmin(String email, String password) {
        String normalized = normalize(email);
        if (users.existsByEmail(normalized)) {
            return false;
        }
        users.insert("HungryBox Admin", normalized, passwordEncoder.encode(password), null,
                AuthenticatedUser.ROLE_ADMIN);
        return true;
    }

    private AuthResponse authResponse(User user) {
        String token = jwtService.issue(user.id(), user.role());
        return new AuthResponse(token, jwtService.ttlSeconds(), UserView.from(user));
    }

    private User getUser(long id) {
        return users.findById(id).orElseThrow(() -> ApiException.notFound("User " + id + " not found"));
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
