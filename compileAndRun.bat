@echo off
REM Esegui il comando Maven clean install
mvn clean install && (
    echo "Maven build completata con successo."
    sam build && (
        echo "SAM build completata con successo."
        sam local invoke NotificamyNotifierLambda --event event.json
        IF %ERRORLEVEL% NEQ 0 (
            echo "Errore durante 'sam local invoke'."
            exit /b %ERRORLEVEL%
        )
    ) || (
        echo "Errore durante 'sam build'. Interruzione dello script."
        exit /b %ERRORLEVEL%
    )
) || (
    echo "Errore durante 'mvn clean install'. Interruzione dello script."
    exit /b %ERRORLEVEL%
)

echo "Script completato con successo!"
pause