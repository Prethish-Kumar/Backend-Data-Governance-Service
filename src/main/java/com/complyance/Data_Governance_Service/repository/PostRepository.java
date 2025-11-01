package com.complyance.Data_Governance_Service.repository;

import com.complyance.Data_Governance_Service.model.Post;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findByUserIdAndDeletedFalse(String userId);
    void deleteByUserId(String userId);
}
