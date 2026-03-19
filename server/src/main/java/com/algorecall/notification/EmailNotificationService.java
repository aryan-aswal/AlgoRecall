package com.algorecall.notification;

import com.algorecall.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private static final Pattern URL_PATTERN = Pattern.compile("(https?://[^\\s<]+)");

    private final JavaMailSender mailSender;

    @Value("${spring.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${spring.mail.username:noreply@algorecall.com}")
    private String fromAddress;

    @Value("${app.frontend-url:https://algo-recall-seven.vercel.app}")
    private String frontendUrl;

    public void send(Notification notification) {
        if (!mailEnabled) {
            log.debug("Email sending disabled. Would have sent to user {}: {}",
                    notification.getUser().getUsername(), notification.getMessage());
            return;
        }

        try {
            String userEmail = notification.getUser().getEmail();
            if (userEmail == null || userEmail.isBlank()) {
                log.warn("No email address for user {}", notification.getUser().getUsername());
                return;
            }

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(userEmail);
            helper.setSubject("AlgoRecall - Revision Reminder");
            helper.setText(buildHtmlBody(notification), true);

            mailSender.send(mimeMessage);
            log.info("Email sent to {} for notification #{}", userEmail, notification.getId());
        } catch (Exception e) {
            log.error("Failed to send email for notification #{}: {}", notification.getId(), e.getMessage());
        }
    }

    private String buildHtmlBody(Notification notification) {
        String username = notification.getUser().getUsername();
        String rawMessage = notification.getMessage();
        String ctaUrl = frontendUrl;

        // Convert bullet points/newlines to HTML and auto-link URLs in the message.
        String[] lines = rawMessage.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean inList = false;
        for (String line : lines) {
            String trimmed = line.stripTrailing();
            if (trimmed.isBlank()) {
                if (inList) { sb.append("</ul>"); inList = false; }
                sb.append("<div style=\"height:8px;\"></div>");
                continue;
            }

            if (trimmed.startsWith("• ")) {
                if (!inList) { sb.append("<ul style=\"margin:8px 0;padding-left:20px;\">"); inList = true; }
                String escaped = escapeHtml(trimmed.substring(2));
                sb.append("<li style=\"margin:6px 0;\">").append(linkifyUrls(escaped)).append("</li>");
            } else {
                if (inList) { sb.append("</ul>"); inList = false; }
                String escaped = escapeHtml(trimmed);
                sb.append("<p style=\"margin:4px 0;\">").append(linkifyUrls(escaped)).append("</p>");
            }
        }
        if (inList) sb.append("</ul>");
        String formattedMessage = sb.toString();

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 0; background: #f5f5f5; }
                    .container { max-width: 480px; margin: 20px auto; background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #6366f1, #8b5cf6); padding: 24px; text-align: center; }
                    .header h1 { color: white; margin: 0; font-size: 22px; }
                    .header p { color: rgba(255,255,255,0.8); margin: 4px 0 0; font-size: 13px; }
                    .body { padding: 24px; }
                    .greeting { font-size: 16px; color: #1f2937; margin-bottom: 16px; }
                    .reminder-card { background: #f8fafc; border: 1px solid #e2e8f0; border-left: 4px solid #6366f1; border-radius: 8px; padding: 16px; margin-bottom: 16px; }
                    .reminder-card p { color: #374151; font-size: 14px; line-height: 1.6; margin: 0; }
                    .cta { display: inline-block; background: #6366f1; color: white; padding: 10px 24px; border-radius: 8px; text-decoration: none; font-weight: 600; font-size: 14px; }
                    .footer { padding: 16px 24px; text-align: center; border-top: 1px solid #e2e8f0; }
                    .footer p { color: #9ca3af; font-size: 11px; margin: 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>AlgoRecall</h1>
                        <p>DSA Revision Tracker</p>
                    </div>
                    <div class="body">
                        <p class="greeting">Hey %s! 👋</p>
                        <div class="reminder-card">
                            %s
                        </div>
                        <p style="text-align: center; margin-top: 20px;">
                            <a href="%s" class="cta">Open AlgoRecall</a>
                        </p>
                    </div>
                    <div class="footer">
                        <p>You're receiving this because you have email notifications enabled.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(username, formattedMessage, ctaUrl);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String linkifyUrls(String escapedText) {
        Matcher matcher = URL_PATTERN.matcher(escapedText);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            String anchor = "<a href=\"" + url + "\" target=\"_blank\" rel=\"noopener noreferrer\" "
                    + "style=\"color:#4f46e5;text-decoration:none;\">" + url + "</a>";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(anchor));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
