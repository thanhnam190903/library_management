package com.example.library_management.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;
    public void sendEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    public void sendEmailWithQr(String to, String subject, String requestId, byte[] qrBytes) throws MessagingException {

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        String html = """
        <h2>Yêu cầu mượn sách đã được duyệt</h2>
        <p>Mã phiếu: <b>%s</b></p>
        <p>Vui lòng đến nhận sách trong vòng 48 giờ.</p>
        <p>Khi đến hãy đưa mã cho thủ thư để được nhận sách nhanh nhất!</p>
        <img src='cid:qrcode'>
        """.formatted(requestId);
        helper.setText(html, true);
        helper.addInline("qrcode", new ByteArrayResource(qrBytes), "image/png");
        mailSender.send(message);
    }
}
