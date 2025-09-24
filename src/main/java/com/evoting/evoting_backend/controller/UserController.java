package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.model.User;
import com.evoting.evoting_backend.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired private UserService userService;

    @GetMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN') or #username == authentication.name")
    public User getUser(@PathVariable String username) {
        Optional<User> userOpt = userService.getByUsername(username);
        return userOpt.orElse(null);
    }
}