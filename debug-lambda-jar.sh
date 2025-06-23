#!/bin/bash

# Script per debuggare il contenuto del JAR Lambda

echo "🔍 Debug JAR Lambda - Verifica contenuto e classpath..."

JAR_FILE="target/lambda-processor-1.0.0-SNAPSHOT-runner.jar"

# 1. Verifica esistenza JAR
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR file non trovato: $JAR_FILE"
    echo "💡 Esegui prima: mvn clean package -DskipTests -Dquarkus.package.type=uber-jar"
    exit 1
fi

echo "✅ JAR file trovato: $(ls -lh $JAR_FILE)"

# 2. Verifica contenuto JAR - cerca la classe handler
echo "🔍 Ricerca classe NotificamyLambdaHandler nel JAR..."
jar tf "$JAR_FILE" | grep -i "notificamylambdahandler" || echo "⚠️ NotificamyLambdaHandler non trovato"

# 3. Verifica classi Quarkus Lambda
echo "🔍 Ricerca classi Quarkus Lambda nel JAR..."
jar tf "$JAR_FILE" | grep -i "quarkusstreamhandler" || echo "⚠️ QuarkusStreamHandler non trovato"

# 4. Verifica struttura package
echo "🔍 Struttura package com.notificamy..."
jar tf "$JAR_FILE" | grep "com/notificamy" | head -20

# 5. Verifica MANIFEST.MF
echo "🔍 Contenuto MANIFEST.MF..."
jar xf "$JAR_FILE" META-INF/MANIFEST.MF
cat META-INF/MANIFEST.MF
rm -f META-INF/MANIFEST.MF
rmdir META-INF 2>/dev/null || true

# 6. Verifica application.yml
echo "🔍 Verifica application.yml nel JAR..."
jar tf "$JAR_FILE" | grep "application.yml" || echo "⚠️ application.yml non trovato"

# 7. Verifica dipendenze AWS Lambda
echo "🔍 Verifica dipendenze AWS Lambda..."
jar tf "$JAR_FILE" | grep -E "(aws-lambda|amazon-lambda)" | head -10

# 8. Test estrazione classe specifica
echo "🔍 Test estrazione classe handler..."
jar xf "$JAR_FILE" "com/notificamy/application/lambda/NotificamyLambdaHandler.class" 2>/dev/null
if [ -f "com/notificamy/application/lambda/NotificamyLambdaHandler.class" ]; then
    echo "✅ Classe NotificamyLambdaHandler estratta con successo"
    rm -rf com/
else
    echo "❌ Impossibile estrarre NotificamyLambdaHandler.class"
fi

# 9. Verifica dimensione JAR
echo "📊 Statistiche JAR:"
echo "Dimensione: $(du -h $JAR_FILE | cut -f1)"
echo "Numero file: $(jar tf $JAR_FILE | wc -l)"
echo "Classi Java: $(jar tf $JAR_FILE | grep '\.class$' | wc -l)"

# 10. Suggerimenti per il debug
echo ""
echo "🔧 Suggerimenti per il debug:"
echo "1. Se NotificamyLambdaHandler non è nel JAR:"
echo "   - Verifica che la classe sia compilata: ls -la target/classes/com/notificamy/application/lambda/"
echo "   - Ricompila: mvn clean compile"
echo ""
echo "2. Se QuarkusStreamHandler non è nel JAR:"
echo "   - Verifica dipendenza quarkus-amazon-lambda nel pom.xml"
echo "   - Ricompila con: mvn clean package -Dquarkus.package.type=uber-jar"
echo ""
echo "3. Per testare localmente:"
echo "   - java -cp $JAR_FILE com.notificamy.application.lambda.NotificamyLambdaHandler"

echo "✅ Debug JAR completato!"