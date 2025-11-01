package com.complyance.Data_Governance_Service.service;

import com.complyance.Data_Governance_Service.exception.ForbiddenException;
import com.complyance.Data_Governance_Service.exception.NotFoundException;
import com.complyance.Data_Governance_Service.model.UserPreference;
import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.repository.UserPreferenceRepository;
import com.complyance.Data_Governance_Service.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class UserPreferenceService {

    private final UserPreferenceRepository prefRepo;
    private final UserRepository userRepo;

    public UserPreferenceService(UserPreferenceRepository prefRepo, UserRepository userRepo) {
        this.prefRepo = prefRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public UserPreference updatePreferences(String userId, UserPreference prefs) {
        // ✅ 1. User existence check (404)
        UserProfile user = userRepo.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        // ✅ 2. User active check (403)
        if (user.isDeleted()) {
            throw new ForbiddenException("Cannot modify preferences of a soft-deleted user");
        }

        // ✅ 3. Handle preference existence and timestamps
        UserPreference existing = prefRepo.findByUserId(userId).orElse(null);

        if (existing != null) {
            if (existing.isDeleted()) {
                throw new ForbiddenException("Cannot update deleted preferences");
            }

            // Update fields
            existing.setTheme(prefs.getTheme());
            existing.setLanguage(prefs.getLanguage());
            existing.setNotificationsEnabled(prefs.isNotificationsEnabled());
            existing.setUpdatedAt(Instant.now());
            return prefRepo.save(existing);
        } else {
            // Create new preference entry
            prefs.setUserId(userId);
            prefs.setCreatedAt(Instant.now());
            prefs.setUpdatedAt(Instant.now());
            prefs.setDeleted(false);
            return prefRepo.save(prefs);
        }
    }

    public UserPreference getPreferences(String userId) {
        // ✅ Fail if user doesn't exist
        if (!userRepo.existsById(userId)) {
            throw new NotFoundException("User not found");
        }

        // ✅ Return only non-deleted preferences
        UserPreference pref = prefRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Preferences not found for user"));

        if (pref.isDeleted()) {
            throw new ForbiddenException("Preferences are deleted for this user");
        }

        return pref;
    }

    @Transactional
    public void softDeletePreferences(String userId) {
        UserPreference pref = prefRepo.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Preferences not found"));

        if (pref.isDeleted()) return; // idempotent

        pref.setDeleted(true);
        pref.setDeletedAt(Instant.now());
        pref.setUpdatedAt(Instant.now());
        prefRepo.save(pref);
    }
}
