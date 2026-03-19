package com.algorecall.notification;

import com.algorecall.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.util.ArrayList;
import java.util.List;
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

        ParsedMessage parsed = parseDetailedMessage(rawMessage);
        String structuredContent = renderStructuredContent(parsed);

        // Fallback to generic rendering if parser cannot detect sections.
        String contentHtml = structuredContent != null ? structuredContent : renderGenericContent(rawMessage);

        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; margin: 0; padding: 16px; background: #f3f4f6; color: #1f2937; }
                    .container { max-width: 760px; margin: 0 auto; background: #ffffff; border-radius: 14px; overflow: hidden; border: 1px solid #e5e7eb; }
                    .header { background: linear-gradient(135deg, #4f46e5, #7c3aed); padding: 26px; text-align: center; }
                    .header h1 { color: #ffffff; margin: 0; font-size: 34px; line-height: 1.2; }
                    .header p { color: rgba(255,255,255,0.88); margin: 8px 0 0; font-size: 15px; }
                    .body { padding: 28px; }
                    .greeting { font-size: 26px; color: #111827; margin: 0 0 14px; }
                    .intro { background: #eef2ff; border: 1px solid #c7d2fe; color: #312e81; border-radius: 10px; padding: 12px 14px; font-size: 14px; margin-bottom: 18px; }
                    .problem-card { border: 1px solid #dbe3ff; border-radius: 12px; padding: 16px; margin-bottom: 14px; background: #f8faff; }
                    .problem-title { font-size: 18px; font-weight: 700; color: #111827; margin: 0 0 10px; }
                    .meta { font-size: 14px; color: #374151; margin: 4px 0; line-height: 1.5; }
                    .meta .label { font-weight: 600; color: #111827; }
                    .meta a { color: #3730a3; text-decoration: none; }
                    .timeline { margin: 8px 0 0; padding-left: 18px; }
                    .timeline li { margin: 4px 0; font-size: 13px; color: #4b5563; }
                    .cta-wrap { text-align: center; margin-top: 22px; }
                    .cta-btn { display: inline-block; background: #312e81; color: #ffffff !important; text-decoration: none; font-weight: 700; font-size: 16px; padding: 14px 24px; border-radius: 12px; min-width: 240px; }
                    .footer { padding: 16px 24px; text-align: center; border-top: 1px solid #e5e7eb; }
                    .footer p { color: #6b7280; font-size: 12px; margin: 0; }
                    @media only screen and (max-width: 640px) {
                        body { padding: 8px; }
                        .container { border-radius: 10px; }
                        .header { padding: 18px; }
                        .header h1 { font-size: 26px; }
                        .header p { font-size: 14px; }
                        .body { padding: 16px; }
                        .greeting { font-size: 22px; }
                        .problem-title { font-size: 17px; }
                        .cta-btn { width: 100%%; min-width: auto; box-sizing: border-box; padding: 14px 18px; font-size: 16px; }
                    }
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
                        %s
                        <div class="cta-wrap">
                            <a href="%s" class="cta-btn">Open AlgoRecall</a>
                        </div>
                    </div>
                    <div class="footer">
                        <p>You're receiving this because you have email notifications enabled.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(escapeHtml(username), contentHtml, ctaUrl);
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
                    + "style=\"color:#3730a3;text-decoration:none;\">" + url + "</a>";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(anchor));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private String renderGenericContent(String rawMessage) {
        String[] lines = rawMessage.split("\\n");
        StringBuilder sb = new StringBuilder();
        boolean inList = false;

        for (String line : lines) {
            String trimmed = line.stripTrailing();
            if (trimmed.isBlank()) {
                if (inList) {
                    sb.append("</ul>");
                    inList = false;
                }
                sb.append("<div style=\"height:8px;\"></div>");
                continue;
            }

            if (trimmed.startsWith("• ")) {
                if (!inList) {
                    sb.append("<ul style=\"margin:8px 0;padding-left:20px;\">");
                    inList = true;
                }
                String escaped = escapeHtml(trimmed.substring(2));
                sb.append("<li style=\"margin:6px 0;\">").append(linkifyUrls(escaped)).append("</li>");
            } else {
                if (inList) {
                    sb.append("</ul>");
                    inList = false;
                }
                String escaped = escapeHtml(trimmed);
                sb.append("<p style=\"margin:4px 0;\">").append(linkifyUrls(escaped)).append("</p>");
            }
        }

        if (inList) {
            sb.append("</ul>");
        }
        return sb.toString();
    }

    private ParsedMessage parseDetailedMessage(String rawMessage) {
        String[] lines = rawMessage.split("\\n");
        ParsedMessage parsed = new ParsedMessage();

        int index = 0;
        while (index < lines.length && lines[index].trim().isBlank()) {
            index++;
        }
        if (index < lines.length && lines[index].startsWith("Your \"")) {
            parsed.sessionLine = lines[index].trim();
            index++;
        }

        while (index < lines.length) {
            String current = lines[index].trim();
            if (current.isBlank() || current.equalsIgnoreCase("Today's revision list:")) {
                index++;
                continue;
            }

            if (current.startsWith("Keep going")) {
                parsed.footerLine = current;
                break;
            }

            if (current.startsWith("Platform:") || current.startsWith("Current Revision:")
                    || current.startsWith("Problem Link:")) {
                index++;
                continue;
            }

            ProblemSection section = new ProblemSection();
            section.title = current;
            index++;

            while (index < lines.length) {
                String detail = lines[index].trim();
                if (detail.isBlank()) {
                    index++;
                    continue;
                }
                if (detail.startsWith("Keep going")) {
                    parsed.footerLine = detail;
                    break;
                }
                if (!detail.contains(":")) {
                    break;
                }

                if (detail.startsWith("Platform:")) {
                    section.platform = detail.substring("Platform:".length()).trim();
                } else if (detail.startsWith("Current Revision:")) {
                    section.currentRevision = detail.substring("Current Revision:".length()).trim();
                } else if (detail.startsWith("Problem Link:")) {
                    section.problemLink = detail.substring("Problem Link:".length()).trim();
                }
                index++;
            }

            parsed.problems.add(section);
        }

        if (parsed.sessionLine == null && parsed.problems.isEmpty()) {
            return null;
        }
        return parsed;
    }

    private String renderStructuredContent(ParsedMessage parsed) {
        if (parsed == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        if (parsed.sessionLine != null) {
            sb.append("<div class=\"intro\">")
                    .append(linkifyUrls(escapeHtml(parsed.sessionLine)))
                    .append("</div>");
        }

        for (ProblemSection section : parsed.problems) {
            sb.append("<div class=\"problem-card\">");
            sb.append("<h3 class=\"problem-title\">").append(linkifyUrls(escapeHtml(section.title))).append("</h3>");

            if (section.platform != null) {
                sb.append("<p class=\"meta\"><span class=\"label\">Platform:</span> ")
                        .append(linkifyUrls(escapeHtml(section.platform))).append("</p>");
            }
            if (section.currentRevision != null) {
                sb.append("<p class=\"meta\"><span class=\"label\">Current Revision:</span> ")
                        .append(linkifyUrls(escapeHtml(section.currentRevision))).append("</p>");
            }
            if (section.problemLink != null) {
                sb.append("<p class=\"meta\"><span class=\"label\">Problem Link:</span> ")
                        .append(linkifyUrls(escapeHtml(section.problemLink))).append("</p>");
            }

            sb.append("</div>");
        }

        if (parsed.footerLine != null) {
            sb.append("<p class=\"meta\" style=\"margin-top:10px;font-style:italic;\">")
                    .append(linkifyUrls(escapeHtml(parsed.footerLine)))
                    .append("</p>");
        }

        return sb.toString();
    }

    private static class ParsedMessage {
        private String sessionLine;
        private String footerLine;
        private final List<ProblemSection> problems = new ArrayList<>();
    }

    private static class ProblemSection {
        private String title;
        private String platform;
        private String currentRevision;
        private String problemLink;
    }
}
