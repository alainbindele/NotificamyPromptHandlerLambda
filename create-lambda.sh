#!/bin/bash

# Script per creare la Lambda function con la configurazione corretta

echo "🚀 Creazione Lambda function NotificamyNotifierLambda..."

# Verifica che l'immagine esista su ECR
echo "📋 Verifica immagine su ECR..."
aws ecr describe-images \
  --repository-name notificamy \
  --region eu-south-1 \
  --query 'imageDetails[0].imageTags[0]' \
  --output text

if [ $? -ne 0 ]; then
  echo "❌ Immagine non trovata su ECR. Esegui prima il push dell'immagine."
  exit 1
fi

# Crea la Lambda function
echo "🔧 Creazione Lambda function..."
aws lambda create-function \
  --function-name NotificamyNotifierLambda \
  --package-type Image \
  --code ImageUri=435703062953.dkr.ecr.eu-south-1.amazonaws.com/notificamy:latest \
  --role arn:aws:iam::435703062953:role/lambda-execution-role \
  --architectures x86_64 \
  --timeout 300 \
  --memory-size 1024 \
  --region eu-south-1 \
  --description "Notificamy notification processor with multi-channel support"

if [ $? -eq 0 ]; then
  echo "✅ Lambda function creata con successo!"
  
  # Configura le variabili d'ambiente
  echo "🔧 Configurazione variabili d'ambiente..."
  aws lambda update-function-configuration \
    --function-name NotificamyNotifierLambda \
    --environment "Variables={
      DATABASE_SECRET_NAME='notificamy/database-credentials',
      API_KEYS_SECRET_NAME='notificamy/api-keys',
      AWS_REGION='eu-south-1',
      OPENAI_API_URL='https://api.openai.com/v1/chat/completions'
    }" \
    --region eu-south-1
  
  echo "✅ Configurazione completata!"
  
  # Verifica la configurazione
  echo "📋 Verifica configurazione finale..."
  aws lambda get-function \
    --function-name NotificamyNotifierLambda \
    --region eu-south-1 \
    --query 'Configuration.[FunctionName,State,LastUpdateStatus,PackageType,Architectures,Handler,Runtime]' \
    --output table
    
else
  echo "❌ Errore nella creazione della Lambda function"
  echo "💡 Possibili cause:"
  echo "   - Il ruolo IAM 'lambda-execution-role' non esiste"
  echo "   - L'immagine Docker non è compatibile"
  echo "   - Permessi AWS insufficienti"
fi