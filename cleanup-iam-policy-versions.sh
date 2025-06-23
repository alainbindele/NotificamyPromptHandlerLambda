#!/bin/bash

# Script per pulire le versioni vecchie delle policy IAM

echo "🧹 Pulizia versioni policy IAM..."

ACCOUNT_ID="435703062953"
POLICY_NAME="NotificamyLambdaPolicy"
POLICY_ARN="arn:aws:iam::${ACCOUNT_ID}:policy/${POLICY_NAME}"

# 1. Verifica se la policy esiste
if ! aws iam get-policy --policy-arn $POLICY_ARN >/dev/null 2>&1; then
    echo "ℹ️ Policy $POLICY_NAME non esiste, niente da pulire"
    exit 0
fi

echo "📋 Policy trovata: $POLICY_NAME"

# 2. Lista tutte le versioni
echo "📋 Versioni attuali della policy:"
aws iam list-policy-versions --policy-arn $POLICY_ARN --output table

# 3. Ottieni la versione di default
DEFAULT_VERSION=$(aws iam get-policy --policy-arn $POLICY_ARN --query 'Policy.DefaultVersionId' --output text)
echo "📌 Versione di default: $DEFAULT_VERSION"

# 4. Ottieni tutte le versioni non-default
NON_DEFAULT_VERSIONS=$(aws iam list-policy-versions --policy-arn $POLICY_ARN --query 'Versions[?IsDefaultVersion==`false`].VersionId' --output text)

if [ -z "$NON_DEFAULT_VERSIONS" ]; then
    echo "ℹ️ Nessuna versione non-default da eliminare"
else
    echo "🗑️ Eliminazione versioni non-default..."
    for VERSION in $NON_DEFAULT_VERSIONS; do
        echo "   Eliminando versione: $VERSION"
        aws iam delete-policy-version --policy-arn $POLICY_ARN --version-id $VERSION
        if [ $? -eq 0 ]; then
            echo "   ✅ Versione $VERSION eliminata"
        else
            echo "   ❌ Errore eliminando versione $VERSION"
        fi
    done
fi

# 5. Verifica finale
echo "📋 Versioni rimanenti:"
aws iam list-policy-versions --policy-arn $POLICY_ARN --output table

REMAINING_VERSIONS=$(aws iam list-policy-versions --policy-arn $POLICY_ARN --query 'length(Versions)' --output text)
echo "📊 Numero versioni rimanenti: $REMAINING_VERSIONS"

if [ "$REMAINING_VERSIONS" -lt 5 ]; then
    echo "✅ Spazio disponibile per nuove versioni!"
else
    echo "⚠️ Ancora troppe versioni, potrebbe essere necessaria pulizia manuale"
fi

echo "✅ Pulizia policy completata!"