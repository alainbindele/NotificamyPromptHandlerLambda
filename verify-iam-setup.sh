#!/bin/bash

# Script per verificare la configurazione IAM per Lambda

echo "🔍 Verifica configurazione IAM per Lambda..."

ROLE_NAME="lambda-execution-role"
ACCOUNT_ID="435703062953"
REGION="eu-south-1"

# 1. Verifica esistenza ruolo
echo "📋 Verifica esistenza ruolo..."
if aws iam get-role --role-name $ROLE_NAME >/dev/null 2>&1; then
    echo "✅ Ruolo $ROLE_NAME esistente"
    
    # Mostra ARN del ruolo
    ROLE_ARN=$(aws iam get-role --role-name $ROLE_NAME --query 'Role.Arn' --output text)
    echo "🔗 ARN Ruolo: $ROLE_ARN"
    
    # Verifica trust policy
    echo "📋 Trust Policy:"
    aws iam get-role --role-name $ROLE_NAME --query 'Role.AssumeRolePolicyDocument' --output json
    
else
    echo "❌ Ruolo $ROLE_NAME non trovato!"
    echo "💡 Esegui: ./create-lambda-role.sh"
    exit 1
fi

# 2. Verifica policy attaccate
echo "📋 Policy attaccate al ruolo:"
aws iam list-attached-role-policies --role-name $ROLE_NAME --output table

# 3. Verifica policy inline
echo "📋 Policy inline:"
aws iam list-role-policies --role-name $ROLE_NAME --output table

# 4. Test assume role (simulazione)
echo "🧪 Test assume role..."
aws sts assume-role \
    --role-arn "arn:aws:iam::${ACCOUNT_ID}:role/${ROLE_NAME}" \
    --role-session-name "test-session" \
    --query 'Credentials.AccessKeyId' \
    --output text >/dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Assume role funzionante"
else
    echo "❌ Problema con assume role"
    echo "💡 Verifica che le credenziali AWS abbiano i permessi necessari"
fi

# 5. Verifica secrets manager access
echo "🔐 Verifica accesso Secrets Manager..."
SECRET_ARNS=(
    "arn:aws:secretsmanager:${REGION}:${ACCOUNT_ID}:secret:notificamy/database-credentials"
    "arn:aws:secretsmanager:${REGION}:${ACCOUNT_ID}:secret:notificamy/api-keys"
)

for SECRET_ARN in "${SECRET_ARNS[@]}"; do
    if aws secretsmanager describe-secret --secret-id "${SECRET_ARN}" >/dev/null 2>&1; then
        echo "✅ Secret esistente: ${SECRET_ARN}"
    else
        echo "⚠️ Secret non trovato: ${SECRET_ARN}"
        echo "💡 Crea il secret in AWS Secrets Manager"
    fi
done

# 6. Verifica SES
echo "📧 Verifica configurazione SES..."
aws ses get-send-quota --region $REGION >/dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ SES configurato"
    aws ses get-send-quota --region $REGION --output table
else
    echo "⚠️ SES non configurato o non accessibile"
fi

echo "✅ Verifica IAM completata!"
echo "🚀 Se tutti i controlli sono passati, la Lambda dovrebbe funzionare correttamente."