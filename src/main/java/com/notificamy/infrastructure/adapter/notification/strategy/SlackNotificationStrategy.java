package com.notificamy.infrastructure.adapter.notification.strategy;

import com.notificamy.domain.model.NotificationChannel;
import com.notificamy.domain.model.NotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@ApplicationScoped
public class SlackNotificationStrategy implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(SlackNotificationStrategy.class);
    
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    
    public SlackNotificationStrategy() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Override
    public void sendNotification(NotificationRequest request) {
        LOG.infof("=== SLACK STRATEGY - STARTING ===");
        LOG.infof("Query ID: %d", request.getQueryId());
        LOG.infof("User ID: %d, Email: %s", request.getUser().getId(), request.getUser().getEmail());
        
        String webhookUrl = request.getUser().getChannelConfiguration(NotificationChannel.SLACK);
        LOG.infof("Slack webhook URL from user config: %s", 
                webhookUrl != null ? (webhookUrl.length() > 50 ? webhookUrl.substring(0, 50) + "..." : webhookUrl) : "null");
        
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            LOG.errorf("❌ SLACK STRATEGY FAILED - Webhook URL not configured for user ID %d (%s)", 
                    request.getUser().getId(), request.getUser().getEmail());
            throw new RuntimeException("Slack webhook URL not configured for user");
        }
        
        try {
            LOG.infof("💬 Preparing Slack notification to user %s (ID: %d) for query %d", 
                    request.getUser().getEmail(), request.getUser().getId(), request.getQueryId());
            
            Map<String, Object> payload = buildSlackPayload(request);
            LOG.infof("Slack payload created with %d top-level keys", payload.size());
            
            String requestBody = objectMapper.writeValueAsString(payload);
            LOG.infof("Slack request body length: %d characters", requestBody.length());
            LOG.infof("Slack request body preview: %s", 
                    requestBody.length() > 300 ? requestBody.substring(0, 300) + "..." : requestBody);
            
            LOG.infof("💬 Sending HTTP POST to Slack webhook...");
            long startTime = System.currentTimeMillis();
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            long endTime = System.currentTimeMillis();
            LOG.infof("💬 Slack HTTP response received:");
            LOG.infof("  Status Code: %d", response.statusCode());
            LOG.infof("  Response Body: %s", response.body());
            LOG.infof("  Duration: %dms", (endTime - startTime));
            
            if (response.statusCode() == 200) {
                LOG.infof("✅ SLACK MESSAGE SENT SUCCESSFULLY");
                LOG.infof("  User: %s (ID: %d)", request.getUser().getEmail(), request.getUser().getId());
                LOG.infof("  Query ID: %d", request.getQueryId());
            } else {
                LOG.errorf("❌ SLACK WEBHOOK ERROR");
                LOG.errorf("  User: %s", request.getUser().getEmail());
                LOG.errorf("  Status Code: %d", response.statusCode());
                LOG.errorf("  Response Body: %s", response.body());
                throw new RuntimeException("Slack webhook returned error: " + response.statusCode());
            }
            
        } catch (Exception e) {
            LOG.errorf("❌ SLACK SENDING FAILED");
            LOG.errorf("  User: %s (ID: %d)", request.getUser().getEmail(), request.getUser().getId());
            LOG.errorf("  Query ID: %d", request.getQueryId());
            LOG.errorf("  Error: %s", e.getMessage());
            LOG.errorf("  Exception type: %s", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                LOG.errorf("  Caused by: %s - %s", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            throw new RuntimeException("Failed to send Slack message", e);
        }
    }
    
    private Map<String, Object> buildSlackPayload(NotificationRequest request) {
        LOG.infof("💬 Building Slack payload for user: %s", request.getUser().getName());
        
        // Convert HTML AI response to Slack-compatible Markdown
        String slackFormattedResponse = convertHtmlToSlackMarkdown(request.getAiResponse());
        LOG.infof("💬 Converted AI response from HTML to Slack Markdown - Length: %d", slackFormattedResponse.length());
        
        Map<String, Object> payload = Map.of(
            "text", "Notificamy Notification",
            "blocks", List.of(
                Map.of(
                    "type", "header",
                    "text", Map.of(
                        "type", "plain_text",
                        "text", "🔔 Notificamy Notification"
                    )
                ),
                Map.of(
                    "type", "section",
                    "text", Map.of(
                        "type", "mrkdwn",
                        "text", String.format("Hello *%s*!\n\nYour notification request has been processed by our AI assistant.",
                                request.getUser().getName() != null ? request.getUser().getName() : "User")
                    )
                ),
                Map.of(
                    "type", "section",
                    "fields", List.of(
                        Map.of(
                            "type", "mrkdwn",
                            "text", String.format("*📝 Your Request:*\n\"%s\"", request.getPrompt())
                        ),
                        Map.of(
                            "type", "mrkdwn",
                            "text", String.format("*🤖 AI Response:*\n%s", slackFormattedResponse)
                        )
                    )
                ),
                Map.of(
                    "type", "context",
                    "elements", List.of(
                        Map.of(
                            "type", "mrkdwn",
                            "text", "Thank you for using Notificamy! 🚀"
                        )
                    )
                )
            )
        );
        
        LOG.infof("💬 Slack payload built successfully");
        return payload;
    }
    
    /**
     * Converts HTML content to Slack-compatible Markdown
     * Slack supports a subset of Markdown with some specific formatting rules
     */
    private String convertHtmlToSlackMarkdown(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return htmlContent;
        }
        
        LOG.infof("💬 Converting HTML to Slack Markdown - Input length: %d", htmlContent.length());
        
        String markdown = htmlContent;
        
        // Remove HTML comments (like <!-- <checked>true</checked> -->)
        markdown = markdown.replaceAll("<!--.*?-->", "");
        
        // Remove <style> blocks completely
        markdown = markdown.replaceAll("(?s)<style[^>]*>.*?</style>", "");
        
        // Convert HTML headings to Slack bold format
        markdown = markdown.replaceAll("(?s)<h[1-6][^>]*>(.*?)</h[1-6]>", "*$1*\n");
        
        // Convert <strong> and <b> to Slack bold
        markdown = markdown.replaceAll("(?s)<(?:strong|b)[^>]*>(.*?)</(?:strong|b)>", "*$1*");
        
        // Convert <em> and <i> to Slack italic
        markdown = markdown.replaceAll("(?s)<(?:em|i)[^>]*>(.*?)</(?:em|i)>", "_$1_");
        
        // Convert <code> to Slack inline code
        markdown = markdown.replaceAll("(?s)<code[^>]*>(.*?)</code>", "`$1`");
        
        // Convert <pre> to Slack code block
        markdown = markdown.replaceAll("(?s)<pre[^>]*>(.*?)</pre>", "```$1```");
        
        // Convert <a> links to Slack link format
        Pattern linkPattern = Pattern.compile("(?s)<a[^>]*href=[\"']([^\"']*)[\"'][^>]*>(.*?)</a>");
        Matcher linkMatcher = linkPattern.matcher(markdown);
        markdown = linkMatcher.replaceAll("<$1|$2>");
        
        // Convert <ul><li> to Slack bullet points
        markdown = markdown.replaceAll("(?s)<ul[^>]*>", "");
        markdown = markdown.replaceAll("(?s)</ul>", "");
        markdown = markdown.replaceAll("(?s)<li[^>]*>(.*?)</li>", "• $1\n");
        
        // Convert <ol><li> to Slack numbered list
        markdown = convertOrderedLists(markdown);
        
        // Convert <p> to line breaks
        markdown = markdown.replaceAll("(?s)<p[^>]*>(.*?)</p>", "$1\n\n");
        
        // Convert <br> to line breaks
        markdown = markdown.replaceAll("(?s)<br[^>]*>", "\n");
        
        // Convert <div> to line breaks (basic handling)
        markdown = markdown.replaceAll("(?s)<div[^>]*>(.*?)</div>", "$1\n");
        
        // Remove remaining HTML tags
        markdown = markdown.replaceAll("<[^>]+>", "");
        
        // Clean up HTML entities
        markdown = markdown.replace("&lt;", "<");
        markdown = markdown.replace("&gt;", ">");
        markdown = markdown.replace("&amp;", "&");
        markdown = markdown.replace("&quot;", "\"");
        markdown = markdown.replace("&#39;", "'");
        markdown = markdown.replace("&nbsp;", " ");
        
        // Clean up excessive whitespace and line breaks
        markdown = markdown.replaceAll("\\n{3,}", "\n\n"); // Max 2 consecutive line breaks
        markdown = markdown.replaceAll("[ \\t]+", " "); // Multiple spaces to single space
        markdown = markdown.trim();
        
        // Ensure emojis are preserved (Slack supports Unicode emojis)
        // No conversion needed for emojis like 🔔, 📈, etc.
        
        LOG.infof("💬 HTML to Slack Markdown conversion completed - Output length: %d", markdown.length());
        LOG.infof("💬 Converted content preview: %s", 
                markdown.length() > 200 ? markdown.substring(0, 200) + "..." : markdown);
        
        return markdown;
    }
    
    /**
     * Converts HTML ordered lists to numbered format for Slack
     */
    private String convertOrderedLists(String content) {
        // This is a simplified approach - for more complex nested lists, 
        // you might need a more sophisticated parser
        Pattern olPattern = Pattern.compile("(?s)<ol[^>]*>(.*?)</ol>");
        Matcher olMatcher = olPattern.matcher(content);
        
        StringBuffer result = new StringBuffer();
        while (olMatcher.find()) {
            String listContent = olMatcher.group(1);
            Pattern liPattern = Pattern.compile("(?s)<li[^>]*>(.*?)</li>");
            Matcher liMatcher = liPattern.matcher(listContent);
            
            StringBuilder numberedList = new StringBuilder();
            int counter = 1;
            while (liMatcher.find()) {
                numberedList.append(counter).append(". ").append(liMatcher.group(1).trim()).append("\n");
                counter++;
            }
            
            olMatcher.appendReplacement(result, numberedList.toString());
        }
        olMatcher.appendTail(result);
        
        return result.toString();
    }
}