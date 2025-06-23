#!/bin/bash

# Script per risolvere il problema della classe Lambda non trovata

echo "🔧 Fix Quarkus Lambda - Risoluzione problema classe non trovata..."

# 1. Pulizia completa
echo "🧹 Pulizia completa del progetto..."
mvn clean
rm -rf target/

# 2. Verifica struttura sorgenti
echo "🔍 Verifica struttura sorgenti..."
find src -name "*.java" | grep -i lambda

# 3. Verifica che la classe handler esista
HANDLER_FILE="src/main/java/com/notificamy/application/lambda/NotificamyLambdaHandler.java"
if [ -f "$HANDLER_FILE" ]; then
    echo "✅ Handler class trovata: $HANDLER_FILE"
else
    echo "❌ Handler class non trovata!"
    exit 1
fi

# 4. Compila solo le classi Java
echo "📦 Compilazione classi Java..."
mvn compile

# 5. Verifica che la classe sia compilata
COMPILED_CLASS="target/classes/com/notificamy/application/lambda/NotificamyLambdaHandler.class"
if [ -f "$COMPILED_CLASS" ]; then
    echo "✅ Classe compilata trovata: $COMPILED_CLASS"
else
    echo "❌ Classe non compilata!"
    echo "🔍 Contenuto target/classes/com/notificamy/:"
    find target/classes/com/notificamy/ -name "*.class" 2>/dev/null || echo "Nessuna classe trovata"
    exit 1
fi

# 6. Build del JAR uber
echo "📦 Build JAR uber..."
mvn package -DskipTests -Dquarkus.package.type=uber-jar

# 7. Verifica contenuto JAR
JAR_FILE="target/lambda-processor-1.0.0-SNAPSHOT-runner.jar"
if [ -f "$JAR_FILE" ]; then
    echo "✅ JAR creato: $(ls -lh $JAR_FILE)"
    
    echo "🔍 Verifica contenuto JAR..."
    echo "Handler class nel JAR:"
    jar tf "$JAR_FILE" | grep -i "NotificamyLambdaHandler" || echo "❌ Handler non trovato nel JAR"
    
    echo "Quarkus Lambda classes nel JAR:"
    jar tf "$JAR_FILE" | grep -i "QuarkusStreamHandler" || echo "❌ QuarkusStreamHandler non trovato"
    
    echo "Package structure:"
    jar tf "$JAR_FILE" | grep "com/notificamy/application/lambda/" || echo "❌ Package lambda non trovato"
    
else
    echo "❌ JAR non creato!"
    exit 1
fi

# 8. Test estrazione classe
echo "🧪 Test estrazione classe dal JAR..."
mkdir -p temp_extract
cd temp_extract
jar xf "../$JAR_FILE" "com/notificamy/application/lambda/NotificamyLambdaHandler.class" 2>/dev/null
if [ -f "com/notificamy/application/lambda/NotificamyLambdaHandler.class" ]; then
    echo "✅ Classe estratta con successo dal JAR"
else
    echo "❌ Impossibile estrarre la classe dal JAR"
fi
cd ..
rm -rf temp_extract

# 9. Verifica configurazione Quarkus
echo "🔍 Verifica configurazione application.yml..."
grep -A 5 -B 5 "lambda:" src/main/resources/application.yml || echo "⚠️ Configurazione lambda non trovata"

# 10. Test build Docker locale
echo "🐳 Test build Docker locale..."
if docker build -t notificamy-lambda-test . >/dev/null 2>&1; then
    echo "✅ Docker build riuscito"
    
    # Test contenuto immagine Docker
    echo "🔍 Test contenuto immagine Docker..."
    docker run --rm notificamy-lambda-test ls -la /var/task/ | head -5
else
    echo "❌ Docker build fallito"
fi

echo ""
echo "🎯 Riepilogo:"
echo "1. ✅ Classe handler esiste nei sorgenti"
echo "2. ✅ Classe compilata correttamente"
echo "3. ✅ JAR uber creato"
if jar tf "$JAR_FILE" | grep -q "NotificamyLambdaHandler"; then
    echo "4. ✅ Classe presente nel JAR"
else
    echo "4. ❌ Classe NON presente nel JAR - PROBLEMA PRINCIPALE"
fi

echo ""
echo "💡 Soluzioni suggerite:"
echo "1. Verifica che @ApplicationScoped sia presente nella classe handler"
echo "2. Verifica che la configurazione quarkus.lambda.handler sia corretta"
echo "3. Prova a ricompilare con: mvn clean package -Dquarkus.package.type=uber-jar"
echo "4. Se il problema persiste, controlla i log di build per errori di compilazione"

echo "✅ Diagnosi completata!"