package fr.uge.project.ruge.services;

import fr.uge.project.ruge.entity.PasswordResetTokenEntity;
import fr.uge.project.ruge.repositories.PasswordResetTokenRepository;
import fr.uge.project.ruge.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Service
public class PasswordResetService {
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(UserRepository userRepository, PasswordResetTokenRepository tokenRepository, JavaMailSender mailSender, PasswordEncoder passwordEncoder) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.tokenRepository = Objects.requireNonNull(tokenRepository);
        this.mailSender = Objects.requireNonNull(mailSender);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }

    @Transactional
    public void processForgotPassword(String email) {
        var usr = userRepository.findByEmail(email);
        if (usr.isEmpty()) {
            return;
        }
        var user = usr.get();
        var token = UUID.randomUUID().toString();
        tokenRepository.deleteByUser(user);
        var myToken = new PasswordResetTokenEntity(token, user);
        tokenRepository.save(myToken);
        sendResetEmail(user.getEmail(), token);
    }

    private void sendResetEmail(String recipientEmail, String token) {
        var resetUrl = "http://localhost:8080/reset-password?token=" + token;

        var message = new SimpleMailMessage();
        message.setFrom("ruge.project@gmail.com");
        message.setTo(recipientEmail);
        message.setSubject("Reset your password");
        message.setText("Hello, \n\n"
                + "To reset your password, click the following link:\n"
                + resetUrl + "\n\n"
                + "This link will expire in 15 minutes.");
        mailSender.send(message);
    }

    public String updatePassword(String token, String newPassword) {
        var resetToken = tokenRepository.findByToken(token);
        if (resetToken == null) {
            return "Invalid token.";
        }
        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            return "The token has expired.";
        }
        var user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        tokenRepository.delete(resetToken);
        return "Password updated successfully.";
    }

    public String validatePasswordResetToken(String token) {
        var resetToken = tokenRepository.findByToken(token);
        if (resetToken == null) {
            return "This reset link is invalid.";
        }
        if (resetToken.isExpired()) {
            return "This reset link has expired. Please request a new one.";
        }
        return null;
    }
}
