package com.notificamy.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;

@ApplicationScoped
public class EmailService {
    
    private static final Logger LOG = Logger.getLogger(EmailService.class);
    
    @Inject
    SesClient sesClient;
    
    @ConfigProperty(name = "app.aws.ses.from-email")
    String fromEmail;
    
    @ConfigProperty(name = "app.aws.ses.from-name")
    String fromName;
    
    public void sendNotificationEmail(String toEmail, String userName, String prompt, String aiResponse) {
        try {
            String subject = "Notificamy: Your AI-Generated Notification";
            String htmlBody = buildHtmlEmailBody(userName, prompt, aiResponse);
            String textBody = buildTextEmailBody(userName, prompt, aiResponse);
            
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .source(fromName + " <" + fromEmail + ">")
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
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
            LOG.infof("Email sent successfully to %s. Message ID: %s", toEmail, response.messageId());
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to send email to %s", toEmail);
            throw new RuntimeException("Failed to send email", e);
        }
    }
    
    private String buildHtmlEmailBody(String userName, String prompt, String aiResponse) {
        return String.format("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>Notificamy Notification</title>
                    <style>
                        body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                        .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                        .header { background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); color: white; padding: 20px; border-radius: 8px 8px 0 0; }
                        .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 8px 8px; }
                        .prompt-box { background: white; padding: 15px; border-left: 4px solid #667eea; margin: 15px 0; }
                        .response-box { background: white; padding: 15px; border-left: 4px solid #764ba2; margin: 15px 0; }
                        .footer { text-align: center; margin-top: 20px; color: #666; font-size: 12px; }
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
                                <p>%s</p>
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
                userName != null ? userName : "User", 
                prompt, 
                aiResponse.replace("\n", "<br>"));
    }
    
    private String buildTextEmailBody(String userName, String prompt, String aiResponse) {
        return String.format("""
                Notificamy - Your AI-Powered Notification
                
                Hello %s!
                
                Your notification request has been processed by our AI assistant.
                
                Your Request:
                "%s"
                
                AI Response:
                %s
                
                Thank you for using Notificamy!
                
                © 2024 Notificamy. Revolutionizing notifications with AI.
                """, 
                userName != null ? userName : "User", 
                prompt, 
                aiResponse);
    }
}