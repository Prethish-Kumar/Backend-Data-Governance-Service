package com.complyance.Data_Governance_Service.service;

import com.complyance.Data_Governance_Service.TestLogger;
import com.complyance.Data_Governance_Service.exception.ConflictException;
import com.complyance.Data_Governance_Service.exception.ForbiddenException;
import com.complyance.Data_Governance_Service.exception.NotFoundException;
import com.complyance.Data_Governance_Service.model.AuditEntry;
import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.model.UserPreference;
import com.complyance.Data_Governance_Service.model.Post;
import com.complyance.Data_Governance_Service.repository.PostRepository;
import com.complyance.Data_Governance_Service.repository.UserPreferenceRepository;
import com.complyance.Data_Governance_Service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(TestLogger.class)
class UserServiceTest {

    @Mock private UserRepository userRepo;
    @Mock private UserPreferenceRepository prefRepo;
    @Mock private PostRepository postRepo;
    @InjectMocks private UserService userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // Set grace period (24h default)
        ReflectionTestUtils.setField(userService, "gracePeriodHours", 24L);
    }

    @Test
    void createUser_shouldSaveNewUser_whenUsernameUnique() {
        UserProfile user = UserProfile.builder()
                .username("john")
                .build();

        when(userRepo.existsByUsername("john")).thenReturn(false);
        when(userRepo.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile saved = userService.createUser(user);

        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("john", saved.getUsername());
        assertTrue(saved.getAuditTrail().stream().anyMatch(a -> a.getAction().equals("CREATE")));
        verify(userRepo).save(any(UserProfile.class));
    }

    @Test
    void createUser_shouldThrowConflict_whenUsernameExists() {
        UserProfile user = new UserProfile();
        user.setUsername("john");

        when(userRepo.existsByUsername("john")).thenReturn(true);

        assertThrows(ConflictException.class, () -> userService.createUser(user));
        verify(userRepo, never()).save(any());
    }

    @Test
    void getUser_shouldReturnUser_whenExistsAndNotDeleted() {
        UserProfile user = UserProfile.builder()
                .id("123")
                .deleted(false)
                .build();

        when(userRepo.findByIdAndDeletedFalse("123")).thenReturn(Optional.of(user));

        UserProfile result = userService.getUser("123");
        assertEquals("123", result.getId());
    }

    @Test
    void getUser_shouldThrowNotFound_whenMissingOrDeleted() {
        when(userRepo.findByIdAndDeletedFalse("123")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.getUser("123"));
    }

    @Test
    void softDeleteUser_shouldCascadeDeletePostsAndPrefs() {
        UserProfile user = UserProfile.builder()
                .id("1")
                .deleted(false)
                .auditTrail(new ArrayList<>())
                .build();

        Post p1 = new Post(); p1.setId("p1"); p1.setDeleted(false);
        Post p2 = new Post(); p2.setId("p2"); p2.setDeleted(false);

        UserPreference pref = new UserPreference();
        pref.setUserId("1");
        pref.setDeleted(false);

        when(userRepo.findByIdAndDeletedFalse("1")).thenReturn(Optional.of(user));
        when(postRepo.findByUserIdAndDeletedFalse("1")).thenReturn(List.of(p1, p2));
        when(prefRepo.findByUserId("1")).thenReturn(Optional.of(pref));

        userService.softDeleteUser("1");

        // User soft-deleted
        assertTrue(user.isDeleted());
        assertNotNull(user.getDeletedAt());

        // Posts soft-deleted
        verify(postRepo, times(2)).save(any(Post.class));

        // Pref soft-deleted
        verify(prefRepo).save(pref);

        // User saved
        verify(userRepo).save(user);
    }

    @Test
    void updateUser_shouldUpdateFieldsAndAudit() {
        UserProfile existing = UserProfile.builder()
                .id("1")
                .deleted(false)
                .name("Old")
                .email("old@test.com")
                .auditTrail(new ArrayList<>())
                .build();

        UserProfile update = UserProfile.builder()
                .name("New")
                .email("new@test.com")
                .roles(List.of("ADMIN"))
                .build();

        when(userRepo.findByIdAndDeletedFalse("1")).thenReturn(Optional.of(existing));
        when(userRepo.save(any(UserProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserProfile result = userService.updateUser("1", update);

        assertEquals("New", result.getName());
        assertEquals("new@test.com", result.getEmail());
        assertTrue(result.getAuditTrail().stream().anyMatch(a -> a.getAction().equals("UPDATE")));
        verify(userRepo).save(existing);
    }

    @Test
    void updateUser_shouldThrowNotFound_whenDeletedOrMissing() {
        when(userRepo.findByIdAndDeletedFalse("x")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.updateUser("x", new UserProfile()));
    }

    @Test
    void purgeUser_shouldDeleteAllAssociated_whenAfterGracePeriod() {
        Instant deletedAt = Instant.now().minusSeconds(25 * 3600); // 25h ago
        UserProfile user = UserProfile.builder()
                .id("u1")
                .deleted(true)
                .deletedAt(deletedAt)
                .auditTrail(new ArrayList<>())
                .build();

        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        userService.purgeUser("u1");

        verify(prefRepo).deleteByUserId("u1");
        verify(postRepo).deleteByUserId("u1");
        verify(userRepo).deleteById("u1");
        assertTrue(user.getAuditTrail().stream().anyMatch(a -> a.getAction().equals("HARD_DELETE")));
    }

    @Test
    void purgeUser_shouldThrowForbidden_ifUserNotSoftDeleted() {
        UserProfile user = UserProfile.builder()
                .id("u1")
                .deleted(false)
                .build();

        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> userService.purgeUser("u1"));
    }

    @Test

    void purgeUser_shouldThrowForbidden_ifWithinGracePeriod() {
        Instant deletedAt = Instant.now().minusSeconds(2 * 3600); // 2h ago
        UserProfile user = UserProfile.builder()
                .id("u1")
                .deleted(true)
                .deletedAt(deletedAt)
                .build();

        when(userRepo.findById("u1")).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> userService.purgeUser("u1"));
        verify(userRepo, never()).deleteById(any());
    }

    @Test

    void purgeUser_shouldThrowNotFound_ifMissing() {
        when(userRepo.findById("nope")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> userService.purgeUser("nope"));
    }
}
