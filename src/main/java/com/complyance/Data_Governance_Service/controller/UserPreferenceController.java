package com.complyance.Data_Governance_Service.controller;

import com.complyance.Data_Governance_Service.model.UserPreference;
import com.complyance.Data_Governance_Service.service.UserPreferenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}/preferences")
public class UserPreferenceController {

    private final UserPreferenceService service;

    public UserPreferenceController(UserPreferenceService service) {
        this.service = service;
    }

    @PutMapping
    public ResponseEntity<UserPreference> update(
            @PathVariable String userId,
            @RequestBody UserPreference prefs
    ) {
        return ResponseEntity.ok(service.updatePreferences(userId, prefs));
    }

    @GetMapping
    public ResponseEntity<UserPreference> get(@PathVariable String userId) {
        return ResponseEntity.ok(service.getPreferences(userId));
    }
}
