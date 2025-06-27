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
        try {
            String apiKey = getOpenAiApiKey();
            
            LOG.infof("OpenAI API Key retrieved: %s", apiKey != null ? "***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) : "null");
            
            if (apiKey == null || apiKey.isEmpty()) {
                LOG.error("OpenAI API key not found in secrets");
                return "Sorry, the AI service is currently unavailable.";
            }
            
            String policy = buildPolicy();
            
            // Usa i parametri configurabili
            OpenAiRequest request = new OpenAiRequest(policy, prompt, maxTokens, temperature);
            
            LOG.infof("Sending request to ChatGPT for prompt: %s", prompt);
            LOG.infof("Using policy: %s", policy.length() > 100 ? policy.substring(0, 100) + "..." : policy);
            LOG.infof("ChatGPT parameters - Max tokens: %d, Temperature: %.2f", maxTokens, temperature);
            
            String requestBody = objectMapper.writeValueAsString(request);
            LOG.debugf("Request body: %s", requestBody);
            
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            
            LOG.infof("ChatGPT API response status: %d", response.statusCode());
            LOG.debugf("ChatGPT API response body: %s", response.body());
            
            if (response.statusCode() == 200) {
                ChatGptResponse chatGptResponse = objectMapper.readValue(response.body(), ChatGptResponse.class);
                
                if (chatGptResponse.getChoices() != null && !chatGptResponse.getChoices().isEmpty()) {
                    ChatGptResponse.Message message = chatGptResponse.getChoices().get(0).getMessage();
                    
                    // Controlla se c'è un refusal (rifiuto da parte di OpenAI)
                    if (message.getRefusal() != null && !message.getRefusal().isEmpty()) {
                        LOG.warnf("OpenAI refused the request: %s", message.getRefusal());
                        return "I'm sorry, but I cannot process this request due to content policy restrictions.";
                    }
                    
                    String content = message.getContent();
                    if (content != null && !content.isEmpty()) {
                        LOG.infof("ChatGPT response received successfully - Length: %d characters", content.length());
                        
                        // Log se la risposta potrebbe essere stata troncata
                        if (chatGptResponse.getChoices().get(0).getFinishReason() != null) {
                            String finishReason = chatGptResponse.getChoices().get(0).getFinishReason();
                            LOG.infof("ChatGPT finish reason: %s", finishReason);
                            
                            if ("length".equals(finishReason)) {
                                LOG.warnf("ChatGPT response was truncated due to max_tokens limit (%d). Consider increasing max_tokens.", maxTokens);
                            }
                        }
                        
                        return content;
                    } else {
                        LOG.error("Empty content in ChatGPT response");
                        return "Sorry, I couldn't generate a proper response at this time.";
                    }
                } else {
                    LOG.error("Empty choices in ChatGPT response");
                    return "Sorry, I couldn't process your request at this time.";
                }
            } else {
                LOG.errorf("ChatGPT API error: %d - %s", response.statusCode(), response.body());
                return "Sorry, there was an error processing your request.";
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Error calling ChatGPT API");
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
    
    private String buildPolicy() {
        // Se la policy è configurata e non è il placeholder, usala
        if (chatGptPolicy != null && !chatGptPolicy.equals("POLICY TEXT") && !chatGptPolicy.trim().isEmpty()) {
            LOG.infof("Using custom ChatGPT policy from configuration");
            return chatGptPolicy;
        }
        
        // Altrimenti usa la policy di default
        LOG.infof("Using default ChatGPT policy");
        return getDefaultPolicy();
    }
    
    private String getDefaultPolicy() {
        return """
                🎯 **Prompt Processing Policy – Versione GPT o3 (Fancy HTML + Media + Conditional Checking)**
                
                Questa policy istruisce l'agente IA (basato su **OpenAI o3**) su come trasformare i prompt degli utenti in **HTML arricchito** (markup, CSS leggero e immagini pertinenti) da usare come corpo delle notifiche nel progetto **Notificami**.
                
                ---
                ## 1 · Obiettivo
                - Comprendere richieste informative o di intrattenimento.
                - Restituire **solo il contenuto del `<body>`** (nessun `<html>`/`<head>` wrapper).
                - Integrare **CSS embedded** per una visualizzazione elegante e coinvolgente.
                - Cercare in tutti i modi di esaudire le richieste del prompt.
                - **IMPORTANTE**: Per richieste condizionali (che richiedono verifica), includere il tag `<checked>true</checked>` SOLO se la condizione è soddisfatta.
                
                ## 2 · Controllo Condizionale
                Per prompt che richiedono verifica di condizioni specifiche:
                - Se la condizione è **SODDISFATTA**: includere `<checked>true</checked>` nella risposta
                - Se la condizione **NON è soddisfatta**: includere `<checked>false</checked>` o omettere il tag
                - Esempi di condizioni: "avvisami quando Bitcoin supera $50000", "notificami se piove domani", "dimmi quando esce il nuovo iPhone"
                
                ## 3 · Tipi di richieste supportate
                | Categoria                | Esempi di prompt                                                |
                |--------------------------|-----------------------------------------------------------------|
                | **News & Attualità**     | "Ultime notizie sulla guerra in Iraq", "Aggiornamenti Metro C Roma" |
                | **Intrattenimento**      | "Una barzelletta al giorno", "Curiosità scientifiche quotidiane" |
                | **Meteo & Traffico**     | "Meteo Milano domani", "Traffico tangenziale Torino"             |
                | **Riepiloghi ricorrenti**| "3 notizie tech ogni mattina", "Frase motivazionale quotidiana"  |
                | **Controlli condizionali**| "Avvisami quando Bitcoin supera $50000", "Notificami se piove"  |
                
                ## 4 · Tipi di richieste NON supportate
                | Categoria                | Esempi di prompt                                                |
                |--------------------------|-----------------------------------------------------------------|
                | **Sesso e discriminazione**| "Ultime notizie sulle pornostar irachene transex"               |
                | **Argomenti criminali**   | "Dimmi se posso fare bombe fatte in casa a meno di 100$"       |
                | **Argomenti terroristici**| "Notificami se il mio terrorista preferito ha ucciso 1000 persone domani alle 10"  |
                | **Argomenti assurdi o irrazionali**| "Notificami se il gomito mi è andato sulla luna"  |
                
                > *Se il prompt non rientra nelle categorie richieste rispondi con un html fatto così:
                
                ```html
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
                ```
                
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
                   - Per news: indicare orario di ultimo aggiornamento entro il primo paragrafo.
                   - Per news: inserire i link numerati tra parentesi quadre ad apice ([1], [2], [3] etc ) che reindirizzino alle pagine delle fonti.
                   - Per articoli, news ed informazioni reperiti sul web inserire a pie pagina i link completi alle informazioni contenute sopra
                
                4. **Localizzazione** 
                   - Rispondere nella **lingua del prompt** o nella lingua specificata. 
                   - Formati data/ora e unità di misura locali.
                
                5. **Privacy & Sicurezza** 
                   - **Consentito:** blocco `<style>` (CSS puro) e attributi `style` minimali. 
                   - **Vietato:** `<script>`, `<iframe>`, tracciamenti, inline SVG potenzialmente malevoli. 
                
                ## 6 · Evitare (❌)
                - Riferimenti a se stessa ("IA", "modello", "GPT"). 
                - Markdown, JSON, commenti HTML, codice non visuale. 
                - Informazioni inventate o non verificate.
                
                ## 7 · Esempi di output HTML
                
                ### 7.1 Controllo condizionale - Bitcoin
                ```html
                <style>
                body{font-family:"Segoe UI",sans-serif;background:#f4f4f8;color:#333;margin:0;padding:1.2rem;}
                .card{background:#fff;border-radius:12px;box-shadow:0 3px 8px rgba(0,0,0,.08);padding:1.5rem;max-width:600px;margin:0 auto;}
                h1{color:#f7931a;margin-top:0;}
                .price{font-size:2em;font-weight:bold;color:#f7931a;}
                </style>
                
                <div class="card">
                  <h1>🚀 Bitcoin Alert!</h1>
                  <p class="price">$52,340</p>
                  <p>Bitcoin ha superato la soglia di $50,000! Prezzo attuale: $52,340 (+4.2% nelle ultime 24h)</p>
                  <checked>true</checked>
                </div>
                ```
                
                ### 7.2 Barzelletta del giorno
                ```html
                <style>
                body{font-family:"Segoe UI",sans-serif;background:#f4f4f8;color:#333;margin:0;padding:1.2rem;}
                .card{background:#fff;border-radius:12px;box-shadow:0 3px 8px rgba(0,0,0,.08);padding:1.5rem;max-width:600px;margin:0 auto;}
                h1{color:#0066cc;margin-top:0;}
                </style>
                
                <div class="card">
                  <h1>Barzelletta del giorno</h1>
                  <p>Perché il computer è andato dallo psicologo?</p>
                  <p>Perché aveva troppi <strong>byte</strong> di ansia!</p>
                </div>
                ```
                
                """;
    }
}