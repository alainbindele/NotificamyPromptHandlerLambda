# Notificamy Lambda Processor

AWS Lambda function built with Quarkus that processes notification requests from SQS, integrates with ChatGPT, and sends emails via AWS SES.

## Architecture

1. **SQS Trigger**: Lambda is triggered by SQS messages
2. **Database Query**: Fetches query and user data from MySQL
3. **AI Processing**: Sends prompt to ChatGPT for intelligent response
4. **Email Delivery**: Formats and sends notification email via AWS SES

## Prerequisites

- Java 17+
- Maven 3.8+
- AWS CLI configured
- MySQL database with required tables
- OpenAI API key
- AWS SES verified sender email

## Database Setup

Create the required tables in your MySQL database:

```sql
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `queries` (
  `id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `prompt` text NOT NULL,
  `is_valid` tinyint(1) DEFAULT '0',
  `cron_params` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `next_execution` timestamp NULL DEFAULT NULL,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `queries_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

## Environment Variables

Set the following environment variables:

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

## Building

### JVM Mode
```bash
mvn clean package
```

### Native Mode (for better cold start performance)
```bash
mvn clean package -Pnative
```

## Deployment

### Create Lambda Function
```bash
aws lambda create-function \
  --function-name notificamy-processor \
  --runtime java17 \
  --role arn:aws:iam::account:role/lambda-execution-role \
  --handler io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler \
  --zip-file fileb://target/function.zip \
  --timeout 300 \
  --memory-size 512
```

### Update Function Code
```bash
aws lambda update-function-code \
  --function-name notificamy-processor \
  --zip-file fileb://target/function.zip
```

### Configure SQS Trigger
```bash
aws lambda create-event-source-mapping \
  --function-name notificamy-processor \
  --event-source-arn arn:aws:sqs:region:account:queue-name \
  --batch-size 10
```

## SQS Message Format

The Lambda expects SQS messages in this format:

```json
{
  "query_id": 123,
  "prompt": "Remind me about my daily standup meeting"
}
```

## IAM Permissions

The Lambda execution role needs these permissions:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:*:*:your-queue-name"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    }
  ]
}
```

## Testing Locally

Run in development mode:
```bash
mvn quarkus:dev
```

## Monitoring

- Check CloudWatch Logs for function execution logs
- Monitor SQS queue metrics for message processing
- Set up CloudWatch alarms for error rates and duration

## Features

- ✅ SQS message processing
- ✅ MySQL database integration with JPA/Hibernate
- ✅ ChatGPT API integration
- ✅ HTML and text email formatting
- ✅ AWS SES email delivery
- ✅ Comprehensive error handling and logging
- ✅ Native compilation support for better performance
- ✅ Production-ready configuration

## Architecture Benefits

- **Serverless**: Pay only for execution time
- **Scalable**: Automatically scales with SQS message volume
- **Resilient**: Built-in retry mechanisms and dead letter queues
- **Cost-effective**: Quarkus native compilation reduces cold start times
- **Maintainable**: Clean separation of concerns with service layers