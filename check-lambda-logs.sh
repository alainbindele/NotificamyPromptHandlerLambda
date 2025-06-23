#!/bin/bash

# Script per controllare i log Lambda

echo "📋 Controllo log Lambda..."

FUNCTION_NAME="NotificamyNotifierLambda"
REGION="eu-south-1"

# 1. Ottieni il log group
LOG_GROUP="/aws/lambda/$FUNCTION_NAME"

echo "🔍 Verifica esistenza log group: $LOG_GROUP"
if aws logs describe-log-groups --log-group-name-prefix "$LOG_GROUP" --region $REGION >/dev/null 2>&1; then
    echo "✅ Log group trovato"
    
    # 2. Ottieni gli stream più recenti
    echo "📋 Log streams più recenti:"
    aws logs describe-log-streams \
        --log-group-name "$LOG_GROUP" \
        --region $REGION \
        --order-by LastEventTime \
        --descending \
        --max-items 5 \
        --query 'logStreams[*].[logStreamName,lastEventTime,lastIngestionTime]' \
        --output table
    
    # 3. Ottieni il log stream più recente
    LATEST_STREAM=$(aws logs describe-log-streams \
        --log-group-name "$LOG_GROUP" \
        --region $REGION \
        --order-by LastEventTime \
        --descending \
        --max-items 1 \
        --query 'logStreams[0].logStreamName' \
        --output text)
    
    if [ "$LATEST_STREAM" != "None" ] && [ -n "$LATEST_STREAM" ]; then
        echo "📋 Log stream più recente: $LATEST_STREAM"
        
        # 4. Mostra gli ultimi log
        echo "📋 Ultimi log eventi:"
        aws logs get-log-events \
            --log-group-name "$LOG_GROUP" \
            --log-stream-name "$LATEST_STREAM" \
            --region $REGION \
            --start-from-head \
            --query 'events[*].[timestamp,message]' \
            --output text | tail -20
    else
        echo "⚠️ Nessun log stream trovato"
    fi
    
else
    echo "❌ Log group non trovato. La Lambda potrebbe non essere stata mai eseguita."
fi

# 5. Verifica configurazione Lambda
echo ""
echo "📋 Configurazione Lambda attuale:"
aws lambda get-function-configuration \
    --function-name $FUNCTION_NAME \
    --region $REGION \
    --query '[FunctionName,State,LastUpdateStatus,Runtime,Handler,Timeout,MemorySize]' \
    --output table

echo "✅ Controllo log completato!"