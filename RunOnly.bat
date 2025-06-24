@echo off
REM Esegui il comando Maven clean install

        sam local invoke NotificamyNotifierLambda --event event.json
        IF %ERRORLEVEL% NEQ 0 (
            echo "Errore durante 'sam local invoke'."
            exit /b %ERRORLEVEL%
        )

echo "Script completato con successo!"
pause