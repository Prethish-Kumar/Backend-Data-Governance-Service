package com.complyance.Data_Governance_Service.service;

import com.complyance.Data_Governance_Service.exception.ConflictException;
import com.complyance.Data_Governance_Service.exception.ForbiddenException;
import com.complyance.Data_Governance_Service.model.AuditEntry;
import com.complyance.Data_Governance_Service.model.UserProfile;
import com.complyance.Data_Governance_Service.repository.PostRepository;
import com.complyance.Data_Governance_Service.repository.UserPreferenceRepository;
import com.complyance.Data_Governance_Service.repository.UserRepository;
import com.complyance.Data_Governance_Service.exception.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final UserRepository repo;
    private final UserPreferenceRepository prefRepo;
    private final PostRepository postRepo;

    @Value("${user.purge.grace-period-hours}")
    private long gracePeriodHours;

    public UserService(UserRepository repo,
                       UserPreferenceRepository prefRepo,
                       PostRepository postRepo) {
        this.repo = repo;
        this.prefRepo = prefRepo;
        this.postRepo = postRepo;
    }

    private void addAudit(UserProfile user, String action, String details) {
        if (user.getAuditTrail() == null) {
            user.setAuditTrail(new ArrayList<>());
        }

        AuditEntry entry = AuditEntry.builder()
                .action(action)
                .performedBy("SYSTEM")  // or later from SecurityContext
                .timestamp(Instant.now())
                .details(details)
                .build();

        user.getAuditTrail().add(entry);
    }


    public UserProfile createUser(UserProfile user) {
        if (repo.existsByUsername(user.getUsername())) {
            throw new ConflictException("Username already exists");
        }
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        addAudit(user, "CREATE", "User account created");
        return repo.save(user);
    }

    public Page<UserProfile> getAllUsersPaged(Pageable pageable) {
        return repo.findAllByDeletedFalse(pageable);
    }

    public UserProfile getUser(String id) {
        return repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Transactional
    public void softDeleteUser(String id) {
        UserProfile user = getUser(id);
        user.setDeleted(true);
        user.setDeletedAt(Instant.now());

        postRepo.findByUserIdAndDeletedFalse(id).forEach(post -> {
            post.setDeleted(true);
            post.setDeletedAt(Instant.now());
            postRepo.save(post);
        });

        prefRepo.findByUserId(id).ifPresent(pref -> {
            pref.setDeleted(true);
            pref.setDeletedAt(Instant.now());
            prefRepo.save(pref);
        });

        addAudit(user, "SOFT_DELETE", "User soft-deleted");
        repo.save(user);
    }

    public List<UserProfile> getAllUsers() {
        return repo.findAll()
                .stream().filter(u -> !u.isDeleted()).toList();
    }

    @Transactional
    public UserProfile updateUser(String id, UserProfile updatedData) {
        UserProfile existingUser = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("User not found or is deleted"));

        // Update only allowed fields
        if (updatedData.getName() != null) existingUser.setName(updatedData.getName());
        if (updatedData.getEmail() != null) existingUser.setEmail(updatedData.getEmail());
        if (updatedData.getRoles() != null && !updatedData.getRoles().isEmpty())
            existingUser.setRoles(updatedData.getRoles());

        if (updatedData.getStatus() != null &&
                !updatedData.getStatus().equalsIgnoreCase(existingUser.getStatus())) {
            existingUser.setStatus(updatedData.getStatus());
            addAudit(existingUser, "STATUS_UPDATE",
                    "User status changed to " + updatedData.getStatus());
        }

        existingUser.setUpdatedAt(Instant.now());
        addAudit(existingUser, "UPDATE", "User profile updated");

        return repo.save(existingUser);
    }


    @Transactional
    public void purgeUser(String id) {
        UserProfile user = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isDeleted()) {
            throw new ForbiddenException("User must be soft-deleted before purge");
        }

        // Compute grace period in milliseconds
        long gracePeriodMillis = gracePeriodHours * 60 * 60 * 1000;

        Instant deletedAt = user.getDeletedAt();
        if (deletedAt == null || Instant.now().isBefore(deletedAt.plusMillis(gracePeriodMillis))) {
            throw new ForbiddenException("Cannot purge user before " + gracePeriodHours + "h grace period has passed");
        }

        prefRepo.deleteByUserId(id);
        postRepo.deleteByUserId(id);
        addAudit(user, "HARD_DELETE", "User permanently deleted");
        repo.deleteById(id);
    }
    @Transactional
    public UserProfile patchUser(String id, UserProfile partialUpdate) {
        UserProfile existing = repo.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new NotFoundException("User not found or is deleted"));

        StringBuilder changedFields = new StringBuilder();

        if (partialUpdate.getName() != null && !partialUpdate.getName().equals(existing.getName())) {
            existing.setName(partialUpdate.getName());
            changedFields.append("name, ");
        }

        if (partialUpdate.getEmail() != null && !partialUpdate.getEmail().equals(existing.getEmail())) {
            existing.setEmail(partialUpdate.getEmail());
            changedFields.append("email, ");
        }

        if (partialUpdate.getUsername() != null && !partialUpdate.getUsername().equals(existing.getUsername())) {
            existing.setUsername(partialUpdate.getUsername());
            changedFields.append("username, ");
        }

        if (partialUpdate.getRoles() != null && !partialUpdate.getRoles().isEmpty()
                && !partialUpdate.getRoles().equals(existing.getRoles())) {
            existing.setRoles(partialUpdate.getRoles());
            changedFields.append("roles, ");
        }

        // ✅ Handle status update
        if (partialUpdate.getStatus() != null && !partialUpdate.getStatus().equals(existing.getStatus())) {
            existing.setStatus(partialUpdate.getStatus());
            changedFields.append("status, ");
        }

        existing.setUpdatedAt(Instant.now());

        // 🧾 Add audit entry
        String details = !changedFields.isEmpty()
                ? "Updated fields: " + changedFields.substring(0, changedFields.length() - 2)
                : "No fields changed";
        addAudit(existing, "PATCH_UPDATE", details);

        return repo.save(existing);
    }


    @Transactional
    public UserProfile restoreUser(String id) {
        UserProfile user = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.isDeleted()) {
            throw new ForbiddenException("User is not deleted — nothing to restore.");
        }

        // Check if still within grace period
        long gracePeriodMillis = gracePeriodHours * 60 * 60 * 1000;
        Instant deletedAt = user.getDeletedAt();

        if (deletedAt == null || Instant.now().isAfter(deletedAt.plusMillis(gracePeriodMillis))) {
            throw new ForbiddenException("Cannot restore — grace period has expired.");
        }

        // Restore user
        user.setDeleted(false);
        user.setDeletedAt(null);
        user.setUpdatedAt(Instant.now());
        addAudit(user, "RESTORE", "User restored from soft-deletion");

        // Restore posts
        postRepo.findByUserIdAndDeletedTrue(id).forEach(post -> {
            post.setDeleted(false);
            post.setDeletedAt(null);
            postRepo.save(post);
        });

        // Restore preferences
        prefRepo.findByUserId(id).ifPresent(pref -> {
            pref.setDeleted(false);
            pref.setDeletedAt(null);
            prefRepo.save(pref);
        });

        return repo.save(user);
    }
}
