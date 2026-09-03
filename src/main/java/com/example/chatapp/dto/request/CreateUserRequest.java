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

    public CreateUserRequest() {
    }

    public CreateUserRequest(String username, String email) {
        this.username = username;
        this.email = email;
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

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String email;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder email(String email) {
            this.email = email;
            return this;
        }

        public CreateUserRequest build() {
            return new CreateUserRequest(username, email);
        }
    }
}
