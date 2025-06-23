# AWS Lambda Deployment Guide

## Problemi Risolti

### 1. Media Type Non Supportato
**Problema**: `The image manifest, config or layer media type is not supported`

**Soluzione**: 
- Aggiunto `--platform linux/amd64` al build Docker
- Specificato `--architectures x86_64` nell'update Lambda
- Usato `uber-jar` packaging per compatibilità Lambda

### 2. Configurazione Corretta

#### GitHub Secrets Richiesti:
```
AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
MY_GITHUB_USERNAME=your-username
MY_GITHUB_TOKEN=ghp_xxxxxxxxxxxx
DATABASE_SECRET_NAME=notificamy/database-credentials
API_KEYS_SECRET_NAME=notificamy/api-keys
AWS_SQS_QUEUE_URL=https://sqs.eu-south-1.amazonaws.com/123456789012/notificamy-queue
AWS_SES_FROM_EMAIL=noreply@yourdomain.com
AWS_SES_FROM_NAME=Notificamy
WHATSAPP_API_URL=https://graph.facebook.com/v18.0/YOUR_PHONE_NUMBER_ID/messages
```

#### AWS Secrets Manager:
**Database Secret** (`notificamy/database-credentials`):
```json
{
  "DB_URL": "jdbc:mysql://your-rds-endpoint:3306/notificamy",
  "DB_USER": "notificamy_user",
  "DB_PASSWORD": "your-secure-password"
}
```

**API Keys Secret** (`notificamy/api-keys`):
```json
{
  "OPENAI_API_KEY": "sk-your-openai-api-key",
  "WHATSAPP_API_TOKEN": "your-whatsapp-business-api-token"
}
```

### 3. Configurazione Lambda Function

Assicurati che la tua Lambda function sia configurata con:
- **Package type**: Container
- **Architecture**: x86_64
- **Runtime**: Non specificato (per container images)
- **Memory**: Almeno 512 MB (raccomandato 1024 MB)
- **Timeout**: Almeno 30 secondi (raccomandato 60 secondi)

### 4. Verifica Deployment

Dopo il deployment, verifica che:
1. L'immagine sia stata pushata correttamente su ECR
2. La Lambda function sia stata aggiornata
3. Le variabili d'ambiente siano configurate
4. I permessi IAM siano corretti

### 5. Troubleshooting

Se continui ad avere problemi:

1. **Verifica l'immagine ECR**:
```bash
aws ecr describe-images --repository-name notificamy/notifier --region eu-south-1
```

2. **Controlla i log Lambda**:
```bash
aws logs describe-log-groups --log-group-name-prefix /aws/lambda/NotificamyNotifierLambda
```

3. **Test della funzione**:
```bash
aws lambda invoke --function-name NotificamyNotifierLambda --payload '{}' response.json
```

## Modifiche Implementate

1. **Dockerfile ottimizzato** per AWS Lambda
2. **GitHub Actions workflow** con platform specification
3. **Configurazione Quarkus** per uber-jar packaging
4. **Inizializzazione automatica** dei secrets all'avvio
5. **Test configuration** separata per evitare conflitti

Queste modifiche dovrebbero risolvere completamente il problema del media type non supportato.