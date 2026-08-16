package de.frauas.design.backend.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Sends transactional emails (e.g. registration code) when mail is enabled.
 *
 * <p>Set {@code MAIL_ENABLED=true} and the MAIL_* env vars to activate.
 * When disabled, the code is logged at INFO level so manual testing remains possible.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:noreply@design.local}")
    private String fromAddress;

    public void sendRegistrationCode(String toEmail, String registrationCode) {
        if (!mailEnabled) {
            log.info("Mail disabled — registration code for {}: {}", toEmail, registrationCode);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(toEmail);
        msg.setSubject("DESIGN DaaS — Email Verification");
        msg.setText(
            "Welcome to DESIGN DaaS!\n\n" +
            "Your registration code is: " + registrationCode + "\n\n" +
            "This code expires in 24 hours.\n\n" +
            "Use POST /user/validate_email with your email and this code to activate your account."
        );
        mailSender.send(msg);
        log.info("Registration code sent to {}", toEmail);
    }

    /**
     * Sends an application-access request email to notify administrators.
     * Mirrors PHP MailService::enqueueApplicationRequestEmail().
     *
     * @param userEmail   the requesting user's email
     * @param userName    the requesting user's display name
     * @param userId      the requesting user's ID (as string)
     * @param application the application name being requested
     */
    public void sendApplicationRequestEmail(String userEmail, String userName,
                                            String userId, String application) {
        if (!mailEnabled) {
            log.info("Mail disabled — application request from user {} (id={}) for: {}",
                    userEmail, userId, application);
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom(fromAddress);
        msg.setTo(fromAddress); // send to the admin/system address
        msg.setReplyTo(userEmail);
        msg.setSubject("DESIGN DaaS — Application Access Request");
        msg.setText(
            "A user has requested access to an application.\n\n" +
            "User: " + userName + " (ID: " + userId + ")\n" +
            "Email: " + userEmail + "\n" +
            "Application: " + application + "\n\n" +
            "Please review and configure the appropriate permissions."
        );
        mailSender.send(msg);
        log.info("Application request email sent for user {} requesting {}", userEmail, application);
    }
}
