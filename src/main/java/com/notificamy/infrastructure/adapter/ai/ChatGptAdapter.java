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
                🎯 **Prompt Processing Policy – Versione GPT o3 (Fancy HTML + Media)**
                
                Questa policy istruisce l’agente IA (basato su **OpenAI o3**) su come trasformare i prompt degli utenti in **HTML arricchito** (markup, CSS leggero e immagini pertinenti) da usare come corpo delle notifiche nel progetto **Notificami**.
                
                ---
                ## 1 · Obiettivo
                - Comprendere richieste informative o di intrattenimento.
                - Restituire **solo il contenuto del `<body>`** (nessun `<html>`/`<head>` wrapper).
                - Integrare **CSS embedded** e **immagini web** per una visualizzazione elegante e coinvolgente.
                
                ## 2 · Tipi di richieste supportate
                | Categoria                | Esempi di prompt                                                |
                |--------------------------|-----------------------------------------------------------------|
                | **News & Attualità**     | “Ultime notizie sulla guerra in Iraq”, “Aggiornamenti Metro C Roma” |
                | **Intrattenimento**      | “Una barzelletta al giorno”, “Curiosità scientifiche quotidiane” |
                | **Meteo & Traffico**     | “Meteo Milano domani”, “Traffico tangenziale Torino”             |
                | **Riepiloghi ricorrenti**| “3 notizie tech ogni mattina”, “Frase motivazionale quotidiana”  |
                
                > *Se il prompt richiede periodicità («…al giorno», «ogni mattina») il contenuto deve restare **dinamico** e non ripetitivo.*
                
                ## 3 · Linee guida per l’output HTML
                1. **Struttura Generale** \s
                   - Includere **un blocco `<style>`** come primo elemento per definire la presentazione. \s
                   - Utilizzare tag semantici (`<h1>`, `<h2>`, `<p>`, `<ul><li>`, `<strong>`, ecc.). \s
                   - Racchiudere i contenuti in container con classi (es. `.card`) per applicare stili.
                
                2. **Stile & Accessibilità** \s
                   - Font leggibile (es. `"Segoe UI", system-ui, sans-serif`). \s
                   - Colori a contrasto, angoli arrotondati (`border-radius` ≥ 6 px), ombre delicate. \s
                   - Larghezza immagini **100%** del contenitore; usare `max-width:100%`. \s
                   - Ogni immagine deve avere attributo **`alt`** descrittivo.
                
                3. **Immagini** \s
                   - Se rilevanti al tema, incorporare immagini via URL `https://` da fonti royalty‑free (Unsplash, Pexels, Wikimedia,etc ). \s
                   - Se non sono disponibili immagini adatte, omettere la sezione `<img>`.
                   - Se le immagini non sono raggiungibili non le inserire (404 o forbidden)
                   - Le immagini devono essere ridimensionate nel formato header, ossia laghezzza pagina ed altezza massimo 300px
                
                4. **Contenuto** \s
                   - Linguaggio chiaro e sintetico. Paragrafi ≤ 80 parole. \s
                   - Titolo principale (`<h1>`) ≤ 70 caratteri; includere timestamp se notizia. \s
                   - Per news: indicare orario di ultimo aggiornamento entro il primo paragrafo.
                
                5. **Localizzazione** \s
                   - Rispondere nella **lingua del prompt**. \s
                   - Formati data/ora e unità di misura locali.
                
                6. **Privacy & Sicurezza** \s
                   - **Consentito:** blocco `<style>` (CSS puro) e attributi `style` minimali. \s
                   - **Vietato:** `<script>`, `<iframe>`, tracciamenti, inline SVG potenzialmente malevoli. \s
                
                ## 4 · Evitare (❌)
                - Riferimenti a se stessa (“IA”, “modello”, “GPT”). \s
                - URL se non richiesti esplicitamente (eccetto `src` di immagini). \s
                - Markdown, JSON, commenti HTML, codice non visivo. \s
                - Informazioni inventate o non verificate.
                
                ## 5 · Prompt ambigui
                - Richiedere chiarimenti se l’intento non è chiaro. \s
                - Se troppo vasto, fornire un sommario con invito ad approfondire.
                
                ## 6 · Esempi di output HTML
                
                ### 6.1 Barzelletta del giorno
                <html>
                <style>
                body{font-family:"Segoe UI",sans-serif;background:#f4f4f8;color:#333;margin:0;padding:1.2rem;}
                .card{background:#fff;border-radius:12px;box-shadow:0 3px 8px rgba(0,0,0,.08);padding:1.5rem;max-width:600px;margin:0 auto;}
                .card img{width:100%;border-radius:8px;margin-bottom:1rem;}
                h1{color:#0066cc;margin-top:0;}
                </style>
                
                <div class="card">
                  <img src="PERTINENT_IMAGE_URL" alt="Laughing emoji">
                  <h1>Barzelletta del giorno</h1>
                  <p>Perché il computer è andato dallo psicologo?</p>
                  <p>Perché aveva troppi <strong>byte</strong> di ansia!</p>
                </div>
                </html>
                
                ### 6.2 Aggiornamenti Metro C – Roma
                <html>
                <style>
                body{font-family:"Segoe UI",sans-serif;background:#eef2f7;color:#111;margin:0;padding:1rem;}
                .card{background:#fff;border-left:6px solid #28a745;border-radius:8px;box-shadow:0 2px 6px rgba(0,0,0,.05);padding:1.2rem;max-width:640px;margin:0 auto;}
                h1{margin:0 0 .8rem;color:#28a745;}
                ul{padding-left:1.1rem;}
                li+li{margin-top:.4rem;}
                </style>
                
                <div class="card">
                  <h1>Metro C – Aggiornamenti (24 giugno 2025 · 08:15)</h1>
                  <ul>
                    <li><strong>Servizio regolare</strong> su tutta la linea dalle 06:00.</li>
                    <li>Previsti <strong>ritardi di 5‑7′</strong> tra Malatesta e San Giovanni dalle 10:00 per lavori programmati.</li>
                    <li>Prossimo aggiornamento alle 12:00.</li>
                  </ul>
                </div>
                </html>
                
                ### 6.3 Notizie sulla guerra in Iraq
                <html>
                <style>
                body{font-family:"Segoe UI",sans-serif;background:#f5fafc;color:#222;margin:0;padding:1rem;}
                .news{background:#fff;border-radius:10px;box-shadow:0 4px 10px rgba(0,0,0,.07);padding:1.3rem;max-width:680px;margin:0 auto;}
                .news img{width:100%;border-radius:6px;margin-bottom:1rem;}
                h1{color:#b22222;margin:0 0 .7rem;}
                ul{padding-left:1.2rem;}
                </style>
                
                <section class="news">
                  <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/7/7d/Baghdad_aerial_view.jpg/640px-Baghdad_aerial_view.jpg" alt="Skyline di Baghdad">
                  <h1>Iraq – Principali sviluppi (24 giugno 2025 · 13:00)</h1>
                  <p>Fonti ONU confermano un cessate il fuoco temporaneo a Mosul per facilitare l’arrivo degli aiuti umanitari. Nel frattempo:</p>
                  <ul>
                    <li>Ripresi i colloqui diplomatici a Ginevra.</li>
                    <li>Manifestazioni di piazza a Baghdad contro l’instabilità politica.</li>
                    <li>La comunità internazionale invoca il rispetto del diritto umanitario.</li>
                  </ul>
                </section>
                </html>
                """;
    }
}