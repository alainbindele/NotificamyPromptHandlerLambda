#!/bin/bash

# Script per creare il ruolo IAM per Lambda con tutte le policy necessarie

echo "🔧 Creazione ruolo IAM per Lambda..."

ROLE_NAME="lambda-execution-role"
ACCOUNT_ID="435703062953"
REGION="eu-south-1"

# 1. Crea il ruolo IAM se non esiste
echo "📋 Verifica esistenza ruolo IAM..."
if aws iam get-role --role-name $ROLE_NAME >/dev/null 2>&1; then
    echo "✅ Ruolo $ROLE_NAME già esistente"
else
    echo "📦 Creazione ruolo $ROLE_NAME..."
    
    # Trust policy per Lambda
    cat > trust-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "lambda.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

    aws iam create-role \
        --role-name $ROLE_NAME \
        --assume-role-policy-document file://trust-policy.json \
        --description "Execution role for Notificamy Lambda function"
    
    if [ $? -eq 0 ]; then
        echo "✅ Ruolo $ROLE_NAME creato con successo!"
    else
        echo "❌ Errore nella creazione del ruolo"
        exit 1
    fi
fi

# 2. Attach policy AWS Lambda Basic Execution Role
echo "🔗 Attach AWS Lambda Basic Execution Role..."
aws iam attach-role-policy \
    --role-name $ROLE_NAME \
    --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole

# 3. Crea policy personalizzata per Notificamy
echo "📝 Creazione policy personalizzata..."
cat > notificamy-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "secretsmanager:GetSecretValue"
      ],
      "Resource": [
        "arn:aws:secretsmanager:${REGION}:${ACCOUNT_ID}:secret:notificamy/database-credentials-*",
        "arn:aws:secretsmanager:${REGION}:${ACCOUNT_ID}:secret:notificamy/api-keys-*"
      ]
    },
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "sqs:ReceiveMessage",
        "sqs:DeleteMessage",
        "sqs:GetQueueAttributes"
      ],
      "Resource": "arn:aws:sqs:${REGION}:${ACCOUNT_ID}:*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "rds:DescribeDBInstances",
        "rds:DescribeDBClusters"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
EOF

# Verifica se la policy esiste già
POLICY_NAME="NotificamyLambdaPolicy"
POLICY_ARN="arn:aws:iam::${ACCOUNT_ID}:policy/${POLICY_NAME}"

if aws iam get-policy --policy-arn $POLICY_ARN >/dev/null 2>&1; then
    echo "✅ Policy $POLICY_NAME già esistente"
    
    # Aggiorna la policy esistente
    echo "🔄 Aggiornamento policy esistente..."
    aws iam create-policy-version \
        --policy-arn $POLICY_ARN \
        --policy-document file://notificamy-policy.json \
        --set-as-default
else
    echo "📦 Creazione policy $POLICY_NAME..."
    aws iam create-policy \
        --policy-name $POLICY_NAME \
        --policy-document file://notificamy-policy.json \
        --description "Policy for Notificamy Lambda function"
fi

# 4. Attach policy personalizzata al ruolo
echo "🔗 Attach policy personalizzata al ruolo..."
aws iam attach-role-policy \
    --role-name $ROLE_NAME \
    --policy-arn $POLICY_ARN

# 5. Aspetta che il ruolo sia propagato
echo "⏳ Attesa propagazione ruolo IAM..."
sleep 10

# 6. Verifica configurazione finale
echo "📋 Verifica configurazione finale..."
echo "Ruolo ARN:"
aws iam get-role --role-name $ROLE_NAME --query 'Role.Arn' --output text

echo "Policy attaccate:"
aws iam list-attached-role-policies --role-name $ROLE_NAME --output table

# Pulizia file temporanei
rm -f trust-policy.json notificamy-policy.json

echo "✅ Ruolo IAM configurato correttamente!"
echo "🚀 Ora puoi procedere con la creazione della Lambda function."