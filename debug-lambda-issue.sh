#!/bin/bash

# Script completo per debuggare e risolvere il problema Lambda

echo "🔧 Debug completo problema Lambda - Classe non trovata"

# 1. Pulizia totale
echo "🧹 Pulizia totale..."
mvn clean
rm -rf target/

# 2. Verifica sorgenti
echo "🔍 Verifica sorgenti..."
if [ -f "src/main/java/com/notificamy/application/lambda/NotificamyLambdaHandler.java" ]; then
    echo "✅ Handler class trovata nei sorgenti"
    echo "📋 Annotazioni nella classe:"
    grep -E "@(ApplicationScoped|Named|Component)" src/main/java/com/notificamy/application/lambda/NotificamyLambdaHandler.java
else
    echo "❌ Handler class non trovata!"
    exit 1
fi

# 3. Verifica dipendenze Maven
echo "🔍 Verifica dipendenze Maven..."
mvn dependency:tree | grep -E "(quarkus-amazon-lambda|aws-lambda)" | head -5

# 4. Compilazione step-by-step
echo "📦 Compilazione step-by-step..."

# Step 1: Solo compile
mvn compile
if [ $? -ne 0 ]; then
    echo "❌ Errore in compilazione"
    exit 1
fi

# Step 2: Verifica classe compilata
if [ -f "target/classes/com/notificamy/application/lambda/NotificamyLambdaHandler.class" ]; then
    echo "✅ Classe compilata correttamente"
else
    echo "❌ Classe non compilata"
    echo "📋 Contenuto target/classes/com/notificamy/:"
    find target/classes/com/notificamy/ -name "*.class" 2>/dev/null || echo "Nessuna classe trovata"
    exit 1
fi

# Step 3: Package
mvn package -DskipTests -Dquarkus.package.type=uber-jar
if [ $? -ne 0 ]; then
    echo "❌ Errore in packaging"
    exit 1
fi

# 5. Analisi JAR dettagliata
JAR_FILE="target/lambda-processor-1.0.0-SNAPSHOT-runner.jar"
echo "🔍 Analisi JAR dettagliata..."

if [ -f "$JAR_FILE" ]; then
    echo "✅ JAR creato: $(ls -lh $JAR_FILE)"
    
    # Verifica handler class
    echo "🔍 Ricerca handler class nel JAR..."
    if jar tf "$JAR_FILE" | grep -q "com/notificamy/application/lambda/NotificamyLambdaHandler.class"; then
        echo "✅ Handler class trovata nel JAR"
    else
        echo "❌ Handler class NON trovata nel JAR"
        echo "📋 Classi lambda presenti:"
        jar tf "$JAR_FILE" | grep -i lambda | head -10
    fi
    
    # Verifica Quarkus Lambda
    echo "🔍 Ricerca Quarkus Lambda nel JAR..."
    if jar tf "$JAR_FILE" | grep -q "io/quarkus/amazon/lambda/runtime/QuarkusStreamHandler.class"; then
        echo "✅ QuarkusStreamHandler trovato nel JAR"
    else
        echo "❌ QuarkusStreamHandler NON trovato nel JAR"
        echo "📋 Classi Quarkus presenti:"
        jar tf "$JAR_FILE" | grep -i quarkus | grep -i lambda | head -10
    fi
    
    # Verifica CDI
    echo "🔍 Verifica configurazione CDI..."
    if jar tf "$JAR_FILE" | grep -E "(beans\.xml|jandex\.idx)" >/dev/null; then
        echo "✅ Configurazione CDI trovata"
    else
        echo "⚠️ Configurazione CDI non trovata"
    fi
    
    # Verifica MANIFEST
    echo "🔍 Verifica MANIFEST.MF..."
    jar xf "$JAR_FILE" META-INF/MANIFEST.MF
    if [ -f "META-INF/MANIFEST.MF" ]; then
        echo "📋 Main-Class nel MANIFEST:"
        grep "Main-Class" META-INF/MANIFEST.MF || echo "Main-Class non trovato"
        rm -f META-INF/MANIFEST.MF
        rmdir META-INF 2>/dev/null || true
    fi
    
else
    echo "❌ JAR non creato!"
    exit 1
fi

# 6. Test estrazione e verifica
echo "🧪 Test estrazione classe..."
mkdir -p temp_test
cd temp_test
jar xf "../$JAR_FILE" "com/notificamy/application/lambda/NotificamyLambdaHandler.class" 2>/dev/null
if [ -f "com/notificamy/application/lambda/NotificamyLambdaHandler.class" ]; then
    echo "✅ Classe estratta con successo"
    echo "📋 Informazioni classe:"
    file com/notificamy/application/lambda/NotificamyLambdaHandler.class
else
    echo "❌ Impossibile estrarre la classe"
fi
cd ..
rm -rf temp_test

# 7. Test Docker build
echo "🐳 Test Docker build..."
if docker build -t notificamy-debug-test . >/dev/null 2>&1; then
    echo "✅ Docker build riuscito"
    
    # Test contenuto container
    echo "🔍 Test contenuto container..."
    docker run --rm notificamy-debug-test ls -la /var/task/ | head -3
    
    # Test JAR nel container
    echo "🔍 Test JAR nel container..."
    docker run --rm notificamy-debug-test jar tf lambda-processor-1.0.0-SNAPSHOT-runner.jar | grep -i "NotificamyLambdaHandler" && echo "✅ Handler nel container" || echo "❌ Handler non nel container"
    
    # Cleanup
    docker rmi notificamy-debug-test >/dev/null 2>&1
else
    echo "❌ Docker build fallito"
fi

# 8. Suggerimenti finali
echo ""
echo "🎯 DIAGNOSI FINALE:"
echo "=================="

# Verifica finale handler nel JAR
if jar tf "$JAR_FILE" | grep -q "com/notificamy/application/lambda/NotificamyLambdaHandler.class"; then
    echo "✅ Handler class presente nel JAR"
    echo "🔧 Il problema potrebbe essere:"
    echo "   1. Configurazione handler errata in application.yml"
    echo "   2. CDI non funzionante correttamente"
    echo "   3. Conflitto di classpath"
    echo ""
    echo "💡 SOLUZIONE SUGGERITA:"
    echo "   - Usa: CMD [\"io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest\"]"
    echo "   - Handler CDI: @Named(\"lambdaHandler\")"
    echo "   - Quarkus config: handler: io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler"
else
    echo "❌ Handler class NON presente nel JAR - PROBLEMA CRITICO"
    echo "💡 SOLUZIONI:"
    echo "   1. Verifica che @ApplicationScoped sia presente"
    echo "   2. Verifica che la classe sia nel package corretto"
    echo "   3. Ricompila con: mvn clean package -Dquarkus.package.type=uber-jar"
    echo "   4. Controlla errori di compilazione nei log Maven"
fi

echo ""
echo "✅ Debug completato!"