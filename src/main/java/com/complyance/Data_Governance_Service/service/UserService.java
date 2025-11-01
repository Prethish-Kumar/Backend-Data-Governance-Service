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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

}
