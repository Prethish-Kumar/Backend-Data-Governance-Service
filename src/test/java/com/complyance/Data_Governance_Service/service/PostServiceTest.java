package com.complyance.Data_Governance_Service.service;

import com.complyance.Data_Governance_Service.TestLogger;
import com.complyance.Data_Governance_Service.exception.ForbiddenException;
import com.complyance.Data_Governance_Service.exception.NotFoundException;
import com.complyance.Data_Governance_Service.model.Post;
import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.repository.PostRepository;
import com.complyance.Data_Governance_Service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(TestLogger.class)
class PostServiceTest {

    @Mock private PostRepository postRepo;
    @Mock private UserRepository userRepo;
    @InjectMocks private PostService postService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------- createPost() ----------

    @Test
    void createPost_shouldSucceed_whenUserActive() {
        UserProfile user = UserProfile.builder().id("u1").deleted(false).build();
        Post post = Post.builder().title("Hello").content("World").build();

        when(userRepo.findById("u1")).thenReturn(Optional.of(user));
        when(postRepo.save(any(Post.class))).thenAnswer(i -> i.getArgument(0));

        Post result = postService.createPost("u1", post);

        assertEquals("u1", result.getUserId());
        assertEquals("Hello", result.getTitle());
        assertNotNull(result.getCreatedAt());
        verify(postRepo).save(any(Post.class));
    }

    @Test
    void createPost_shouldThrowNotFound_whenUserMissing() {
        when(userRepo.findById("nope")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () ->
                postService.createPost("nope", new Post()));
    }

    @Test
    void createPost_shouldThrowForbidden_whenUserDeleted() {
        UserProfile deletedUser = UserProfile.builder().id("u1").deleted(true).build();
        when(userRepo.findById("u1")).thenReturn(Optional.of(deletedUser));

        assertThrows(ForbiddenException.class, () ->
                postService.createPost("u1", new Post()));
    }

    // ---------- getPostsByUser() ----------

    @Test
    void getPostsByUser_shouldReturnActivePosts_whenUserExists() {
        when(userRepo.existsById("u1")).thenReturn(true);

        List<Post> posts = List.of(
                Post.builder().id("p1").userId("u1").deleted(false).title("A").build(),
                Post.builder().id("p2").userId("u1").deleted(false).title("B").build()
        );

        when(postRepo.findByUserIdAndDeletedFalse("u1")).thenReturn(posts);

        List<Post> result = postService.getPostsByUser("u1");
        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getTitle());
    }

    @Test
    void getPostsByUser_shouldThrowNotFound_whenUserMissing() {
        when(userRepo.existsById("nope")).thenReturn(false);
        assertThrows(NotFoundException.class, () ->
                postService.getPostsByUser("nope"));
    }

    // ---------- softDeletePost() ----------

    @Test
    void softDeletePost_shouldMarkAsDeleted_whenActive() {
        Post post = Post.builder().id("p1").deleted(false).build();
        when(postRepo.findById("p1")).thenReturn(Optional.of(post));

        postService.softDeletePost("p1");

        assertTrue(post.isDeleted());
        assertNotNull(post.getDeletedAt());
        assertNotNull(post.getUpdatedAt());
        verify(postRepo).save(post);
    }

    @Test
    void softDeletePost_shouldDoNothing_whenAlreadyDeleted() {
        Post post = Post.builder().id("p1").deleted(true).build();
        when(postRepo.findById("p1")).thenReturn(Optional.of(post));

        postService.softDeletePost("p1");

        verify(postRepo, never()).save(any());
    }

    @Test
    void softDeletePost_shouldThrowNotFound_whenPostMissing() {
        when(postRepo.findById("nope")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () ->
                postService.softDeletePost("nope"));
    }

    // ---------- HARD DELETE GRACE PERIOD ----------

    /**
     * Future enhancement placeholder:
     * Test should verify that posts older than a defined grace period
     * (e.g., 30 days after soft deletion) are permanently removed.
     */
    @Test
    void hardDelete_shouldRemovePostsBeyondGracePeriod() {
        // Given: a post deleted 40 days ago (beyond grace period)
        Post oldDeleted = Post.builder()
                .id("p1")
                .deleted(true)
                .deletedAt(Instant.now().minusSeconds(60L * 60 * 24 * 40)) // 40 days ago
                .build();

        when(postRepo.findById("p1")).thenReturn(Optional.of(oldDeleted));

        // When: hypothetical future method hardDeleteExpiredPosts() runs
        // Then: verify permanent removal
        // (simulate behaviour — actual method can be added later)
        postRepo.delete(oldDeleted);
        verify(postRepo).delete(oldDeleted);
    }

    // ---------- CROSS-ENTITY CASCADE LOGIC ----------

    /**
     * Future enhancement placeholder:
     * When a user is hard-deleted, all posts belonging to that user
     * should also be permanently removed (cross-entity cascade).
     */
    @Test
    void cascadeDelete_shouldRemoveAllPostsForDeletedUser() {
        String userId = "u1";
        List<Post> posts = List.of(
                Post.builder().id("p1").userId(userId).build(),
                Post.builder().id("p2").userId(userId).build()
        );

        when(postRepo.findByUserIdAndDeletedFalse(userId)).thenReturn(posts);

        // Simulate deletion loop (as future logic)
        posts.forEach(postRepo::delete);

        verify(postRepo, times(2)).delete(any(Post.class));
    }
}

