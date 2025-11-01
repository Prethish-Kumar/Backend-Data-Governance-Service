package com.complyance.Data_Governance_Service.controller;

import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UserProfile> create(@Valid @RequestBody UserProfile user) {
        return ResponseEntity.status(201).body(service.createUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserProfile> get(@PathVariable String id) {
        return ResponseEntity.ok(service.getUser(id));
    }

    @GetMapping
    public ResponseEntity<List<UserProfile>> getAll() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> updateUser(@PathVariable String id, @Valid @RequestBody UserProfile updatedUser) {
        return ResponseEntity.ok(service.updateUser(id, updatedUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/purge")
    public ResponseEntity<Void> purgeUser(@PathVariable String id) {
        service.purgeUser(id);
        return ResponseEntity.noContent().build();
    }
}
