#!/bin/bash

# Script completo per debug Lambda deployment

echo "🔧 Debug completo deployment Lambda..."

FUNCTION_NAME="NotificamyNotifierLambda"
REGION="eu-south-1"

# 1. Verifica stato Lambda
echo "📋 Verifica stato Lambda..."
if aws lambda get-function --function-name $FUNCTION_NAME --region $REGION >/dev/null 2>&1; then
    echo "✅ Lambda function esiste"
    
    # Mostra configurazione
    aws lambda get-function-configuration \
        --function-name $FUNCTION_NAME \
        --region $REGION \
        --query '[FunctionName,State,LastUpdateStatus,CodeSha256,PackageType,Architectures[0]]' \
        --output table
else
    echo "❌ Lambda function non trovata!"
    exit 1
fi

# 2. Verifica immagine ECR
echo "📋 Verifica immagine ECR..."
aws ecr describe-images \
    --repository-name notificamy \
    --region $REGION \
    --query 'imageDetails[0].[imageTags[0],imagePushedAt,imageSizeInBytes,imageManifestMediaType]' \
    --output table

# 3. Test invocazione con payload corretto
echo "🧪 Test invocazione Lambda..."

# Crea payload di test
cat > test-payload.json << 'EOF'
{
  "Records": [
    {
      "body": "{\"query_id\":1,\"prompt\":\"Test\",\"user_email\":\"test@example.com\"}"
    }
  ]
}
EOF

# Invoca Lambda
aws lambda invoke \
    --function-name $FUNCTION_NAME \
    --region $REGION \
    --payload file://test-payload.json \
    --cli-binary-format raw-in-base64-out \
    response.json

echo "📋 Response:"
if [ -f response.json ]; then
    cat response.json
    echo ""
    
    # Controlla se c'è un errore
    if grep -q "errorMessage" response.json; then
        echo "❌ Errore nella Lambda!"
        echo "🔍 Dettagli errore:"
        cat response.json | jq -r '.errorMessage' 2>/dev/null || cat response.json
    else
        echo "✅ Lambda eseguita senza errori!"
    fi
else
    echo "❌ Nessuna response ricevuta"
fi

# 4. Controlla log
echo ""
echo "📋 Controllo log Lambda..."
LOG_GROUP="/aws/lambda/$FUNCTION_NAME"

# Aspetta un po' per i log
sleep 5

if aws logs describe-log-groups --log-group-name-prefix "$LOG_GROUP" --region $REGION >/dev/null 2>&1; then
    # Ottieni il log stream più recente
    LATEST_STREAM=$(aws logs describe-log-streams \
        --log-group-name "$LOG_GROUP" \
        --region $REGION \
        --order-by LastEventTime \
        --descending \
        --max-items 1 \
        --query 'logStreams[0].logStreamName' \
        --output text)
    
    if [ "$LATEST_STREAM" != "None" ] && [ -n "$LATEST_STREAM" ]; then
        echo "📋 Ultimi log (stream: $LATEST_STREAM):"
        aws logs get-log-events \
            --log-group-name "$LOG_GROUP" \
            --log-stream-name "$LATEST_STREAM" \
            --region $REGION \
            --start-time $(date -d '5 minutes ago' +%s)000 \
            --query 'events[*].message' \
            --output text | tail -10
    fi
fi

# 5. Test payload vuoto
echo ""
echo "🧪 Test con payload vuoto..."
aws lambda invoke \
    --function-name $FUNCTION_NAME \
    --region $REGION \
    --payload '{}' \
    --cli-binary-format raw-in-base64-out \
    empty-response.json

echo "📋 Response payload vuoto:"
cat empty-response.json 2>/dev/null || echo "Nessuna response"

# Pulizia
rm -f test-payload.json response.json empty-response.json

echo ""
echo "🎯 RIEPILOGO DEBUG:"
echo "=================="
echo "1. ✅ Lambda function verificata"
echo "2. ✅ Immagine ECR verificata"
echo "3. 🧪 Test invocazione eseguito"
echo "4. 📋 Log controllati"
echo ""
echo "💡 Se ci sono ancora errori:"
echo "   - Controlla i log CloudWatch per dettagli"
echo "   - Verifica che i secrets AWS esistano"
echo "   - Controlla le policy IAM"
echo "   - Verifica la configurazione dell'immagine Docker"

echo "✅ Debug completo terminato!"