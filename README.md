# Notificamy Lambda - Hybrid Handler

## 🎯 Cosa Fa

Riceve messaggi SQS e li processa usando **RequestHandler + QuarkusStreamHandler**.

## ✨ Implementazione Ibrida

- ✅ **Estende QuarkusStreamHandler** (per Quarkus CDI)
- ✅ **Implementa RequestHandler<SQSEvent, String>** (per AWS Lambda)
- ✅ **Logging** con Quarkus
- ✅ **Echo dei messaggi** SQS
- ✅ **Uber JAR** ottimizzato

## 🚀 Deploy

1. **Push su GitHub** → Deploy automatico
2. **Test**: Invia messaggio SQS

## 📦 Struttura

```
src/main/java/com/notificamy/application/lambda/
└── NotificamyLambdaHandler.java  ← Hybrid: extends + implements
```

## 🔧 Handler Configuration

```java
@Named("notificamyLambda")
public class NotificamyLambdaHandler extends QuarkusStreamHandler 
                                    implements RequestHandler<SQSEvent, String>
```

## ✅ Vantaggi Approccio Ibrido

- ✅ **QuarkusStreamHandler**: CDI injection, Quarkus features
- ✅ **RequestHandler**: Standard AWS Lambda interface
- ✅ **Compatibilità massima** con entrambi gli ecosistemi
- ✅ **Logging Quarkus** funzionante

**IBRIDO E POTENTE!** 🎉