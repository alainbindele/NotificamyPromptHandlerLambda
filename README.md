# Notificamy Lambda Processor - MINIMAL VERSION

AWS Lambda function that processes SQS messages. **MINIMAL SETUP** - only essential code.

## 🎯 What This Does

1. **Receives SQS messages** with notification requests
2. **Processes them** (currently just logs and simulates)
3. **Returns success/error count**

## 🚀 Quick Deploy

1. **Push to GitHub** - the workflow will automatically:
   - Build the JAR
   - Create Docker image
   - Deploy to AWS Lambda

2. **Test the Lambda**:
```bash
chmod +x test-lambda-minimal.sh
./test-lambda-minimal.sh
```

## 📦 What's Included

- **Minimal Lambda Handler** - No CDI, no Quarkus, just pure Java
- **GitHub Actions Workflow** - Automatic build and deploy
- **Docker Setup** - Lambda-optimized container
- **Test Script** - Simple Lambda testing

## 🔧 Required GitHub Secrets

```
AWS_ACCESS_KEY_ID=your-access-key
AWS_SECRET_ACCESS_KEY=your-secret-key
MY_GITHUB_USERNAME=your-github-username
MY_GITHUB_TOKEN=your-github-token
```

## 📨 SQS Message Format

```json
{
  "Records": [
    {
      "body": "{\"query_id\":1,\"prompt\":\"Test\",\"user_email\":\"test@example.com\"}"
    }
  ]
}
```

## ✅ Benefits

- **MINIMAL** - Only 4 dependencies
- **FAST** - No framework overhead
- **RELIABLE** - Simple code, fewer failure points
- **EASY** - Just push to deploy

That's it! 🎉