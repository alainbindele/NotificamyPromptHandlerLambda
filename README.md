# Notificamy Lambda Processor - Hexagonal Architecture

AWS Lambda function built with Quarkus using Hexagonal Architecture that processes notification requests from SQS, integrates with ChatGPT, and sends notifications via multiple channels (Email, WhatsApp, Slack, Discord).

## Architecture Overview

This project follows **Hexagonal Architecture** (Ports and Adapters) principles:

### Core Domain
- **Domain Models**: `NotificationRequest`, `User`, `Query`, `NotificationChannel`
- **Domain Services**: `NotificationService` - contains business logic
- **Ports**: Interfaces defining contracts (`NotificationPort`, `QueryRepositoryPort`, `AiServicePort`)

### Infrastructure Layer
- **Adapters**: Implementations of ports
  - `NotificationAdapter` - Multi-channel notification orchestrator
  - `QueryRepositoryAdapter` - Database access
  - `ChatGptAdapter` - AI service integration
- **Notification Strategies**: Channel-specific implementations
  - `EmailNotificationStrategy` - AWS SES
  - `WhatsAppNotificationStrategy` - WhatsApp Business API
  - `SlackNotificationStrategy` - Slack Webhooks
  - `DiscordNotificationStrategy` - Discord Webhooks

### Design Patterns Used

#### 1. Decorator Pattern
The notification system uses the **Decorator Pattern** to chain multiple notification channels:
```java
NotificationDecorator chain = new NotificationDecorator(emailStrategy, 
    new NotificationDecorator(slackStrategy, 
        new NotificationDecorator(whatsappStrategy, null)));
```

**Why Decorator Pattern?**
- ✅ **Flexible Composition**: Users can enable any combination of channels
- ✅ **Runtime Configuration**: Channels can be added/removed dynamically
- ✅ **Fault Tolerance**: If one channel fails, others continue working
- ✅ **Single Responsibility**: Each strategy handles one channel
- ✅ **Open/Closed Principle**: Easy to add new channels without modifying existing code

#### 2. Strategy Pattern
Each notification channel is implemented as a separate strategy, allowing for:
- Different message formats per channel
- Channel-specific error handling
- Independent configuration and testing

#### 3. Adapter Pattern
External services (database, AI, notifications) are wrapped in adapters that implement domain ports.

## Notification Channels

### 1. Email (AWS SES)
- Rich HTML formatting with embedded CSS
- Fallback plain text version
- Professional email templates

### 2. WhatsApp (Business API)
- Formatted messages with emojis
- Requires WhatsApp Business API setup
- Phone number validation

### 3. Slack
- Rich block-based formatting
- Webhook integration
- Professional workspace notifications

### 4. Discord
- Embedded messages with colors
- Webhook integration
- Gaming/community focused formatting

## Database Schema Updates

Add these columns to your existing `users` table:

```sql
ALTER TABLE users 
ADD COLUMN whatsapp_phone VARCHAR(20),
ADD COLUMN slack_webhook VARCHAR(500),
ADD COLUMN discord_webhook VARCHAR(500);

ALTER TABLE queries 
ADD COLUMN enabled_channels JSON DEFAULT '["EMAIL"]';
```

## Environment Variables

### Core Configuration
```bash
export OPENAI_API_KEY=your-openai-api-key
export AWS_REGION=us-east-1
export AWS_SQS_QUEUE_URL=https://sqs.region.amazonaws.com/account/queue-name
export AWS_SES_FROM_EMAIL=noreply@yourdomain.com
export AWS_SES_FROM_NAME="Notificamy"
export DB_HOST=localhost
export DB_PORT=3306
export DB_USERNAME=root
export DB_PASSWORD=your-password
export DB_NAME=notificamy
```

### WhatsApp Configuration
```bash
export WHATSAPP_API_URL=https://graph.facebook.com/v18.0/YOUR_PHONE_NUMBER_ID/messages
export WHATSAPP_API_TOKEN=your-whatsapp-business-api-token
```

## SQS Message Format

```json
{
  "query_id": 123,
  "prompt": "Remind me about my daily standup meeting"
}
```

The system will automatically determine which channels to use based on the query's `enabled_channels` configuration.

## Building and Deployment

### JVM Mode
```bash
mvn clean package
```

### Native Mode (Recommended for Lambda)
```bash
mvn clean package -Pnative
```

## Channel Setup Instructions

### WhatsApp Business API
1. Create a Meta Business account
2. Set up WhatsApp Business API
3. Get your Phone Number ID and Access Token
4. Configure webhook URLs for message status

### Slack Integration
1. Create a Slack App in your workspace
2. Enable Incoming Webhooks
3. Create webhook URL for your channel
4. Store webhook URL in user's `slack_webhook` field

### Discord Integration
1. Go to your Discord server settings
2. Navigate to Integrations → Webhooks
3. Create a new webhook
4. Copy webhook URL to user's `discord_webhook` field

## Testing

Each notification strategy can be tested independently:

```java
@Test
public void testEmailNotification() {
    // Test email strategy
}

@Test
public void testMultiChannelNotification() {
    // Test decorator chain
}
```

## Benefits of This Architecture

1. **Testability**: Each component can be tested in isolation
2. **Maintainability**: Clear separation of concerns
3. **Extensibility**: Easy to add new notification channels
4. **Flexibility**: Users can choose any combination of channels
5. **Fault Tolerance**: Channel failures don't affect others
6. **Performance**: Parallel notification sending possible
7. **Clean Code**: Domain logic separated from infrastructure concerns

## MapStruct Integration

The project uses MapStruct for clean object mapping between layers:
- External DTOs → Domain Models
- Domain Models → Persistence Entities
- Automatic JSON serialization for complex fields

This ensures type safety and eliminates boilerplate mapping code while maintaining clear boundaries between architectural layers.