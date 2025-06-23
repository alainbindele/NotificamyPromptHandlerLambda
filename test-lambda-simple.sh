#!/bin/bash

# Script per testare Lambda con encoding corretto

echo "🧪 Test Lambda con encoding corretto..."

FUNCTION_NAME="NotificamyNotifierLambda"
REGION="eu-south-1"

# 1. Test con payload file (metodo più sicuro)
echo "📋 Test 1: Payload da file..."
cat > payload.json << 'EOF'
{
  "Records": [
    {
      "body": "{\"query_id\":1,\"prompt\":\"Test message\",\"user_email\":\"test@example.com\"}"
    }
  ]
}
EOF

echo "📋 Contenuto payload:"
cat payload.json

echo "🚀 Invocazione con file..."
aws lambda invoke \
  --function-name $FUNCTION_NAME \
  --region $REGION \
  --payload file://payload.json \
  --cli-binary-format raw-in-base64-out \
  response1.json

echo "📋 Response 1:"
cat response1.json 2>/dev/null || echo "Nessuna response"
echo ""

# 2. Test con payload base64 encoded
echo "📋 Test 2: Payload base64..."
PAYLOAD_B64=$(echo '{"Records":[{"body":"{\"query_id\":1,\"prompt\":\"Test\",\"user_email\":\"test@example.com\"}"}]}' | base64)

aws lambda invoke \
  --function-name $FUNCTION_NAME \
  --region $REGION \
  --payload "$PAYLOAD_B64" \
  --cli-binary-format base64 \
  response2.json

echo "📋 Response 2:"
cat response2.json 2>/dev/null || echo "Nessuna response"
echo ""

# 3. Test con payload semplificato (senza escape)
echo "📋 Test 3: Payload semplificato..."
cat > simple.json << 'EOF'
{
  "Records": [
    {
      "body": "test message"
    }
  ]
}
EOF

aws lambda invoke \
  --function-name $FUNCTION_NAME \
  --region $REGION \
  --payload file://simple.json \
  --cli-binary-format raw-in-base64-out \
  response3.json

echo "📋 Response 3:"
cat response3.json 2>/dev/null || echo "Nessuna response"
echo ""

# 4. Test payload vuoto
echo "📋 Test 4: Payload vuoto..."
aws lambda invoke \
  --function-name $FUNCTION_NAME \
  --region $REGION \
  --payload '{}' \
  --cli-binary-format raw-in-base64-out \
  response4.json

echo "📋 Response 4:"
cat response4.json 2>/dev/null || echo "Nessuna response"
echo ""

# Pulizia
rm -f payload.json simple.json response*.json

echo "✅ Test completati!"