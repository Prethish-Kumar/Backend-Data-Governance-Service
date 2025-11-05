package com.complyance.Data_Governance_Service.controller;

import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public ResponseEntity<?> getAll(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String direction
    ) {
        if (page != null && size != null) {
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    direction.equalsIgnoreCase("desc")
                            ? org.springframework.data.domain.Sort.by(sortBy).descending()
                            : org.springframework.data.domain.Sort.by(sortBy).ascending()
            );
            return ResponseEntity.ok(service.getAllUsersPaged(pageable));
        }
        return ResponseEntity.ok(service.getAllUsers());
    }


    @PutMapping("/{id}")
    public ResponseEntity<UserProfile> updateUser(@PathVariable String id, @Valid @RequestBody UserProfile updatedUser) {
        return ResponseEntity.ok(service.updateUser(id, updatedUser));
    }


    @PatchMapping("/{id}")
    public ResponseEntity<UserProfile> patchUser(
            @PathVariable String id,
            @RequestBody UserProfile partialUpdate
    ) {
        return ResponseEntity.ok(service.patchUser(id, partialUpdate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        service.softDeleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restore")
    public ResponseEntity<UserProfile> restoreUser(@PathVariable String id) {
        return ResponseEntity.ok(service.restoreUser(id));
    }

    @PostMapping("/{id}/purge")
    public ResponseEntity<Void> purgeUser(@PathVariable String id) {
        service.purgeUser(id);
        return ResponseEntity.noContent().build();
    }

}
