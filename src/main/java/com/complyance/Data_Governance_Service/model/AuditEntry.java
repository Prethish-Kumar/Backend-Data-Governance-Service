package com.complyance.Data_Governance_Service.model;

import lombok.*;
import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuditEntry {
    private String action;      // e.g., "CREATE", "UPDATE", "SOFT_DELETE", "HARD_DELETE"
    private String performedBy; // Could be system or user ID
    private Instant timestamp;  // When the action occurred
    private String details;     // Optional: description or metadata about the change
}
