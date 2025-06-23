# Guida per Creare la Lambda Function

## Problema Risolto: Media Type Non Supportato

Il problema `The image manifest, config or layer media type is not supported` si verifica quando:
1. L'immagine Docker non è compatibile con AWS Lambda
2. L'architettura non è specificata correttamente
3. Il formato dell'immagine non è quello atteso da Lambda

## Soluzione: Creare la Lambda Function Manualmente

### Passo 1: Creare la Lambda Function via AWS CLI

**IMPORTANTE**: Non creare la Lambda function tramite console AWS. Usa questo comando:

```bash
aws lambda create-function \
  --function-name NotificamyNotifierLambda \
  --package-type Image \
  --code ImageUri=435703062953.dkr.ecr.eu-south-1.amazonaws.com/notificamy/notifier:latest \
  --role arn:aws:iam::435703062953:role/lambda-execution-role \
  --architectures x86_64 \
  --timeout 60 \
  --memory-size 1024 \
  --region eu-south-1
```

### Passo 2: Configurare il Ruolo IAM

Il ruolo `lambda-execution-role` deve avere queste policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    },
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
      "Resource": "arn:aws:sqs:eu-south-1:435703062953:notificamy-queue"
    }
  ]
}
```

### Passo 3: Configurare il Trigger SQS

```bash
aws lambda create-event-source-mapping \
  --function-name NotificamyNotifierLambda \
  --event-source-arn arn:aws:sqs:eu-south-1:435703062953:notificamy-queue \
  --batch-size 10 \
  --maximum-batching-window-in-seconds 5
```

### Passo 4: Verificare la Configurazione

```bash
aws lambda get-function --function-name NotificamyNotifierLambda
```

## Alternativa: Creare via Console AWS

Se preferisci usare la console AWS:

1. **Vai su AWS Lambda Console**
2. **Clicca "Create function"**
3. **Seleziona "Container image"**
4. **Configura**:
   - Function name: `NotificamyNotifierLambda`
   - Container image URI: `435703062953.dkr.ecr.eu-south-1.amazonaws.com/notificamy/notifier:latest`
   - Architecture: `x86_64`
5. **Clicca "Create function"**

### Configurazione Avanzata:

- **Memory**: 1024 MB
- **Timeout**: 1 minuto
- **Environment variables**: (verranno impostate dal workflow GitHub)

## Risoluzione Problemi

### Se l'immagine non esiste ancora:

1. **Pusha prima l'immagine manualmente**:
```bash
# Build locale
mvn clean package -DskipTests -Dquarkus.package.type=uber-jar

# Build e push Docker
docker buildx build --platform linux/amd64 -t 435703062953.dkr.ecr.eu-south-1.amazonaws.com/notificamy/notifier:latest .
docker push 435703062953.dkr.ecr.eu-south-1.amazonaws.com/notificamy/notifier:latest
```

2. **Poi crea la Lambda function**

### Se continui ad avere problemi:

1. **Verifica che ECR repository esista**:
```bash
aws ecr describe-repositories --repository-names notificamy/notifier --region eu-south-1
```

2. **Se non esiste, crealo**:
```bash
aws ecr create-repository --repository-name notificamy/notifier --region eu-south-1
```

## Ordine di Esecuzione

1. ✅ Configura GitHub Secrets
2. ✅ Crea ECR repository (se non esiste)
3. ✅ Esegui il workflow GitHub Actions (per build e push immagine)
4. ✅ Crea Lambda function con il comando AWS CLI sopra
5. ✅ Configura trigger SQS
6. ✅ Testa la funzione

Questo approccio dovrebbe risolvere definitivamente il problema del media type!