#!/bin/bash

# Script per creare il repository ECR se non esiste

echo "🔍 Verifica esistenza repository ECR..."

# Verifica se il repository esiste
if aws ecr describe-repositories --repository-names notificamy --region eu-south-1 >/dev/null 2>&1; then
  echo "✅ Repository ECR 'notificamy' già esistente"
else
  echo "📦 Creazione repository ECR 'notificamy'..."
  aws ecr create-repository \
    --repository-name notificamy \
    --region eu-south-1 \
    --image-scanning-configuration scanOnPush=true
  
  if [ $? -eq 0 ]; then
    echo "✅ Repository ECR creato con successo!"
  else
    echo "❌ Errore nella creazione del repository ECR"
    exit 1
  fi
fi

# Mostra informazioni del repository
echo "📋 Informazioni repository:"
aws ecr describe-repositories \
  --repository-names notificamy \
  --region eu-south-1 \
  --query 'repositories[0].[repositoryName,repositoryUri,createdAt]' \
  --output table

echo "🚀 Repository ECR pronto per il deployment!"