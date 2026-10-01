package com.hungrybox.user;

import com.hungrybox.common.security.AuthenticatedUser;
import com.hungrybox.common.security.CurrentUser;
import com.hungrybox.common.web.ApiException;
import com.hungrybox.user.UserDtos.AuthResponse;
import com.hungrybox.user.UserDtos.LoginRequest;
import com.hungrybox.user.UserDtos.RegisterRequest;
import com.hungrybox.user.UserDtos.UpdateProfileRequest;
import com.hungrybox.user.UserDtos.UserView;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/me")
    public UserView me(@CurrentUser AuthenticatedUser user) {
        return userService.profile(user.userId());
    }

    @PutMapping("/me")
    public UserView updateMe(@CurrentUser AuthenticatedUser user, @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(user.userId(), request);
    }

    @GetMapping
    public List<UserView> listUsers(@CurrentUser AuthenticatedUser user) {
        if (!user.isAdmin()) {
            throw ApiException.forbidden("Admin role required");
        }
        return userService.listAll();
    }
}
