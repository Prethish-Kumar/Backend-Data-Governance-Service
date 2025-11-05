package com.complyance.Data_Governance_Service.controller;

import com.complyance.Data_Governance_Service.service.SeedService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/seed")
public class SeedController {

    private final SeedService seedService;

    public SeedController(SeedService seedService) {
        this.seedService = seedService;
    }

    @PostMapping
    public ResponseEntity<String> seed(
            @RequestParam(defaultValue = "10") int users,
            @RequestParam(defaultValue = "3") int postsPerUser
    ) {
        String result = seedService.seedUsersAndPosts(users, postsPerUser);
        return ResponseEntity.ok(result);
    }

}
