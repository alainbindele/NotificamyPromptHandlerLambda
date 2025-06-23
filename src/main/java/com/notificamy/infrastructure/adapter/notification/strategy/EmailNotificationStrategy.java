package com.notificamy.infrastructure.adapter.notification.strategy;

import com.notificamy.domain.model.NotificationRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@ApplicationScoped
public class EmailNotificationStrategy implements NotificationStrategy {
    
    private static final Logger LOG = Logger.getLogger(EmailNotificationStrategy.class);
    
    @Inject
    SesClient sesClient;
    
    @ConfigProperty(name = "app.aws.ses.from-email")
    String fromEmail;
    
    @ConfigProperty(name = "app.aws.ses.from-name")
    String fromName;
    
    @Override
    public void sendNotification(NotificationRequest request) {
        try {
            String subject = "Notificamy: Risposta AI per la tua richiesta";
            String htmlBody = buildHtmlEmailBody(request);
            String textBody = buildTextEmailBody(request);
            
            LOG.infof("Sending email to %s for query %d", request.getUser().getEmail(), request.getQueryId());
            
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .source(fromName + " <" + fromEmail + ">")
                    .destination(Destination.builder()
                            .toAddresses(request.getUser().getEmail())
                            .build())
                    .message(Message.builder()
                            .subject(Content.builder()
                                    .charset("UTF-8")
                                    .data(subject)
                                    .build())
                            .body(Body.builder()
                                    .html(Content.builder()
                                            .charset("UTF-8")
                                            .data(htmlBody)
                                            .build())
                                    .text(Content.builder()
                                            .charset("UTF-8")
                                            .data(textBody)
                                            .build())
                                    .build())
                            .build())
                    .build();
            
            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            LOG.infof("Email sent successfully to %s for query %d. Message ID: %s", 
                    request.getUser().getEmail(), request.getQueryId(), response.messageId());
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send email to %s for query %d", request.getUser().getEmail(), request.getQueryId());
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    private String buildHtmlEmailBody(NotificationRequest request) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Notificamy - Risposta AI</title>
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; line-height: 1.6; color: #333; margin: 0; padding: 0; }
                        .container { max-width: 600px; margin: 0 auto; background: #ffffff; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 30px 20px; text-align: center; }
                        .header h1 { margin: 0; font-size: 28px; font-weight: 300; }
                        .header p { margin: 10px 0 0 0; opacity: 0.9; }
                        .content { padding: 30px 20px; }
                        .greeting { font-size: 18px; margin-bottom: 25px; color: #2c3e50; }
                        .section { margin: 25px 0; }
                        .section-title { font-size: 16px; font-weight: 600; color: #34495e; margin-bottom: 10px; display: flex; align-items: center; }
                        .prompt-box { background: #f8f9fa; padding: 20px; border-left: 4px solid #667eea; border-radius: 0 8px 8px 0; margin: 15px 0; }
                        .response-box { background: #f1f8ff; padding: 20px; border-left: 4px solid #764ba2; border-radius: 0 8px 8px 0; margin: 15px 0; }
                        .timestamp { background: #e9ecef; padding: 10px 15px; border-radius: 6px; font-size: 14px; color: #6c757d; text-align: center; }
                        .footer { background: #f8f9fa; padding: 20px; text-align: center; border-top: 1px solid #dee2e6; }
                        .footer p { margin: 5px 0; color: #6c757d; font-size: 14px; }
                        .emoji { font-size: 20px; margin-right: 8px; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>🔔 Notificamy</h1>
                            <p>La tua risposta AI è pronta!</p>
                        </div>
                        <div class="content">
                            <div class="greeting">Ciao %s! 👋</div>
                            
                            <div class="section">
                                <div class="section-title">
                                    <span class="emoji">📝</span>
                                    La tua richiesta:
                                </div>
                                <div class="prompt-box">
                                    <em>"%s"</em>
                                </div>
                            </div>
                            
                            <div class="section">
                                <div class="section-title">
                                    <span class="emoji">🤖</span>
                                    Risposta dell'AI:
                                </div>
                                <div class="response-box">
                                    %s
                                </div>
                            </div>
                            
                            <div class="timestamp">
                                ⏰ Ricevuto: %s
                            </div>
                        </div>
                        <div class="footer">
                            <p><strong>Grazie per aver usato Notificamy!</strong> 🚀</p>
                            <p>© 2024 Notificamy. Notifiche intelligenti con AI.</p>
                        </div>
                    </div>
                </body>
                </html>
                """, 
                request.getUser().getName() != null ? request.getUser().getName() : "Utente", 
                request.getPrompt(), 
                request.getAiResponse().replace("\n", "<br>"),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }
    
    private String buildTextEmailBody(NotificationRequest request) {
        return String.format("""
                NOTIFICAMY - RISPOSTA AI
                
                Ciao %s!
                
                La tua richiesta:
                "%s"
                
                Risposta dell'AI:
                %s
                
                Ricevuto: %s
                
                Grazie per aver usato Notificamy!
                
                © 2024 Notificamy. Notifiche intelligenti con AI.
                """, 
                request.getUser().getName() != null ? request.getUser().getName() : "Utente", 
                request.getPrompt(), 
                request.getAiResponse(),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
    }
}