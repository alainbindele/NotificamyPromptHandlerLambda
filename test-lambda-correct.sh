#!/bin/bash

# Test Lambda con il metodo CORRETTO per evitare problemi di encoding

echo "🎯 Test Lambda - Metodo CORRETTO"

FUNCTION_NAME="NotificamyNotifierLambda"
REGION="eu-south-1"

# Metodo 1: Usa --cli-input-json (RACCOMANDATO)
echo "📋 Metodo 1: CLI Input JSON..."
cat > lambda-input.json << 'EOF'
{
  "FunctionName": "NotificamyNotifierLambda",
  "Payload": "{\"Records\":[{\"body\":\"{\\\"query_id\\\":1,\\\"prompt\\\":\\\"Test message\\\",\\\"user_email\\\":\\\"test@example.com\\\"}\"}]}"
}
EOF

aws lambda invoke \
  --region $REGION \
  --cli-input-json file://lambda-input.json \
  response1.json

echo "📋 Response metodo 1:"
cat response1.json
echo ""

# Metodo 2: Payload esterno con encoding corretto
echo "📋 Metodo 2: File payload esterno..."
cat > event.json << 'EOF'
{
  "Records": [
    {
      "messageId": "test-message-1",
      "receiptHandle": "test-receipt-handle",
      "body": "{\"query_id\":1,\"prompt\":\"Test notification\",\"user_email\":\"test@example.com\",\"user_phone\":\"+1234567890\"}",
      "attributes": {},
      "messageAttributes": {},
      "md5OfBody": "test-md5",
      "eventSource": "aws:sqs",
      "eventSourceARN": "arn:aws:sqs:eu-south-1:123456789012:test-queue",
      "awsRegion": "eu-south-1"
    }
  ]
}
EOF

aws lambda invoke \
  --function-name $FUNCTION_NAME \
  --region $REGION \
  --payload file://event.json \
  response2.json

echo "📋 Response metodo 2:"
cat response2.json
echo ""

# Metodo 3: Test con AWS CLI v2 syntax
echo "📋 Metodo 3: AWS CLI v2 syntax..."
aws lambda invoke \
  --function-name $FUNCTION_NAME \
  --region $REGION \
  --cli-binary-format raw-in-base64-out \
  --payload file://event.json \
  response3.json

echo "📋 Response metodo 3:"
cat response3.json
echo ""

# Verifica log Lambda
echo "📋 Controllo log Lambda..."
sleep 5

LOG_GROUP="/aws/lambda/$FUNCTION_NAME"
LATEST_STREAM=$(aws logs describe-log-streams \
  --log-group-name "$LOG_GROUP" \
  --region $REGION \
  --order-by LastEventTime \
  --descending \
  --max-items 1 \
  --query 'logStreams[0].logStreamName' \
  --output text 2>/dev/null)

if [ "$LATEST_STREAM" != "None" ] && [ -n "$LATEST_STREAM" ]; then
  echo "📋 Ultimi log:"
  aws logs get-log-events \
    --log-group-name "$LOG_GROUP" \
    --log-stream-name "$LATEST_STREAM" \
    --region $REGION \
    --start-time $(date -d '2 minutes ago' +%s)000 \
    --query 'events[*].message' \
    --output text 2>/dev/null | tail -5
fi

# Pulizia
rm -f lambda-input.json event.json response*.json

echo "✅ Test con metodi corretti completato!"