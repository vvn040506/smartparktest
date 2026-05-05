package com.smartpark.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EmailConfigValidator {

    @Value("${spring.mail.username:#{null}}")
    private String mailUsername;

    @Value("${spring.mail.password:#{null}}")
    private String mailPassword;

    @Value("${spring.mail.host}")
    private String mailHost;

    @Value("${spring.mail.port}")
    private int mailPort;

    @EventListener(ApplicationReadyEvent.class)
    public void validateEmailConfig() {
        System.out.println("=== Email Configuration Validation ===");
        
        if (mailUsername == null || mailUsername.trim().isEmpty()) {
            System.err.println("⚠️  WARNING: MAIL_USERNAME is not set or empty!");
        } else {
            System.out.println("✓ MAIL_USERNAME: " + maskEmail(mailUsername));
        }
        
        if (mailPassword == null || mailPassword.trim().isEmpty()) {
            System.err.println("⚠️  WARNING: MAIL_PASSWORD is not set or empty!");
        } else {
            System.out.println("✓ MAIL_PASSWORD: " + maskPassword(mailPassword));
        }
        
        System.out.println("✓ MAIL_HOST: " + mailHost);
        System.out.println("✓ MAIL_PORT: " + mailPort);
        
        if ((mailUsername == null || mailUsername.trim().isEmpty()) || 
            (mailPassword == null || mailPassword.trim().isEmpty())) {
            System.err.println("⚠️  Email functionality will NOT work without valid credentials!");
        } else {
            System.out.println("✓ Email configuration looks valid");
        }
        
        System.out.println("=====================================");
    }
    
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        return parts[0].substring(0, Math.min(3, parts[0].length())) + "***@" + parts[1];
    }
    
    private String maskPassword(String password) {
        if (password == null || password.length() < 4) return "***";
        return password.substring(0, 4) + "***";
    }
}
