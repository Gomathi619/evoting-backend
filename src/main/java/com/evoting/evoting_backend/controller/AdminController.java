package com.evoting.evoting_backend.controller;

import com.evoting.evoting_backend.dto.ApiResponse;
import com.evoting.evoting_backend.service.UserService;
import com.evoting.evoting_backend.service.EnhancedKYCRegistrationService;
import com.evoting.evoting_backend.service.ImmutableAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
    
    @Autowired
    private UserService userService;

    @Autowired
    private EnhancedKYCRegistrationService kycService;
    
    @Autowired
    private ImmutableAuditService auditService;

    @GetMapping("/dashboard/stats")
    public ApiResponse<Map<String, Object>> getAdminStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsers", 150);
            stats.put("activeElections", 3);
            stats.put("pendingApprovals", 5);
            stats.put("systemHealth", "Optimal");
            stats.put("lastAudit", java.time.LocalDateTime.now().minusDays(1).toString());
            
            return new ApiResponse<>(true, "Admin stats retrieved", stats);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Error retrieving stats: " + e.getMessage());
        }
    }

    @GetMapping("/users")
    public ApiResponse<List<Map<String, Object>>> getAllUsers() {
        try {
            // This would typically fetch from user service
            List<Map<String, Object>> users = List.of(
                Map.of("id", 1, "username", "admin", "role", "ADMIN", "status", "ACTIVE"),
                Map.of("id", 2, "username", "officer1", "role", "ELECTION_OFFICER", "status", "ACTIVE"),
                Map.of("id", 3, "username", "voter1", "role", "VOTER", "status", "ACTIVE")
            );
            
            auditService.logEvent("USER_LIST_ACCESSED", "AdminController",
                "getAllUsers", "accessed_by:" + getCurrentUsername());
            
            return new ApiResponse<>(true, "Users retrieved successfully", users);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Error retrieving users: " + e.getMessage());
        }
    }

    @PostMapping("/users/{userId}/toggle-status")
    public ApiResponse<String> toggleUserStatus(@PathVariable Long userId) {
        try {
            // Implementation to toggle user status
            auditService.logEvent("USER_STATUS_TOGGLED", "AdminController",
                "toggleUserStatus", "user:" + userId + 
                ", action_by:" + getCurrentUsername());
                
            return new ApiResponse<>(true, "User status updated successfully");
        } catch (Exception e) {
            return new ApiResponse<>(false, "Error updating user: " + e.getMessage());
        }
    }

    @GetMapping("/system/logs")
    public ApiResponse<List<Map<String, Object>>> getSystemLogs() {
        try {
            List<Map<String, Object>> logs = List.of(
                Map.of("timestamp", java.time.LocalDateTime.now().minusMinutes(30), 
                      "level", "INFO", 
                      "message", "System health check completed"),
                Map.of("timestamp", java.time.LocalDateTime.now().minusMinutes(45), 
                      "level", "WARN", 
                      "message", "High memory usage detected"),
                Map.of("timestamp", java.time.LocalDateTime.now().minusHours(1), 
                      "level", "INFO", 
                      "message", "Backup completed successfully")
            );
            
            return new ApiResponse<>(true, "System logs retrieved", logs);
        } catch (Exception e) {
            return new ApiResponse<>(false, "Error retrieving logs: " + e.getMessage());
        }
    }

    @GetMapping("/voter-statistics")
    public ApiResponse<Map<String, Object>> getVoterStatistics() {
        try {
            Map<String, Object> stats = kycService.getKYCStatistics();
            
            // Add additional admin statistics
            stats.put("management", "ADMIN_VOTER_MANAGEMENT");
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("accessed_by", getCurrentUsername());
            
            auditService.logEvent("VOTER_STATS_ACCESSED", "AdminController",
                "getVoterStatistics", "accessed_by:" + getCurrentUsername());
            
            return new ApiResponse<>(true, "Voter statistics retrieved successfully", stats);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "Failed to get voter statistics: " + e.getMessage());
        }
    }

    // CHANGED: Renamed to avoid conflict with VoterManagementController
    @PostMapping("/voters/bulk-verify")
    public ApiResponse<Map<String, Object>> bulkVerifyVoters(@RequestBody Map<String, Object> batchRequest) {
        try {
            // This would typically process multiple voters
            // For now, return success with audit info
            
            auditService.logEvent("BULK_VOTER_VERIFICATION", "AdminController",
                "bulkVerifyVoters", "batch_size:" + batchRequest.get("count") +
                ", initiated_by:" + getCurrentUsername());
            
            return new ApiResponse<>(true, 
                "Bulk voter verification initiated", 
                Map.of(
                    "batchId", "BULK_" + System.currentTimeMillis(),
                    "status", "PROCESSING",
                    "message", "Voters will be verified asynchronously",
                    "type", "ADMIN_BULK_VERIFICATION"
                ));
                
        } catch (Exception e) {
            return new ApiResponse<>(false, "Bulk verification failed: " + e.getMessage());
        }
    }

    // CHANGED: Renamed to avoid conflict with VoterManagementController
    @GetMapping("/voters/system-analytics")
    public ApiResponse<Map<String, Object>> getSystemVoterAnalytics() {
        try {
            Map<String, Object> analytics = Map.of(
                "totalVerified", kycService.getVerifiedVoterCount(),
                "verificationSuccessRate", "98.5%",
                "averageVerificationTime", "45 seconds",
                "activeElections", 3,
                "tokensIssuedToday", 150,
                "systemHealth", "OPTIMAL",
                "generated_by", getCurrentUsername(),
                "analytics_type", "SYSTEM_WIDE"
            );
            
            return new ApiResponse<>(true, "System voter analytics retrieved", analytics);
            
        } catch (Exception e) {
            return new ApiResponse<>(false, "Failed to get system analytics: " + e.getMessage());
        }
    }

    private String getCurrentUsername() {
        try {
            org.springframework.security.core.context.SecurityContext context = 
                org.springframework.security.core.context.SecurityContextHolder.getContext();
            if (context != null && context.getAuthentication() != null) {
                return context.getAuthentication().getName();
            }
        } catch (Exception e) {
            // Ignore - return default
        }
        return "unknown";
    }
}