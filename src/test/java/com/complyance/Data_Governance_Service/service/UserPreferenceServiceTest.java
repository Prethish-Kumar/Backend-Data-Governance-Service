package com.complyance.Data_Governance_Service.service;

import com.complyance.Data_Governance_Service.TestLogger;
import com.complyance.Data_Governance_Service.exception.ForbiddenException;
import com.complyance.Data_Governance_Service.exception.NotFoundException;
import com.complyance.Data_Governance_Service.model.UserPreference;
import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.repository.UserPreferenceRepository;
import com.complyance.Data_Governance_Service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(TestLogger.class)
class UserPreferenceServiceTest {

    @Mock private UserPreferenceRepository prefRepo;
    @Mock private UserRepository userRepo;
    @InjectMocks private UserPreferenceService prefService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ---------- updatePreferences() ----------

    @Test
    void updatePreferences_shouldCreateNew_whenNoExistingPref() {
        UserProfile user = UserProfile.builder().id("u1").deleted(false).build();
        UserPreference newPref = UserPreference.builder()
                .theme("dark").language("en").notificationsEnabled(true).build();

        when(userRepo.findById("u1")).thenReturn(Optional.of(user));
        when(prefRepo.findByUserId("u1")).thenReturn(Optional.empty());
        when(prefRepo.save(any(UserPreference.class))).thenAnswer(i -> i.getArgument(0));

        UserPreference saved = prefService.updatePreferences("u1", newPref);

        assertEquals("u1", saved.getUserId());
        assertEquals("dark", saved.getTheme());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        verify(prefRepo).save(any(UserPreference.class));
    }

    @Test
    void updatePreferences_shouldUpdateExisting_whenActivePref() {
        UserProfile user = UserProfile.builder().id("u1").deleted(false).build();
        UserPreference existing = UserPreference.builder()
                .userId("u1").theme("light").language("fr").deleted(false).build();

        UserPreference update = UserPreference.builder()
                .theme("dark").language("en").notificationsEnabled(true).build();

        when(userRepo.findById("u1")).thenReturn(Optional.of(user));
        when(prefRepo.findByUserId("u1")).thenReturn(Optional.of(existing));
        when(prefRepo.save(any(UserPreference.class))).thenAnswer(i -> i.getArgument(0));

        UserPreference result = prefService.updatePreferences("u1", update);

        assertEquals("dark", result.getTheme());
        assertEquals("en", result.getLanguage());
        assertTrue(result.isNotificationsEnabled());
        assertNotNull(result.getUpdatedAt());
        verify(prefRepo).save(existing);
    }

    @Test
    void updatePreferences_shouldThrowNotFound_whenUserMissing() {
        when(userRepo.findById("nope")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () ->
                prefService.updatePreferences("nope", new UserPreference()));
    }

    @Test
    void updatePreferences_shouldThrowForbidden_whenUserDeleted() {
        UserProfile deletedUser = UserProfile.builder().id("u1").deleted(true).build();
        when(userRepo.findById("u1")).thenReturn(Optional.of(deletedUser));

        assertThrows(ForbiddenException.class, () ->
                prefService.updatePreferences("u1", new UserPreference()));
    }

    @Test
    void updatePreferences_shouldThrowForbidden_whenPrefDeleted() {
        UserProfile user = UserProfile.builder().id("u1").deleted(false).build();
        UserPreference deletedPref = UserPreference.builder().userId("u1").deleted(true).build();

        when(userRepo.findById("u1")).thenReturn(Optional.of(user));
        when(prefRepo.findByUserId("u1")).thenReturn(Optional.of(deletedPref));

        assertThrows(ForbiddenException.class, () ->
                prefService.updatePreferences("u1", new UserPreference()));
    }

    // ---------- getPreferences() ----------

    @Test
    void getPreferences_shouldReturnActivePrefs_whenExists() {
        when(userRepo.existsById("u1")).thenReturn(true);
        UserPreference pref = UserPreference.builder()
                .userId("u1").deleted(false).theme("dark").build();

        when(prefRepo.findByUserId("u1")).thenReturn(Optional.of(pref));

        UserPreference result = prefService.getPreferences("u1");
        assertEquals("dark", result.getTheme());
    }

    @Test
    void getPreferences_shouldThrowNotFound_whenUserMissing() {
        when(userRepo.existsById("nope")).thenReturn(false);
        assertThrows(NotFoundException.class, () ->
                prefService.getPreferences("nope"));
    }

    @Test
    void getPreferences_shouldThrowNotFound_whenPrefMissing() {
        when(userRepo.existsById("u1")).thenReturn(true);
        when(prefRepo.findByUserId("u1")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                prefService.getPreferences("u1"));
    }

    @Test
    void getPreferences_shouldThrowForbidden_whenPrefDeleted() {
        when(userRepo.existsById("u1")).thenReturn(true);
        UserPreference pref = UserPreference.builder().userId("u1").deleted(true).build();
        when(prefRepo.findByUserId("u1")).thenReturn(Optional.of(pref));

        assertThrows(ForbiddenException.class, () ->
                prefService.getPreferences("u1"));
    }

    // ---------- softDeletePreferences() ----------

    @Test
    void softDeletePreferences_shouldMarkAsDeleted_whenActive() {
        UserPreference pref = UserPreference.builder()
                .userId("u1").deleted(false).build();

        when(prefRepo.findByUserId("u1")).thenReturn(Optional.of(pref));

        prefService.softDeletePreferences("u1");

        assertTrue(pref.isDeleted());
        assertNotNull(pref.getDeletedAt());
        verify(prefRepo).save(pref);
    }

    @Test
    void softDeletePreferences_shouldDoNothing_whenAlreadyDeleted() {
        UserPreference pref = UserPreference.builder().userId("u1").deleted(true).build();
        when(prefRepo.findByUserId("u1")).thenReturn(Optional.of(pref));

        prefService.softDeletePreferences("u1");

        verify(prefRepo, never()).save(any());
    }

    @Test
    void softDeletePreferences_shouldThrowNotFound_whenMissing() {
        when(prefRepo.findByUserId("nope")).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () ->
                prefService.softDeletePreferences("nope"));
    }
}
