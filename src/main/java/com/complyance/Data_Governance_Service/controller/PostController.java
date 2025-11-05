package com.complyance.Data_Governance_Service.controller;

import com.complyance.Data_Governance_Service.model.Post;
import com.complyance.Data_Governance_Service.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class PostController {
    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    // FR8 — Create Post
    @PostMapping("/users/{userId}/posts")
    public ResponseEntity<Post> createPost(@PathVariable String userId, @RequestBody Post post) {
        return ResponseEntity.ok(service.createPost(userId, post));
    }

    // FR9 — Get Posts (with optional pagination)
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<?> getPosts(
            @PathVariable String userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false, defaultValue = "createdAt,desc") String sort
    ) {
        return ResponseEntity.ok(service.getPostsByUser(userId, page, size, sort));
    }


    // FR10 — Soft Delete Post
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<Void> softDeletePost(@PathVariable String postId) {
        service.softDeletePost(postId);
        return ResponseEntity.noContent().build();
    }
}
