package com.sanskaracademy.config;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TotpConfig {

    @Value("${totp.issuer:SanskarAcademyClasses}")
    private String issuer;

    /**
     * Google Authenticator configuration bean
     */
    @Bean
    public GoogleAuthenticator googleAuthenticator() {

        GoogleAuthenticatorConfig config =
                new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                        .setTimeStepSizeInMillis(30000)   // 30 seconds window
                        .setWindowSize(3)                 // Allow ±3 windows (90 sec tolerance)
                        .setCodeDigits(6)                 // 6 digit code
                        .build();

        return new GoogleAuthenticator(config);
    }

    /**
     * Issuer name used in QR Code generation
     */
    @Bean
    public String totpIssuer() {
        return issuer;
    }
}