package com.example.chatapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request body to create a new user")
public class CreateUserRequest {

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
    @Schema(example = "priyank", description = "Unique username")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    @Schema(example = "priyank@example.com", description = "Unique user email")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 4, max = 100, message = "Password must be between 4 and 100 characters")
    @Schema(example = "password123", description = "User password")
    private String password;

    public CreateUserRequest() {
        this.password = "password123";
    }

    public CreateUserRequest(String username, String email) {
        this.username = username;
        this.email = email;
        this.password = "password123";
    }

    public CreateUserRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = (password != null && !password.isBlank()) ? password : "password123";
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String email;
        private String password = "password123";

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public CreateUserRequest build() {
            return new CreateUserRequest(username, email, password);
        }
    }
}
