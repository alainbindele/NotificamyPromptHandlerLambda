package com.notificamy.infrastructure.adapter.ai;

import com.notificamy.domain.port.AiServicePort;
import com.notificamy.infrastructure.external.dto.ChatGptResponse;
import com.notificamy.infrastructure.external.dto.OpenAiRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class ChatGptAdapter implements AiServicePort {
    
    private static final Logger LOG = Logger.getLogger(ChatGptAdapter.class);
    
    @Inject
    SecretsManagerClient secretsManagerClient;
    
    @ConfigProperty(name = "app.aws.secrets.api-keys")
    String apiKeysSecretName;
    
    @ConfigProperty(name = "app.openai.api-url")
    String apiUrl;
    
    @ConfigProperty(name = "app.openai.policy")
    String chatGptPolicy;
    
    @ConfigProperty(name = "app.openai.max-tokens")
    int maxTokens;
    
    @ConfigProperty(name = "app.openai.temperature")
    double temperature;
    
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private String cachedApiKey;
    
    public ChatGptAdapter() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }
    
    @Override
    public String processPrompt(String prompt) {
        return processPrompt(prompt, false);
    }
    
    public String processPrompt(String prompt, boolean isConditionalQuery) {
        try {
            LOG.infof("=== CHATGPT ADAPTER - STARTING ===");
            LOG.infof("Prompt length: %d characters", prompt != null ? prompt.length() : 0);
            LOG.infof("Is conditional query: %s", isConditionalQuery);
            LOG.infof("Prompt preview (first 200 chars): %s", 
                    prompt != null && prompt.length() > 200 ? prompt.substring(0, 200) + "..." : prompt);
            
            String apiKey = getOpenAiApiKey();
            
            LOG.infof("🤖 OpenAI API Key status: %s", apiKey != null ? "Retrieved (***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) + ")" : "null");
            
            if (apiKey == null || apiKey.isEmpty()) {
                LOG.errorf("❌ OpenAI API key not found in secrets");
                return "Sorry, the AI service is currently unavailable.";
            }
            
            String policy = buildPolicy(isConditionalQuery);
            LOG.infof("🤖 Policy built - Length: %d characters", policy.length());
            LOG.infof("🤖 Policy type: %s", isConditionalQuery ? "Conditional" : "Standard");
            
            // Usa i parametri configurabili
            OpenAiRequest request = new OpenAiRequest(policy, prompt, maxTokens, temperature);
            
            LOG.infof("🤖 ChatGPT Request Parameters:");
            LOG.infof("  API URL: %s", apiUrl);
            LOG.infof("  Max tokens: %d", maxTokens);
            LOG.infof("  Temperature: %.2f", temperature);
            LOG.infof("  Model: gpt-4o");
            
            String requestBody = objectMapper.writeValueAsString(request);
            LOG.infof("🤖 Request body length: %d characters", requestBody.length());
            LOG.infof("🤖 Request body preview: %s", 
                    requestBody.length() > 500 ? requestBody.substring(0, 500) + "..." : requestBody);
            
            LOG.infof("🤖 Sending HTTP POST to OpenAI API...");
            long startTime = System.currentTimeMillis();
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            long endTime = System.currentTimeMillis();
            LOG.infof("🤖 OpenAI API response received:");
            LOG.infof("  Status Code: %d", response.statusCode());
            LOG.infof("  Duration: %dms", (endTime - startTime));
            LOG.infof("  Response body length: %d characters", response.body().length());
            LOG.infof("  Response body preview: %s", 
                    response.body().length() > 300 ? response.body().substring(0, 300) + "..." : response.body());
            
            if (response.statusCode() == 200) {
                ChatGptResponse chatGptResponse = objectMapper.readValue(response.body(), ChatGptResponse.class);
                LOG.infof("🤖 Response parsed successfully");
                
                if (chatGptResponse.getChoices() != null && !chatGptResponse.getChoices().isEmpty()) {
                    LOG.infof("🤖 Found %d choices in response", chatGptResponse.getChoices().size());
                    ChatGptResponse.Message message = chatGptResponse.getChoices().get(0).getMessage();
                    
                    // Controlla se c'è un refusal (rifiuto da parte di OpenAI)
                    if (message.getRefusal() != null && !message.getRefusal().isEmpty()) {
                        LOG.warnf("⚠️ OpenAI refused the request: %s", message.getRefusal());
                        return "I'm sorry, but I cannot process this request due to content policy restrictions.";
                    }
                    
                    String content = message.getContent();
                    if (content != null && !content.isEmpty()) {
                        LOG.infof("✅ CHATGPT RESPONSE RECEIVED SUCCESSFULLY");
                        LOG.infof("  Content length: %d characters", content.length());
                        LOG.infof("  Content preview: %s", 
                                content.length() > 200 ? content.substring(0, 200) + "..." : content);
                        
                        // Log se la risposta potrebbe essere stata troncata
                        if (chatGptResponse.getChoices().get(0).getFinishReason() != null) {
                            String finishReason = chatGptResponse.getChoices().get(0).getFinishReason();
                            LOG.infof("🤖 Finish reason: %s", finishReason);
                            
                            if ("length".equals(finishReason)) {
                                LOG.warnf("⚠️ Response truncated due to max_tokens limit (%d)", maxTokens);
                            }
                        }
                        
                        // Log usage information if available
                        if (chatGptResponse.getUsage() != null) {
                            LOG.infof("🤖 Token usage:");
                            LOG.infof("  Prompt tokens: %d", chatGptResponse.getUsage().getPromptTokens());
                            LOG.infof("  Completion tokens: %d", chatGptResponse.getUsage().getCompletionTokens());
                            LOG.infof("  Total tokens: %d", chatGptResponse.getUsage().getTotalTokens());
                        }
                        
                        return content;
                    } else {
                        LOG.errorf("❌ Empty content in ChatGPT response");
                        return "Sorry, I couldn't generate a proper response at this time.";
                    }
                } else {
                    LOG.errorf("❌ Empty choices in ChatGPT response");
                    return "Sorry, I couldn't process your request at this time.";
                }
            } else {
                LOG.errorf("❌ CHATGPT API ERROR");
                LOG.errorf("  Status Code: %d", response.statusCode());
                LOG.errorf("  Response Body: %s", response.body());
                return "Sorry, there was an error processing your request.";
            }
            
        } catch (Exception e) {
            LOG.errorf("❌ CHATGPT API CALL FAILED");
            LOG.errorf("  Error: %s", e.getMessage());
            LOG.errorf("  Exception type: %s", e.getClass().getSimpleName());
            if (e.getCause() != null) {
                LOG.errorf("  Caused by: %s - %s", e.getCause().getClass().getSimpleName(), e.getCause().getMessage());
            }
            return "Sorry, there was an error processing your request.";
        }
    }
    
    private String getOpenAiApiKey() {
        if (cachedApiKey != null) {
            return cachedApiKey;
        }
        
        try {
            GetSecretValueRequest secretRequest = GetSecretValueRequest.builder()
                    .secretId(apiKeysSecretName)
                    .build();
            
            GetSecretValueResponse secretResponse = secretsManagerClient.getSecretValue(secretRequest);
            String secretString = secretResponse.secretString();
            
            JsonNode secretJson = objectMapper.readTree(secretString);
            cachedApiKey = secretJson.get("OPENAI_API_KEY").asText();
            
            LOG.info("OpenAI API key retrieved from AWS Secrets Manager");
            return cachedApiKey;
            
        } catch (Exception e) {
            LOG.errorf(e, "Failed to retrieve OpenAI API key from AWS Secrets Manager");
            return null;
        }
    }
    
    private String buildPolicy(boolean isConditionalQuery) {
        // Se la policy è configurata e non è il placeholder, usala
        if (chatGptPolicy != null && !chatGptPolicy.equals("POLICY TEXT") && !chatGptPolicy.trim().isEmpty()) {
            LOG.infof("Using custom ChatGPT policy from configuration");
            return enhancePolicyForQueryType(chatGptPolicy, isConditionalQuery);
        }
        
        // Altrimenti usa la policy di default
        LOG.infof("Using default ChatGPT policy for %s query", isConditionalQuery ? "conditional" : "informational");
        return getDefaultPolicy(isConditionalQuery);
    }
    
    private String enhancePolicyForQueryType(String basePolicy, boolean isConditionalQuery) {
        if (isConditionalQuery) {
            return basePolicy + "\n\n" + getConditionalCheckInstructions();
        }
        return basePolicy;
    }
    
    private String getDefaultPolicy(boolean isConditionalQuery) {
        String basePolicy = getBasePolicyText();
        
        if (isConditionalQuery) {
            return basePolicy + "\n\n" + getConditionalCheckInstructions();
        }
        
        return basePolicy;
    }
    
    private String getBasePolicyText() {
        return """
                🎯 **Prompt Processing Policy – Versione GPT o3 (Fancy HTML + Media + Conditional Checking)**
                
                Questa policy istruisce l'agente IA (basato su **OpenAI o3**) su come trasformare i prompt degli utenti in **HTML arricchito** (markup, CSS leggero e immagini pertinenti) da usare come corpo delle notifiche nel progetto **Notificami**.
                
                ---
                ## 1 · Obiettivo
                - Comprendere richieste informative o di intrattenimento.
                - Restituire **solo il contenuto del `<body>`** (nessun `<html>`/`<head>` wrapper).
                - Integrare **CSS embedded** per una visualizzazione elegante e coinvolgente.
                - **SEMPRE esaudire le richieste del prompt con informazioni concrete e attuali**.
                - **MAI dire "controllo programmato" o "prossimo controllo" - fornire sempre la risposta richiesta**.
                - **FORNISCI SEMPRE** le informazioni richieste (andamento ETF ARKK) con dati attuali
                - **ASSUMI** che sia il momento giusto per fornire la risposta
                - **INCLUDI** l'orario attuale nella risposta per contestualizzare
                - **SE RICHIESTO REPORT** sii esaustivo ed esplora anche possibilità che all'utente possano essere sfuggite cercando di stimolarlo con argomentazioni utili
                - **SEMPRE** se ti viene richiesto un report inserisci le fonti possibilimente con link cliccabile
                
                Esempio CORRETTO:
                ```html
                <div class="card">
                  <h1>📈 Andamento ETF ARKK</h1>
                  <p class="time">Aggiornamento delle 20:45 – 9 Luglio 2025</p>
                  <p class="price">Prezzo attuale: $72.06</p>
                  <p>L'ETF ARK Innovation (ARKK) ha registrato...</p>
                </div>
                ```
                
                Esempio SBAGLIATO:
                ```html
                <div class="card">
                  <h1>⏰ Controllo Programmato</h1>
                  <p>Il controllo è programmato per le 20:45...</p>
                </div>
                ```
                IMPORTANTE: "ad eccezione delle regole per un CONTROLLO CONDIZIONALE AVANZATO (che se presenti nella policy andranno considerate nella risposta)
                NON considerare nel prompt che ricevi in input il riferimento all'essere notificato e/o al riferimento temporale in cui farlo
                considera solo la natura di quello che viene richiesto come prompt"
                ES: "notificami ogni ora sull'andamento dei mercati finanziari" -> CONSIDERARE: "Richiesto andamento mercati finanziari"
                ES2: "notificami domani alle 15 sul prezzo dell'oro" -> CONSIDERARE: "Richiesto prezzo dell'oro"
                
                 ## 4.0 · Tipi di richieste supportate
                | Categoria                | Esempi di prompt                                                |
                |--------------------------|-----------------------------------------------------------------|
                | **News & Attualità**     | “Ultime notizie sulla guerra in Iraq”, “Aggiornamenti Metro C Roma” |
                | **Intrattenimento**      | “Una barzelletta al giorno”, “Curiosità scientifiche quotidiane” |
                | **Meteo & Traffico**     | “Meteo Milano domani”, “Traffico tangenziale Torino”             |
                | **Riepiloghi ricorrenti**| “3 notizie tech ogni mattina”, “Frase motivazionale quotidiana”  |
                | **ALTRE CATEGORIE**      | "qualunque categoria che non appaia tra quelle NON supportate"
                
                ## 4.1 · Tipi di richieste NON supportate
                | Categoria                | Esempi di prompt                                                |
                |--------------------------|-----------------------------------------------------------------|
                | **Sesso e discriminazione**| "Ultime notizie sulle pornostar irachene transex"               |
                | **Argomenti criminali**   | "Dimmi se posso fare bombe fatte in casa a meno di 100$"       |
                | **Argomenti terroristici**| "Notificami se il mio terrorista preferito ha ucciso 1000 persone domani alle 10"  |
                | **Argomenti assurdi o irrazionali**| "Notificami se il gomito mi è andato sulla luna"  |
                
                > *Se il prompt non rientra nelle categorie richieste rispondi con un html fatto così:
                
               
                <style>
                body{font-family:"Segoe UI",sans-serif;background:#f4f4f8;color:#333;margin:0;padding:1.2rem;}
                .card{background:#fff;border-radius:12px;box-shadow:0 3px 8px rgba(0,0,0,.08);padding:1.5rem;max-width:600px;margin:0 auto;}
                .card img{width:100%;border-radius:8px;margin-bottom:1rem;}
                h1{color:#0066cc;margin-top:0;}
                </style>
                <div class="card">
                  <h1>INVALID REQUEST</h1>
                  <p>Motivo: {{MOTIVATION_PLACEHOLDER}}</p>
                </div>
                
                > *Sostituisci {{MOTIVATION_PLACEHOLDER}} con la motivazione per cui non è valido il prompt *
                
                ## 5 · Linee guida per l'output HTML
                1. **Struttura Generale** 
                   - Includere **un blocco `<style>`** come primo elemento per definire la presentazione. 
                   - Utilizzare tag semantici (`<h1>`, `<h2>`, `<p>`, `<ul><li>`, `<strong>`, ecc.). 
                   - Racchiudere i contenuti in container con classi (es. `.card`) per applicare stili.
                
                2. **Stile & Accessibilità** 
                   - Font leggibile (es. `"Segoe UI", system-ui, sans-serif`). 
                   - Colori a contrasto, angoli arrotondati (`border-radius` ≥ 6 px), ombre delicate. 
          
               
                3. **Contenuto** 
                   - Linguaggio chiaro ed esaustivo. Paragrafi ≤ 80 parole. 
                   - Titolo principale (`<h1>`) ≤ 70 caratteri; includere timestamp se notizia. 
                   - **SEMPRE fornire informazioni concrete e attuali, mai messaggi di attesa**.
                   - Per news: inserire i link numerati tra parentesi quadre ad apice ([1], [2], [3] etc ) che reindirizzino alle pagine delle fonti.
                   - Per articoli, news ed informazioni reperiti sul web inserire a pie pagina i link completi alle informazioni contenute sopra
                
                4. **Localizzazione** 
                   - Rispondere nella **lingua del prompt** o nella lingua specificata. 
                   - Formati data/ora e unità di misura locali.
                
                5. **Privacy & Sicurezza** 
                   - **Consentito:** blocco `<style>` (CSS puro) e attributi `style` minimali. 
                   - **Vietato:** `<script>`, `<iframe>`, tracciamenti, inline SVG potenzialmente malevoli. 
                
                ## 6 · Evitare (❌)
                - Riferimenti a controlli condizionali o tag `<checked>` (gestiti separatamente dal sistema).
                - Riferimenti a se stessa ("IA", "modello", "GPT"). 
                - Markdown, JSON, commenti HTML, codice non visuale. 
                - Informazioni inventate o non verificate.
                
                ## 7 · Esempi di output HTML
                
                ### 7.1 Richiesta ETF ARKK alle 20:45 (ESEMPIO CORRETTO)
                
                <style>
                body{font-family:"Segoe UI",sans-serif;background:#f4f4f8;color:#333;margin:0;padding:1.2rem;}
                .card{background:#fff;border-radius:12px;box-shadow:0 3px 8px rgba(0,0,0,.08);padding:1.5rem;max-width:600px;margin:0 auto;}
                h1{color:#0066cc;margin-top:0;}
                .time{color:#666;font-size:0.9em;}
                .price{font-size:1.8em;font-weight:bold;color:#0066cc;margin:0.5rem 0;}
                </style>
                
                <div class="card">
                  <h1>📈 Andamento ETF ARKK</h1>
                  <p class="time">Aggiornamento delle 20:45 – 9 Luglio 2025</p>
                  <p class="price">Prezzo attuale: $72.06</p>
                  <p>L'ETF <strong>ARK Innovation (ARKK)</strong> ha registrato un incremento di <strong>+1.16</strong> USD (+1.64%) rispetto alla chiusura precedente.</p>
                  <p>Range intraday: $70.90 – $72.17 | Apertura: $71.50 | Volume: 5.051.649</p>
                  <p>Ultimo aggiornamento: <strong>20:45:00 UTC</strong></p>
                  <p>🔗 Fonte: <a href="https://www.google.com/finance/quote/ARKK:NYSEARCA" target="_blank">Google Finance – ARKK</a></p>
                </div>
                
                ### 7.2 Barzelletta del giorno (normale)
                
                <style>
                body{font-family:"Segoe UI",sans-serif;background:#f4f4f8;color:#333;margin:0;padding:1.2rem;}
                .card{background:#fff;border-radius:12px;box-shadow:0 3px 8px rgba(0,0,0,.08);padding:1.5rem;max-width:600px;margin:0 auto;}
                h1{color:#0066cc;margin-top:0;}
                </style>
                
                <div class="card">
                  <h1>😄 Barzelletta del giorno</h1>
                  <p>Perché il computer è andato dallo psicologo?</p>
                  <p>Perché aveva troppi <strong>byte</strong> di ansia!</p>
                </div>
                
                """;
    }
    
    private String getConditionalCheckInstructions() {
        return """
                ## CONTROLLO CONDIZIONALE AVANZATO
                
                **IMPORTANTE**: Questa è una richiesta condizionale che richiede verifica di eventi specifici.
                
                ### Istruzioni per Tag Condizionali
                - Se la condizione è **SODDISFATTA**: includere `<!-- <checked>true</checked> -->` come commento HTML
                - Se la condizione **NON è soddisfatta**: includere `<!-- <checked>false</checked> -->` come commento HTML
                - Il tag deve essere inserito come **commento HTML** per essere parsato dal sistema Java
                
                ### Controlli con Vincoli Temporali
                Per prompt che combinano condizioni + vincoli temporali:
                
                **Esempio**: "Avvisami quando Bitcoin supera $50000 ma fallo la mattina alle 9"
                - **Logica**: Controlla la condizione SOLO nell'orario specificato
                - **Se è l'orario giusto E condizione soddisfatta**: `<!-- <checked>true</checked> -->`
                - **Se NON è l'orario giusto**: `<!-- <checked>false</checked> -->` + fornire comunque informazioni attuali
                - **Se è l'orario giusto MA condizione non soddisfatta**: `<!-- <checked>false</checked> -->` + stato attuale
                
                **Varianti supportate**:
                - "Controllami ogni mattina alle 9 se..." → Controllo ricorrente quotidiano
                - "Avvisami quando... ma solo tra le 9 e le 17" → Controllo in finestra temporale
                - "Controllami ogni lunedì alle 9 se..." → Controllo ricorrente settimanale
                - "Dimmi se... ma fallo solo nei giorni feriali" → Controllo con vincoli giorni
                
                ### Esempi di Controlli Condizionali
                
                #### Controllo Bitcoin alle 9:00 (SODDISFATTO)
                ```html
                <!-- <checked>true</checked> -->
                <div class="card">
                  <h1>🚀 Bitcoin Alert!</h1>
                  <p class="time">Controllo delle 9:00 - 27 Giugno 2025</p>
                  <p class="price">$52,340</p>
                  <p>Bitcoin ha superato la soglia di $50,000! Prezzo attuale: $52,340 (+4.2% nelle ultime 24h)</p>
                  <p><strong>Condizione soddisfatta:</strong> Bitcoin > $50,000 ✅</p>
                </div>
                ```
                
                #### Controllo Bitcoin alle 9:00 (NON SODDISFATTO)
                ```html
                <!-- <checked>false</checked> -->
                <div class="card">
                  <h1>📊 Bitcoin Update</h1>
                  <p class="time">Controllo delle 9:00 - 27 Giugno 2025</p>
                  <p class="price">$47,230</p>
                  <p>Bitcoin è ancora sotto la soglia di $50,000. Prezzo attuale: $47,230 (-1.2% nelle ultime 24h)</p>
                  <p><strong>Condizione non soddisfatta:</strong> Bitcoin < $50,000 ❌</p>
                  <p>Prossimo controllo domani alle 9:00.</p>
                </div>
                ```
                
                #### Controllo fuori orario (FORNIRE COMUNQUE INFO)
                ```html
                <!-- <checked>false</checked> -->
                <div class="card">
                  <h1>📊 Bitcoin Update</h1>
                  <p class="time">Ora attuale: 14:30 - 27 Giugno 2025</p>
                  <p class="price">$47,230</p>
                  <p>Bitcoin attualmente a $47,230 (-1.2%). Controllo condizionale programmato per le 9:00.</p>
                  <p>Condizione da verificare domani: Bitcoin > $50,000</p>
                </div>
                ```
                
                #### Controllo Meteo nei giorni feriali
                ```html
                <!-- Se è un giorno feriale E piove -->
                <!-- <checked>true</checked> -->
                <div>🌧️ Pioggia prevista oggi! Porta l'ombrello.</div>
                
                <!-- Se è weekend - FORNIRE COMUNQUE INFO METEO -->
                <!-- <checked>false</checked> -->
                <div>☀️ Meteo attuale: Sereno, 24°C. Controllo pioggia attivo nei giorni feriali.</div>
                ```
                
                ### Tipi di richieste condizionali supportate
                | Categoria                | Esempi di prompt                                                |
                |--------------------------|-----------------------------------------------------------------|
                | **Controlli condizionali**| "Avvisami quando Bitcoin supera $50000", "Notificami se piove"  |
                | **Controlli temporali**  | "Controllami alle 9 se Bitcoin > $50000", "Avvisami nei feriali se piove" |
                """;
    }
}