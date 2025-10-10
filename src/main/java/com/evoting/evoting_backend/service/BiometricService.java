package com.evoting.evoting_backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BiometricService {
    
    @Autowired
    private ImmutableAuditService auditService;
    
    // In-memory template storage (in production, use secure encrypted storage)
    private final Map<String, String> biometricTemplates = new ConcurrentHashMap<>();
    private final Map<String, Long> templateCreationTime = new ConcurrentHashMap<>();
    
    // Template expiration time (5 minutes)
    private static final long TEMPLATE_EXPIRY_MS = 5 * 60 * 1000;
    
    /**
     * Process facial image and create non-reversible template
     */
    public String processFacialImage(MultipartFile imageFile, String governmentId) {
        try {
            // Convert to biometric template (non-reversible)
            String template = extractFacialTemplate(imageFile.getBytes());
            String templateId = UUID.randomUUID().toString();
            
            // Store template with expiration
            biometricTemplates.put(templateId, template);
            templateCreationTime.put(templateId, System.currentTimeMillis());
            
            // Audit the processing
            auditService.logEvent("BIOMETRIC_TEMPLATE_CREATED", "BiometricService",
                "processFacialImage", "government_id:" + maskGovernmentId(governmentId) +
                ", template_id:" + templateId + ", stored:ephemeral");
            
            return templateId;
            
        } catch (Exception e) {
            auditService.logEvent("BIOMETRIC_PROCESSING_FAILED", "BiometricService",
                "processFacialImage", "government_id:" + maskGovernmentId(governmentId) +
                ", error:" + e.getMessage());
            throw new RuntimeException("Biometric processing failed: " + e.getMessage());
        }
    }
    
    /**
     * Verify biometric against stored template and DELETE after verification
     */
    public boolean verifyAndDeleteBiometric(String templateId, MultipartFile verificationImage, String governmentId) {
        try {
            cleanupExpiredTemplates();
            
            if (!biometricTemplates.containsKey(templateId)) {
                return false;
            }
            
            String storedTemplate = biometricTemplates.get(templateId);
            String verificationTemplate = extractFacialTemplate(verificationImage.getBytes());
            
            // Simple template matching (in production, use proper biometric algorithms)
            boolean match = verifyBiometricMatch(storedTemplate, verificationTemplate);
            
            // CRITICAL: DELETE template regardless of match result
            biometricTemplates.remove(templateId);
            templateCreationTime.remove(templateId);
            
            if (match) {
                auditService.logEvent("BIOMETRIC_VERIFIED_DELETED", "BiometricService",
                    "verifyAndDeleteBiometric", "government_id:" + maskGovernmentId(governmentId) +
                    ", template_id:" + templateId + ", status:verified_deleted");
            } else {
                auditService.logEvent("BIOMETRIC_MISMATCH_DELETED", "BiometricService",
                    "verifyAndDeleteBiometric", "government_id:" + maskGovernmentId(governmentId) +
                    ", template_id:" + templateId + ", status:rejected_deleted");
            }
            
            return match;
            
        } catch (Exception e) {
            // Ensure deletion even on error
            biometricTemplates.remove(templateId);
            templateCreationTime.remove(templateId);
            
            auditService.logEvent("BIOMETRIC_VERIFICATION_ERROR", "BiometricService",
                "verifyAndDeleteBiometric", "government_id:" + maskGovernmentId(governmentId) +
                ", error:" + e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract non-reversible facial template
     */
    private String extractFacialTemplate(byte[] imageData) {
        try {
            // In production, use proper biometric algorithms like:
            // - OpenFace, FaceNet, or commercial SDKs
            // - Convert to non-reversible feature vector
            
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
            
            // Simplified template extraction (hash of image features)
            String features = extractImageFeatures(image);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(features.getBytes());
            
            return Base64.getEncoder().encodeToString(hash);
            
        } catch (Exception e) {
            throw new RuntimeException("Facial template extraction failed", e);
        }
    }
    
    /**
     * Simple biometric verification (replace with proper algorithm)
     */
    private boolean verifyBiometricMatch(String template1, String template2) {
        // In production, use proper biometric matching with threshold
        // For demo: simple equality check
        return template1.equals(template2);
    }
    
    /**
     * Extract simplified image features (demo implementation)
     */
    private String extractImageFeatures(BufferedImage image) {
        // Simplified feature extraction - in production use proper computer vision
        StringBuilder features = new StringBuilder();
        
        // Basic image properties
        features.append("w:").append(image.getWidth())
                .append("h:").append(image.getHeight())
                .append("t:").append(System.currentTimeMillis());
        
        return features.toString();
    }
    
    /**
     * Clean up expired templates
     */
    private void cleanupExpiredTemplates() {
        long currentTime = System.currentTimeMillis();
        templateCreationTime.entrySet().removeIf(entry -> {
            if (currentTime - entry.getValue() > TEMPLATE_EXPIRY_MS) {
                biometricTemplates.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * Force cleanup all templates (for testing/emergency)
     */
    public void cleanupAllTemplates() {
        biometricTemplates.clear();
        templateCreationTime.clear();
        
        auditService.logEvent("BIOMETRIC_TEMPLATES_CLEARED", "BiometricService",
            "cleanupAllTemplates", "all_templates_cleared:true");
    }
    
    /**
     * Get template statistics
     */
    public Map<String, Object> getTemplateStats() {
        cleanupExpiredTemplates();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeTemplates", biometricTemplates.size());
        stats.put("timestamp", System.currentTimeMillis());
        
        return stats;
    }
    
    private String maskGovernmentId(String governmentId) {
        if (governmentId == null || governmentId.length() < 8) return "***";
        return governmentId.substring(0, 4) + "***" + governmentId.substring(governmentId.length() - 4);
    }
}