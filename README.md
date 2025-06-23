# Notificamy Lambda - Minimal

## 🎯 Cosa Fa

Riceve messaggi SQS e li processa usando **RequestHandler**.

## ✨ Implementazione

- ✅ **RequestHandler<SQSEvent, String>** 
- ✅ **Logging** con Quarkus
- ✅ **Echo dei messaggi** SQS
- ✅ **Uber JAR** ottimizzato

## 🚀 Deploy

1. **Push su GitHub** → Deploy automatico
2. **Test**: Invia messaggio SQS

## 📦 Struttura

```
src/main/java/com/notificamy/application/lambda/
└── NotificamyLambdaHandler.java  ← Implementa RequestHandler
```

## 🔧 Handler Configuration

```java
@Named("notificamyLambda")
public class NotificamyLambdaHandler implements RequestHandler<SQSEvent, String>
```

## ✅ Risolto

- ❌ ClassNotFoundException 
- ✅ RequestHandler implementation
- ✅ Quarkus logging
- ✅ Uber JAR ottimizzato

**MINIMALE E FUNZIONALE!** 🎉