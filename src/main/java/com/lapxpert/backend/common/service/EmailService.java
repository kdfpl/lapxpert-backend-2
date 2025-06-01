package com.lapxpert.backend.common.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender emailSender;

    @Value("${spring.mail.username}")
    private String configuredSenderEmail;

    public void sendEmail(String to, String subject, String text) {
        sendEmail(to, subject, text, configuredSenderEmail);
    }

    public void sendBulkEmail(List<String> to, String subject, String text) {
        sendBulkEmail(to, subject, text, configuredSenderEmail);
    }


    public void sendEmail(String to, String subject, String text, String from) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);  // Đặt người gửi
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }


    public void sendBulkEmail(List<String> to, String subject, String text, String from) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);  // Đặt người gửi
        message.setTo(to.toArray(new String[0]));
        message.setSubject(subject);
        message.setText(text);
        emailSender.send(message);
    }

    public void sendPasswordEmail(String to, String rawPassword) {
        String subject = "Mật khẩu mới của bạn";
        String text = String.format(
                "Chào bạn,\n\n" +
                        "Mật khẩu mới của bạn là: %s\n\n" +
                        "Vui lòng không chia sẻ mật khẩu này cho bất kỳ ai để đảm bảo an toàn tài khoản.\n\n" +
                        "Trân trọng,\nLapXpert Store",
                rawPassword
        );

        sendEmail(to, subject, text);
    }

}
