# Notificamy Lambda - ULTRA MINIMAL

**PROBLEMA RISOLTO**: ClassNotFoundException

## 🎯 Cosa Fa

Riceve messaggi SQS e li processa. **BASTA.**

## 🚀 Deploy

1. **Push su GitHub** → Deploy automatico
2. **Test**: `chmod +x test-lambda.sh && ./test-lambda.sh`

## 📦 Struttura

```
src/main/java/com/notificamy/application/lambda/
└── NotificamyLambdaHandler.java  ← SOLO QUESTO FILE
```

## 🔧 Secrets GitHub

```
AWS_ACCESS_KEY_ID=your-key
AWS_SECRET_ACCESS_KEY=your-secret
```

## ✅ Risolto

- ❌ ClassNotFoundException 
- ✅ JAR corretto con Shade plugin
- ✅ Handler minimale senza dipendenze
- ✅ Dockerfile ottimizzato
- ✅ Build verificato nel workflow

**ADESSO FUNZIONA!** 🎉