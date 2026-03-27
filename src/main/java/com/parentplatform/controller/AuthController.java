package com.parentplatform.controller;

import com.parentplatform.model.User;
import com.parentplatform.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public User register(@RequestBody User user) {
        return userService.register(user);
    }

    @PostMapping("/login")
    public Object login(@RequestBody User user) {

        Optional<User> u = userService.login(user.getEmail(), user.getPassword());

        if (u.isPresent()) {
            return u.get();
        } else {
            return "Email ou mot de passe incorrect";
        }
    }
}