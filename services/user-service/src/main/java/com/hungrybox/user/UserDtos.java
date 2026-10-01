package com.hungrybox.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/** Request and response shapes for the user API. */
public final class UserDtos {

    private UserDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 120) String fullName,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8-72 characters") String password,
            @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "must be a valid phone number") String phone) {
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Pattern(regexp = "^[0-9+\\- ]{7,20}$", message = "must be a valid phone number") String phone,
            @Size(max = 255) String address) {
    }

    public record UserView(long id, String fullName, String email, String phone, String address,
                           String role, Instant createdAt) {
        static UserView from(User user) {
            return new UserView(user.id(), user.fullName(), user.email(), user.phone(), user.address(),
                    user.role(), user.createdAt());
        }
    }

    public record AuthResponse(String token, long expiresInSeconds, UserView user) {
    }
}
