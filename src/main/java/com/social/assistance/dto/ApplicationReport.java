package com.social.assistance.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationReport {
    private long totalApplications;
    private long approvedApplications;
    private long pendingApplications;
    private long rejectedApplications;
    private String programmeName;
}
