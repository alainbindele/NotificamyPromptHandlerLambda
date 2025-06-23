#!/bin/bash

# Script per testare il build Docker localmente

echo "🔧 Test build Docker per AWS Lambda..."

# Pulisci build precedenti
echo "🧹 Pulizia build precedenti..."
mvn clean

# Build dell'applicazione
echo "📦 Build Maven..."
mvn package -DskipTests -Dquarkus.package.type=uber-jar

# Verifica che il JAR esista
if [ ! -f target/lambda-processor-1.0.0-SNAPSHOT-runner.jar ]; then
  echo "❌ JAR file non trovato!"
  exit 1
fi

echo "✅ JAR file creato: $(ls -lh target/lambda-processor-1.0.0-SNAPSHOT-runner.jar)"

# Build Docker image
echo "🐳 Build Docker image..."
docker build --platform linux/amd64 -t notificamy-lambda-test .

if [ $? -eq 0 ]; then
  echo "✅ Docker image creata con successo!"
  
  # Mostra informazioni sull'immagine
  echo "📋 Informazioni immagine:"
  docker images notificamy-lambda-test
  
  # Test rapido dell'immagine
  echo "🧪 Test rapido dell'immagine..."
  docker run --rm \
    -e AWS_REGION=eu-south-1 \
    -e DATABASE_SECRET_NAME=test \
    -e API_KEYS_SECRET_NAME=test \
    notificamy-lambda-test \
    echo "Test completato"
  
  echo "🚀 Immagine Docker pronta per il push su ECR!"
else
  echo "❌ Errore nel build Docker"
  exit 1
fi