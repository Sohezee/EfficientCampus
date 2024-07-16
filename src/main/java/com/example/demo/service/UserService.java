package com.example.demo.service;

import com.example.demo.entity.User;

import java.util.List;

public interface UserService {

    List<User> findAll();

    User findById(int id);

    User findByEmail(String email);

    User saveUser(User user);

    void deleteById(int id);
}
