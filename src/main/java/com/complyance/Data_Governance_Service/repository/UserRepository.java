package com.complyance.Data_Governance_Service.repository;

import com.complyance.Data_Governance_Service.model.UserPreference;
import com.complyance.Data_Governance_Service.model.UserProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface UserRepository extends MongoRepository<UserProfile, String> {
    Optional<UserProfile> findByIdAndDeletedFalse(String id);
    Page<UserProfile> findAllByDeletedFalse(Pageable pageable);

    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
}

