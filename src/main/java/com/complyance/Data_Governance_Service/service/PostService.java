package com.complyance.Data_Governance_Service.service;

import com.complyance.Data_Governance_Service.exception.ForbiddenException;
import com.complyance.Data_Governance_Service.exception.NotFoundException;
import com.complyance.Data_Governance_Service.model.Post;
import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.repository.PostRepository;
import com.complyance.Data_Governance_Service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class PostService {
    private final PostRepository postRepo;
    private final UserRepository userRepo;

    public PostService(PostRepository postRepo, UserRepository userRepo) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public Post createPost(String userId, Post post) {
        UserProfile user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (user.isDeleted()) {
            throw new ForbiddenException("Cannot create post for soft-deleted user");
        }

        post.setUserId(userId);
        post.setCreatedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        return postRepo.save(post);
    }

    public List<Post> getPostsByUser(String userId) {
        // Ensure user exists (404 if not)
        if (!userRepo.existsById(userId)) {
            throw new NotFoundException("User not found");
        }

        return postRepo.findByUserIdAndDeletedFalse(userId);
    }

    @Transactional
    public void softDeletePost(String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));

        if (post.isDeleted()) return; // idempotent

        post.setDeleted(true);
        post.setDeletedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        postRepo.save(post);
    }
}
