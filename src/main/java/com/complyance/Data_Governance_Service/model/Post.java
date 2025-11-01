package com.complyance.Data_Governance_Service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "posts")
public class Post {
    @Id
    private String id;
    private String userId;
    private String title;
    private String content;

    private boolean deleted = false;
    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
    private Instant deletedAt;
}
