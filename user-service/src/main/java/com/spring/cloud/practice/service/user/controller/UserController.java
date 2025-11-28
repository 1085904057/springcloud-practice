package com.spring.cloud.practice.service.user.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/users")
public class UserController {

    private final ConcurrentHashMap<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        Long id = idGenerator.getAndIncrement();
        user.setId(id);
        users.put(id, user);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = users.get(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(user);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(new ArrayList<>(users.values()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User user = users.get(id);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        user.setName(userDetails.getName());
        user.setEmail(userDetails.getEmail());
        users.put(id, user);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        User removedUser = users.remove(id);
        if (removedUser == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }


    public static class User {
        private Long id;
        private String name;
        private String email;

        public User() {}

        public User(String name, String email) {
            this.name = name;
            this.email = email;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return "User{" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", email='" + email + '\'' +
                    '}';
        }
    }
}
