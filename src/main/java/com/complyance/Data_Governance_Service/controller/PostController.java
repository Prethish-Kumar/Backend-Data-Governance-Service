package com.complyance.Data_Governance_Service.controller;

import com.complyance.Data_Governance_Service.model.Post;
import com.complyance.Data_Governance_Service.service.PostService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PostController {
    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    // FR8
    @PostMapping("/users/{userId}/posts")
    public ResponseEntity<Post> createPost(@PathVariable String userId, @RequestBody Post post) {
        return ResponseEntity.ok(service.createPost(userId, post));
    }

    // FR9
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<List<Post>> getPosts(@PathVariable String userId) {
        return ResponseEntity.ok(service.getPostsByUser(userId));
    }

    // FR10
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> softDeletePost(@PathVariable String postId) {
        service.softDeletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
