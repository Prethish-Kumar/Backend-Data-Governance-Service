package com.complyance.Data_Governance_Service.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "preferences")
public class UserPreference {

    @Id
    private String id;

    @Indexed(unique = true)   // ensures only one pref per user
    private String userId;

    private String theme;
    private String language;
    private boolean notificationsEnabled;

    // Audit & lifecycle tracking
    private Instant createdAt;
    private Instant updatedAt;

    // Soft deletion tracking
    private boolean deleted;
    private Instant deletedAt;
}
