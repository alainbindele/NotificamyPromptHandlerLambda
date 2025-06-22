# Notificamy Lambda Processor - Hexagonal Architecture with AWS Secrets Manager

AWS Lambda function built with Quarkus using Hexagonal Architecture that processes notification requests from SQS, integrates with ChatGPT, and sends notifications via multiple channels (Email, WhatsApp, Slack, Discord).

## 🔐 Security-First Architecture

This project implements a **security-first approach** using:
- **AWS Secrets Manager** for sensitive data (API keys, database credentials)
- **GitHub Secrets** for deployment configuration
- **IAM roles** with least-privilege access
- **Encrypted secrets** at rest and in transit

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
- **Configuration Services**: Secure secret management
  - `SecretsManagerService` - AWS Secrets Manager integration
  - `DatabaseConfigService` - Database configuration from secrets
  - `ApiKeysConfigService` - API keys from secrets
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

## 🔐 Secrets Management

### AWS Secrets Manager
Sensitive data is stored in AWS Secrets Manager:

**Database Credentials** (`notificamy/database-credentials`):
```json
{
  "DB_URL": "jdbc:mysql://your-rds-endpoint:3306/notificamy",
  "DB_USER": "notificamy_user", 
  "DB_PASSWORD": "your-secure-password"
}
```

**API Keys** (`notificamy/api-keys`):
```json
{
  "OPENAI_API_KEY": "sk-your-openai-api-key",
  "WHATSAPP_API_TOKEN": "your-whatsapp-business-api-token"
}
```

### GitHub Secrets
Non-sensitive configuration stored in GitHub Secrets:
- `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`
- `DATABASE_SECRET_NAME` / `API_KEYS_SECRET_NAME`
- `AWS_SQS_QUEUE_URL`
- `AWS_SES_FROM_EMAIL` / `AWS_SES_FROM_NAME`
- `WHATSAPP_API_URL`

See [SECRETS_SETUP.md](docs/SECRETS_SETUP.md) for detailed setup instructions.

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

### Deployment via GitHub Actions
The project includes a complete CI/CD pipeline that:
1. Builds the application with Maven
2. Creates and pushes Docker image to ECR
3. Updates Lambda function configuration
4. Deploys the new version

## Channel Setup Instructions

### WhatsApp Business API
1. Create a Meta Business account
2. Set up WhatsApp Business API
3. Get your Phone Number ID and Access Token
4. Store access token in AWS Secrets Manager
5. Store API URL in GitHub Secrets

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

## Security Features

1. **No Hardcoded Secrets**: All sensitive data in AWS Secrets Manager
2. **Least Privilege IAM**: Lambda role has minimal required permissions
3. **Encrypted Storage**: Secrets encrypted at rest and in transit
4. **Audit Logging**: All secret access logged via CloudTrail
5. **Secret Rotation**: Support for automatic secret rotation
6. **Caching**: Secrets cached during Lambda execution for performance

## Benefits of This Architecture

1. **Security**: Secrets never exposed in code or logs
2. **Testability**: Each component can be tested in isolation
3. **Maintainability**: Clear separation of concerns
4. **Extensibility**: Easy to add new notification channels
5. **Flexibility**: Users can choose any combination of channels
6. **Fault Tolerance**: Channel failures don't affect others
7. **Performance**: Parallel notification sending possible
8. **Clean Code**: Domain logic separated from infrastructure concerns
9. **Secret Management**: Centralized, secure secret handling

## MapStruct Integration

The project uses MapStruct for clean object mapping between layers:
- External DTOs → Domain Models
- Domain Models → Persistence Entities
- Automatic JSON serialization for complex fields

This ensures type safety and eliminates boilerplate mapping code while maintaining clear boundaries between architectural layers.