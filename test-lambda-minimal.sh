#!/bin/bash

echo "🧪 Testing MINIMAL Lambda function..."

# Create simple test payload
cat > test-payload.json << 'EOF'
{
  "Records": [
    {
      "body": "{\"query_id\":1,\"prompt\":\"Test message\",\"user_email\":\"test@example.com\"}"
    }
  ]
}
EOF

echo "📋 Test payload created:"
cat test-payload.json

echo ""
echo "🚀 Invoking Lambda function..."

aws lambda invoke \
  --function-name NotificamyNotifierLambda \
  --payload file://test-payload.json \
  --cli-binary-format raw-in-base64-out \
  response.json

echo ""
echo "📋 Lambda response:"
cat response.json

echo ""
echo "📋 CloudWatch logs (last 5 minutes):"
aws logs filter-log-events \
  --log-group-name /aws/lambda/NotificamyNotifierLambda \
  --start-time $(date -d '5 minutes ago' +%s)000 \
  --query 'events[*].message' \
  --output text

echo ""
echo "✅ Test completed!"