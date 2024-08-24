package com.example.demo.rest;

import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.util.BrowserManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserRestController {

    private UserService userService;

    @Autowired
    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<User> findAll() {
        return userService.findAll();
    }

    @GetMapping("/users/{id}")
    public User findById(@PathVariable int id) {
        User user = userService.findById(id);
        if (user == null)  {
            throw new RuntimeException("User id not found - " + id);
        }
        return user;
    }

    @PostMapping("/users")
    public User addUser(@RequestBody User user) {
        if(userService.findAll().stream().anyMatch(dbUser -> dbUser.getEmail().equals(user.getEmail()))) throw new EmailExistsException(user.getEmail());
        user.setId(0);
        return userService.saveUser(user);
    }

    @PutMapping("/users")
    public User updateUser(@RequestBody User user) {return userService.saveUser(user);}

    @DeleteMapping("/users/{id}")
    public String deleteById(@PathVariable int id) {
        User user = userService.findById(id);
        if (user == null)  {
            throw new RuntimeException("User id not found - " + id);
        }
        userService.deleteById(user.getId());
        return "Deleted user id - " + id;
    }

    @DeleteMapping("/users")
    public String deleteByEmail(@RequestBody Map<String, String> email) {
        String retrievedEmail = email.get("email");
        User user = userService.findByEmail(retrievedEmail);
        if (user == null)  {
            throw new RuntimeException("User email not found - " + email);
        }
        int id = user.getId();
        userService.deleteById(id);
        return "Deleted user id - " + id;
    }

    @PostMapping("/users/login")
    public User login(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");

        if (email == null || password == null) {
            throw new InvalidCredentialsException("Email and password must be provided");
        }

        User foundUser = userService.findByEmail(email);
        if(foundUser == null) {
            throw new UnregisteredUserException();
        }
        else if (!foundUser.getPassword().equals(password)) {
            throw new InvalidCredentialsException("Invalid email or password");
        } else {
            return foundUser;
        }
    }

    @PostMapping("/users/verify")
    public boolean verify(@RequestBody Map<String, String> credentials) {
        String email = credentials.get("email");
        String password = credentials.get("password");
        int attempts = 1;
        if (credentials.get("attempts") != null) attempts = Integer.parseInt(credentials.get("attempts"));

        if (email == null || password == null) {
            throw new InvalidCredentialsException("Email and password must be provided");
        }
        for (int i = 0; i < attempts; i++) {
            try (BrowserManager browser = new BrowserManager(true)) {
                if (browser.pageSetup(email, password) != null) return true;
            }
        }
        return false;
    }
}


