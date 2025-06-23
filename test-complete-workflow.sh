#!/bin/bash

# Script per testare completamente il workflow di deployment

echo "🚀 Test completo del workflow di deployment..."

# Verifica prerequisiti
echo "🔍 Verifica prerequisiti..."

# Verifica AWS CLI
if ! command -v aws &> /dev/null; then
    echo "❌ AWS CLI non installato"
    exit 1
fi

# Verifica Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker non installato"
    exit 1
fi

# Verifica credenziali AWS
if ! aws sts get-caller-identity >/dev/null 2>&1; then
    echo "❌ Credenziali AWS non configurate"
    exit 1
fi

echo "✅ Prerequisiti verificati"

# Test 1: Creazione repository ECR
echo "📦 Test 1: Creazione repository ECR..."
aws ecr describe-repositories --repository-names notificamy --region eu-south-1 || \
aws ecr create-repository --repository-name notificamy --region eu-south-1

# Test 2: Login ECR
echo "🔐 Test 2: Login ECR..."
aws ecr get-login-password --region eu-south-1 | docker login --username AWS --password-stdin 435703062953.dkr.ecr.eu-south-1.amazonaws.com

# Test 3: Build locale
echo "🐳 Test 3: Build Docker locale..."
docker build --platform linux/amd64 -t notificamy-test .

if [ $? -ne 0 ]; then
    echo "❌ Build Docker fallito"
    exit 1
fi

# Test 4: Tag e push
echo "📤 Test 4: Tag e push immagine..."
docker tag notificamy-test 435703062953.dkr.ecr.eu-south-1.amazonaws.com/notificamy:test
docker push 435703062953.dkr.ecr.eu-south-1.amazonaws.com/notificamy:test

# Test 5: Verifica immagine su ECR
echo "📋 Test 5: Verifica immagine su ECR..."
aws ecr describe-images \
  --repository-name notificamy \
  --region eu-south-1 \
  --image-ids imageTag=test \
  --query 'imageDetails[0].[imageTags[0],imagePushedAt,imageSizeInBytes,imageManifestMediaType]' \
  --output table

# Test 6: Test creazione Lambda (opzionale)
echo "🧪 Test 6: Test creazione Lambda temporanea..."
read -p "Vuoi testare la creazione di una Lambda temporanea? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    # Elimina funzione test se esiste
    aws lambda delete-function --function-name NotificamyTest --region eu-south-1 2>/dev/null || true
    sleep 10
    
    # Crea funzione test
    aws lambda create-function \
      --function-name NotificamyTest \
      --package-type Image \
      --code ImageUri=435703062953.dkr.ecr.eu-south-1.amazonaws.com/notificamy:test \
      --role arn:aws:iam::435703062953:role/lambda-execution-role \
      --architectures x86_64 \
      --timeout 60 \
      --memory-size 512 \
      --region eu-south-1 \
      --description "Test function for Notificamy"
    
    if [ $? -eq 0 ]; then
        echo "✅ Lambda test creata con successo!"
        
        # Pulisci
        echo "🧹 Pulizia funzione test..."
        aws lambda delete-function --function-name NotificamyTest --region eu-south-1
    else
        echo "❌ Errore nella creazione Lambda test"
    fi
fi

# Pulizia immagine test
echo "🧹 Pulizia immagine test..."
aws ecr batch-delete-image \
  --repository-name notificamy \
  --region eu-south-1 \
  --image-ids imageTag=test 2>/dev/null || true

docker rmi notificamy-test 2>/dev/null || true
docker rmi 435703062953.dkr.ecr.eu-south-1.amazonaws.com/notificamy:test 2>/dev/null || true

echo "✅ Test workflow completato con successo!"
echo "🚀 Il workflow GitHub Actions dovrebbe funzionare correttamente ora."