#!/bin/bash

# Script per verificare la compatibilità dell'immagine Docker con AWS Lambda

echo "🔍 Verifica compatibilità immagine Docker per AWS Lambda..."

# Verifica che l'immagine esista localmente
if ! docker images | grep -q "notificamy-lambda"; then
  echo "❌ Immagine 'notificamy-lambda' non trovata localmente"
  echo "💡 Esegui prima: docker build -t notificamy-lambda ."
  exit 1
fi

echo "✅ Immagine trovata localmente"

# Verifica la piattaforma dell'immagine
echo "📋 Informazioni immagine:"
docker inspect notificamy-lambda --format='{{.Architecture}}'
docker inspect notificamy-lambda --format='{{.Os}}'

# Verifica che il JAR esista nell'immagine
echo "📋 Contenuto immagine:"
docker run --rm notificamy-lambda ls -la /var/task/

# Test rapido dell'immagine
echo "🧪 Test rapido dell'immagine..."
docker run --rm \
  -e AWS_REGION=eu-south-1 \
  -e DATABASE_SECRET_NAME=test \
  -e API_KEYS_SECRET_NAME=test \
  notificamy-lambda \
  echo "Immagine funzionante"

if [ $? -eq 0 ]; then
  echo "✅ Immagine compatibile con AWS Lambda"
  echo "🚀 Puoi procedere con il push su ECR e la creazione della Lambda"
else
  echo "❌ Problemi con l'immagine Docker"
fi