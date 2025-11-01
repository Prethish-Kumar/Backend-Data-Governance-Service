package com.complyance.Data_Governance_Service.repository;

import com.complyance.Data_Governance_Service.model.UserPreference;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends MongoRepository<UserPreference, String> {
    Optional<UserPreference> findByUserId(String userId);

    void deleteByUserId(String userId);
}
