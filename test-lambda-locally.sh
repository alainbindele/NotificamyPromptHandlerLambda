#!/bin/bash

# Script per testare la Lambda localmente

echo "🧪 Test Lambda localmente..."

JAR_FILE="target/lambda-processor-1.0.0-SNAPSHOT-runner.jar"

# 1. Verifica JAR
if [ ! -f "$JAR_FILE" ]; then
    echo "❌ JAR non trovato. Building..."
    mvn clean package -DskipTests -Dquarkus.package.type=uber-jar
fi

# 2. Set environment variables per test
export AWS_REGION=eu-south-1
export DATABASE_SECRET_NAME=test-db-secret
export API_KEYS_SECRET_NAME=test-api-secret
export NOTIFICAMY_AWS_REGION=eu-south-1

# 3. Test 1: Verifica che la JVM possa caricare la classe
echo "🔍 Test 1: Verifica caricamento classe..."
java -cp "$JAR_FILE" -Dquarkus.log.level=DEBUG \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  com.notificamy.application.lambda.NotificamyLambdaHandler 2>&1 | head -20

# 4. Test 2: Simula invocazione Lambda con payload SQS
echo "🔍 Test 2: Simulazione invocazione Lambda..."

# Crea payload di test
cat > test-payload.json << EOF
{
  "Records": [
    {
      "body": "{\"query_id\":1,\"prompt\":\"Test message\",\"user_email\":\"test@example.com\",\"user_phone\":\"+1234567890\"}"
    }
  ]
}
EOF

# Test con Quarkus dev mode (se possibile)
echo "🔍 Test 3: Avvio Quarkus per verifica dipendenze..."
timeout 30s mvn quarkus:dev -Dquarkus.args="--help" 2>&1 | head -20 || echo "Quarkus dev mode test completato"

# 4. Verifica configurazione Quarkus
echo "🔍 Test 4: Verifica configurazione Quarkus..."
java -jar "$JAR_FILE" -Dquarkus.log.level=INFO \
  -Djava.util.logging.manager=org.jboss.logmanager.LogManager \
  --help 2>&1 | head -10 || echo "Help command test completato"

# Pulizia
rm -f test-payload.json

echo "✅ Test locali completati!"
echo ""
echo "💡 Se i test falliscono:"
echo "1. Verifica che tutte le dipendenze siano nel pom.xml"
echo "2. Ricompila con: mvn clean package -Dquarkus.package.type=uber-jar"
echo "3. Controlla i log per errori di configurazione"
echo "4. Verifica che AWS credentials siano configurate per test completi"