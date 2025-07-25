package com.notificamy.infrastructure.adapter.notification.strategy;

import  com.notificamy.domain.model.NotificationRequest;
import com.notificamy.infrastructure.config.SmtpConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

@ApplicationScoped
public class EmailNotificationStrategy implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(EmailNotificationStrategy.class);
    
    @Inject
    SmtpConfig smtpConfig;
    
    @ConfigProperty(name = "app.email.from-name")
    String fromName;
    
    @Override
    public void sendNotification(NotificationRequest request) {
        LOG.infof("=== EMAIL STRATEGY - STARTING ===");
        LOG.infof("Query ID: %d", request.getQueryId());
        LOG.infof("User ID: %d", request.getUser().getId());
        
        // Check if user has email configured
        String userEmail = request.getUser().getEmail();
        LOG.infof("User email from database: %s", userEmail);
        
        if (userEmail == null || userEmail.trim().isEmpty()) {
            LOG.errorf("❌ EMAIL STRATEGY FAILED - Email address not configured for user ID %d", 
                    request.getUser().getId());
            throw new RuntimeException("Email address not configured for user");
        }
        
        try {
            LOG.infof("📧 Preparing email notification to %s for query %d", 
                    userEmail, request.getQueryId());
            
            String subject = "Notificamy: Your AI-Generated Notification";
            LOG.infof("Email subject: %s", subject);
            
            String htmlBody = buildHtmlEmailBody(request);
            LOG.infof("Email body built - Length: %d characters", htmlBody.length());
            LOG.infof("Email body preview (first 200 chars): %s", 
                    htmlBody.length() > 200 ? htmlBody.substring(0, 200) + "..." : htmlBody);
            
            // Configura le proprietà SMTP
            Properties props = new Properties();
            props.put("mail.smtp.host", smtpConfig.getSmtpHost());
            props.put("mail.smtp.port", String.valueOf(smtpConfig.getSmtpPort()));
            props.put("mail.smtp.auth", String.valueOf(smtpConfig.isSmtpAuth()));
            props.put("mail.smtp.starttls.enable", String.valueOf(smtpConfig.isSmtpStartTls()));
            props.put("mail.smtp.starttls.required", String.valueOf(smtpConfig.isSmtpStartTls()));
            props.put("mail.smtp.ssl.protocols", "TLSv1.2");
            
            LOG.infof("📧 SMTP Configuration:");
            LOG.infof("  Host: %s", smtpConfig.getSmtpHost());
            LOG.infof("  Port: %d", smtpConfig.getSmtpPort());
            LOG.infof("  Username: %s", smtpConfig.getSmtpUsername());
            LOG.infof("  From Email: %s", smtpConfig.getFromEmail());
            LOG.infof("  From Name: %s", fromName);
            LOG.infof("  Auth: %s", smtpConfig.isSmtpAuth());
            LOG.infof("  StartTLS: %s", smtpConfig.isSmtpStartTls());
            
            LOG.infof("📧 Email Details:");
            LOG.infof("  To: %s", userEmail);
            LOG.infof("  Subject: %s", subject);
            LOG.infof("  Content-Type: text/html; charset=UTF-8");
            
            LOG.infof("📧 Creating SMTP session...");
                    smtpConfig.getSmtpHost(), smtpConfig.getSmtpPort(), 
                    smtpConfig.isSmtpAuth(), smtpConfig.isSmtpStartTls());
            
            // Crea la sessione SMTP
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(smtpConfig.getSmtpUsername(), smtpConfig.getSmtpPassword());
                }
            });
            
            // Abilita debug per troubleshooting
            session.setDebug(false); // Disabilitato per evitare troppo output, riabilitare se necessario
            
            LOG.infof("📧 SMTP session created successfully");
            
            // Crea il messaggio
            LOG.infof("📧 Creating email message...");
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(smtpConfig.getFromEmail(), fromName));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(userEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            
            LOG.infof("📧 Email message created, attempting to send...");
            long startTime = System.currentTimeMillis();
            
            // Invia l'email
            Transport.send(message);
            
            long endTime = System.currentTimeMillis();
            LOG.infof("✅ EMAIL SENT SUCCESSFULLY");
            LOG.infof("  To: %s", userEmail);
            LOG.infof("  Query ID: %d", request.getQueryId());
            LOG.infof("  Send duration: %dms", (endTime - startTime));
            
        } catch (Exception e) {
            LOG.errorf("❌ EMAIL SENDING FAILED");
            LOG.errorf("  To: %s", userEmail);
            LOG.errorf("  Query ID: %d", request.getQueryId());
            LOG.errorf("  Error: %s", e.getMessage());
            LOG.errorf("  Exception type: %s", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                LOG.errorf("  Caused by: %s - %s", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            // Lanciamo l'eccezione per permettere al NotificationAdapter di gestirla
            throw new RuntimeException("Failed to send email notification: " + e.getMessage(), e);
        }
    }
    
    private String buildHtmlEmailBody(NotificationRequest request) {
        // Se la risposta AI contiene già HTML, usala direttamente
        if (request.getAiResponse().trim().startsWith("<") && request.getAiResponse().contains("</")) {
            LOG.infof("📧 AI response contains HTML, using it directly");
            return request.getAiResponse();
        }
        
        // Altrimenti, crea un template HTML semplice
        LOG.infof("📧 AI response is plain text, wrapping in HTML template");
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Notificamy Notification</title>
                    <style>
                        body { font-family: "Segoe UI", Arial, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 20px; background: #f4f4f8; }
                        .container { max-width: 600px; margin: 0 auto; background: white; border-radius: 12px; box-shadow: 0 3px 8px rgba(0,0,0,.08); overflow: hidden; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px 20px; text-align: center; }
                        .header h1 { margin: 0; font-size: 24px; }
                        .content { padding: 30px 20px; }
                        .prompt-box { background: #f8f9fa; padding: 20px; border-left: 4px solid #667eea; margin: 20px 0; border-radius: 4px; }
                        .response-box { background: #f8f9fa; padding: 20px; border-left: 4px solid #764ba2; margin: 20px 0; border-radius: 4px; }
                        .footer { text-align: center; padding: 20px; color: #666; font-size: 14px; background: #f8f9fa; }
                        h2 { color: #333; margin-top: 0; }
                        .ai-response { white-space: pre-wrap; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔔 Notificamy</h1>
                            <p>Your AI-Powered Notification</p>
                        </div>
                        <div class="content">
                            <h2>Hello %s!</h2>
                            <p>Your notification request has been processed by our AI assistant.</p>
                            
                            <div class="prompt-box">
                                <h3>📝 Your Request:</h3>
                                <p><em>"%s"</em></p>
                            </div>
                            
                            <div class="response-box">
                                <h3>🤖 AI Response:</h3>
                                <div class="ai-response">%s</div>
                            </div>
                            
                            <p>Thank you for using Notificamy!</p>
                        </div>
                        <div class="footer">
                            <p>© 2024 Notificamy. Revolutionizing notifications with AI.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, 
                request.getUser().getName() != null ? request.getUser().getName() : "User", 
                request.getPrompt(), 
                request.getAiResponse().replace("<", "&lt;").replace(">", "&gt;"));
    }
}