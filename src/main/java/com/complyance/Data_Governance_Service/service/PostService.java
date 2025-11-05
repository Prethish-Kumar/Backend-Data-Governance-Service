package com.complyance.Data_Governance_Service.service;

import com.complyance.Data_Governance_Service.exception.ForbiddenException;
import com.complyance.Data_Governance_Service.exception.NotFoundException;
import com.complyance.Data_Governance_Service.model.Post;
import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.repository.PostRepository;
import com.complyance.Data_Governance_Service.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public Object getPostsByUser(String userId, Integer page, Integer size, String sort) {
        // Ensure user exists
        if (!userRepo.existsById(userId)) {
            throw new NotFoundException("User not found");
        }

        // If pagination params provided
        if (page != null && size != null) {
            String[] sortParts = sort.split(",");
            String sortBy = sortParts[0];
            String sortDir = (sortParts.length > 1) ? sortParts[1] : "asc";

            Sort sortObj = sortDir.equalsIgnoreCase("desc")
                    ? Sort.by(sortBy).descending()
                    : Sort.by(sortBy).ascending();

            Pageable pageable = PageRequest.of(page, size, sortObj);
            Page<Post> postPage = postRepo.findByUserIdAndDeletedFalse(userId, pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("posts", postPage.getContent());
            response.put("currentPage", postPage.getNumber());
            response.put("totalItems", postPage.getTotalElements());
            response.put("totalPages", postPage.getTotalPages());
            response.put("sort", sort);

            return response;
        }

        // No pagination → return all
        return postRepo.findByUserIdAndDeletedFalse(userId);
    }

    @Transactional
    public void softDeletePost(String postId) {
        Post post = postRepo.findById(postId)
                .orElseThrow(() -> new NotFoundException("Post not found"));

        if (post.isDeleted()) return;

        post.setDeleted(true);
        post.setDeletedAt(Instant.now());
        post.setUpdatedAt(Instant.now());
        postRepo.save(post);
    }
}
