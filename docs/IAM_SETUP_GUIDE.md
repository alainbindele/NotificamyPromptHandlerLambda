# IAM Setup Guide for Notificamy Lambda

## Problema Risolto

L'errore `The role defined for the function cannot be assumed by Lambda` indica che:
1. Il ruolo IAM non esiste
2. Il ruolo non ha la trust policy corretta per Lambda
3. Il ruolo non ha le policy necessarie

## Soluzione Automatica

### 1. Esegui lo script di creazione ruolo:
```bash
chmod +x create-lambda-role.sh
./create-lambda-role.sh
```

### 2. Verifica la configurazione:
```bash
chmod +x verify-iam-setup.sh
./verify-iam-setup.sh
```

## Configurazione Manuale (se necessario)

### 1. Crea il Ruolo IAM

```bash
aws iam create-role \
  --role-name lambda-execution-role \
  --assume-role-policy-document '{
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
  }'
```

### 2. Attach Policy AWS Lambda Basic Execution

```bash
aws iam attach-role-policy \
  --role-name lambda-execution-role \
  --policy-arn arn:aws:iam::aws:policy/service-role/AWSLambdaBasicExecutionRole
```

### 3. Crea Policy Personalizzata

```bash
aws iam create-policy \
  --policy-name NotificamyLambdaPolicy \
  --policy-document '{
    "Version": "2012-10-17",
    "Statement": [
      {
        "Effect": "Allow",
        "Action": [
          "secretsmanager:GetSecretValue"
        ],
        "Resource": [
          "arn:aws:secretsmanager:eu-south-1:435703062953:secret:notificamy/database-credentials-*",
          "arn:aws:secretsmanager:eu-south-1:435703062953:secret:notificamy/api-keys-*"
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
        "Resource": "arn:aws:sqs:eu-south-1:435703062953:*"
      }
    ]
  }'
```

### 4. Attach Policy Personalizzata

```bash
aws iam attach-role-policy \
  --role-name lambda-execution-role \
  --policy-arn arn:aws:iam::435703062953:policy/NotificamyLambdaPolicy
```

## Verifica Configurazione

### 1. Verifica Ruolo
```bash
aws iam get-role --role-name lambda-execution-role
```

### 2. Verifica Policy Attaccate
```bash
aws iam list-attached-role-policies --role-name lambda-execution-role
```

### 3. Test Assume Role
```bash
aws sts assume-role \
  --role-arn arn:aws:iam::435703062953:role/lambda-execution-role \
  --role-session-name test-session
```

## Prerequisiti AWS Secrets Manager

Assicurati che questi secrets esistano:

### 1. Database Credentials
```bash
aws secretsmanager create-secret \
  --name "notificamy/database-credentials" \
  --description "Database credentials for Notificamy" \
  --secret-string '{
    "DB_URL": "jdbc:mysql://your-rds-endpoint:3306/notificamy",
    "DB_USER": "notificamy_user",
    "DB_PASSWORD": "your-secure-password"
  }'
```

### 2. API Keys
```bash
aws secretsmanager create-secret \
  --name "notificamy/api-keys" \
  --description "API keys for Notificamy" \
  --secret-string '{
    "OPENAI_API_KEY": "sk-your-openai-api-key",
    "WHATSAPP_API_TOKEN": "your-whatsapp-business-api-token"
  }'
```

## Troubleshooting

### Se il ruolo esiste ma non funziona:
1. Verifica la trust policy
2. Controlla che le policy siano attaccate
3. Aspetta 10-15 secondi per la propagazione IAM

### Se continui ad avere problemi:
1. Elimina il ruolo esistente: `aws iam delete-role --role-name lambda-execution-role`
2. Ri-esegui lo script di creazione
3. Verifica che l'account AWS sia corretto (435703062953)

## Workflow GitHub Actions

Il workflow ora include automaticamente:
- ✅ Creazione/verifica del ruolo IAM
- ✅ Configurazione delle policy necessarie
- ✅ Attesa per la propagazione IAM
- ✅ Verifica finale della configurazione

Questo dovrebbe risolvere completamente il problema del ruolo IAM! 🚀