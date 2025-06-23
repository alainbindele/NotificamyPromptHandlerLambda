# Secrets Management Setup Guide

## Overview
This project uses a hybrid approach for secrets management:
- **AWS Secrets Manager**: For sensitive data (API keys, database credentials)
- **GitHub Secrets**: For deployment configuration and non-sensitive environment variables

## 🔐 AWS Secrets Manager Setup

### 1. Database Credentials Secret
Create a secret named: `notificamy/database-credentials`

```json
{
  "DB_URL": "jdbc:mysql://your-rds-endpoint:3306/notificamy",
  "DB_USER": "notificamy_user",
  "DB_PASSWORD": "your-secure-password"
}
```

### 2. API Keys Secret
Create a secret named: `notificamy/api-keys`

```json
{
  "OPENAI_API_KEY": "sk-your-openai-api-key",
  "WHATSAPP_API_TOKEN": "your-whatsapp-business-api-token"
}
```

### 3. IAM Permissions
Your Lambda execution role needs these permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": "secretsmanager:GetSecretValue",
      "Resource": [
        "arn:aws:secretsmanager:eu-south-1:YOUR-ACCOUNT-ID:secret:notificamy/database-credentials-*",
        "arn:aws:secretsmanager:eu-south-1:YOUR-ACCOUNT-ID:secret:notificamy/api-keys-*"
      ]
    }
  ]
}
```

## 🔧 GitHub Secrets Setup

Go to: **GitHub Repository → Settings → Secrets and variables → Actions**

### Required Secrets:

| Secret Name | Description | Example |
|-------------|-------------|---------|
| `AWS_ACCESS_KEY_ID` | IAM user access key for deployment | `AKIAIOSFODNN7EXAMPLE` |
| `AWS_SECRET_ACCESS_KEY` | IAM user secret key | `wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY` |
| `MY_GITHUB_USERNAME` | GitHub username for Maven packages | `your-username` |
| `MY_GITHUB_TOKEN` | GitHub personal access token | `ghp_xxxxxxxxxxxx` |
| `DATABASE_SECRET_NAME` | AWS Secrets Manager secret name | `notificamy/database-credentials` |
| `API_KEYS_SECRET_NAME` | AWS Secrets Manager secret name | `notificamy/api-keys` |
| `AWS_SQS_QUEUE_URL` | SQS queue URL | `https://sqs.eu-south-1.amazonaws.com/123456789012/notificamy-queue` |
| `AWS_SES_FROM_EMAIL` | Verified SES sender email | `noreply@yourdomain.com` |
| `AWS_SES_FROM_NAME` | Email sender name | `Notificamy` |
| `WHATSAPP_API_URL` | WhatsApp Business API URL | `https://graph.facebook.com/v18.0/YOUR_PHONE_NUMBER_ID/messages` |

## 🚀 Benefits of This Approach

### AWS Secrets Manager:
- ✅ Automatic rotation support
- ✅ Encryption at rest and in transit
- ✅ Fine-grained access control
- ✅ Audit logging
- ✅ No secrets in code or environment variables

### GitHub Secrets:
- ✅ Simple deployment configuration
- ✅ No sensitive data exposure in logs
- ✅ Easy to manage per repository

## 🔄 Secret Rotation

When rotating secrets in AWS Secrets Manager:
1. Update the secret value in AWS Console
2. The Lambda will automatically use the new value on next execution
3. No code deployment required!

## 🧪 Testing Locally

For local development, you can use environment variables:
```bash
export AWS_REGION=eu-south-1
export DATABASE_SECRET_NAME=notificamy/database-credentials
export API_KEYS_SECRET_NAME=notificamy/api-keys
```

Make sure your local AWS credentials have access to Secrets Manager.