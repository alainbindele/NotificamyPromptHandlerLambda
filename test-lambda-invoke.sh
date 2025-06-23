#!/bin/bash

# Script per testare l'invocazione Lambda con payload corretto

echo "🧪 Test invocazione Lambda con payload corretto..."

# 1. Crea payload di test in file separato (evita problemi di escaping)
cat > lambda-test-payload.json << 'EOF'
{
  "Records": [
    {
      "body": "{\"query_id\":1,\"prompt\":\"Test message\",\"user_email\":\"test@example.com\",\"user_phone\":\"+1234567890\"}"
    }
  ]
}
EOF

echo "📋 Payload creato:"
cat lambda-test-payload.json

# 2. Test con file payload
echo "🚀 Invocazione Lambda con file payload..."
aws lambda invoke \
  --function-name NotificamyNotifierLambda \
  --region eu-south-1 \
  --payload file://lambda-test-payload.json \
  --cli-binary-format raw-in-base64-out \
  response.json

# 3. Mostra risultato
echo "📋 Status code: $?"
echo "📋 Response file:"
if [ -f response.json ]; then
    cat response.json
    echo ""
else
    echo "❌ File response.json non creato"
fi

# 4. Test alternativo con payload semplificato
echo ""
echo "🧪 Test alternativo con payload semplificato..."

cat > simple-payload.json << 'EOF'
{
  "Records": [
    {
      "body": "{\"query_id\":1,\"prompt\":\"Hello\",\"user_email\":\"test@example.com\"}"
    }
  ]
}
EOF

aws lambda invoke \
  --function-name NotificamyNotifierLambda \
  --region eu-south-1 \
  --payload file://simple-payload.json \
  --cli-binary-format raw-in-base64-out \
  response2.json

echo "📋 Response semplificata:"
if [ -f response2.json ]; then
    cat response2.json
    echo ""
fi

# 5. Test con payload vuoto per verificare che Lambda risponda
echo ""
echo "🧪 Test con payload vuoto..."
aws lambda invoke \
  --function-name NotificamyNotifierLambda \
  --region eu-south-1 \
  --payload '{}' \
  --cli-binary-format raw-in-base64-out \
  response3.json

echo "📋 Response payload vuoto:"
if [ -f response3.json ]; then
    cat response3.json
    echo ""
fi

# Pulizia
rm -f lambda-test-payload.json simple-payload.json response.json response2.json response3.json

echo "✅ Test invocazione completati!"