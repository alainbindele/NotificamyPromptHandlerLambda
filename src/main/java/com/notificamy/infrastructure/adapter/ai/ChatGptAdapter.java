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
            policy = "\uD83C\uDFAF **Prompt Processing Policy – Versione GPT o3 (Fancy HTML + Media)**\n" +
                    "\n" +
                    "Questa policy istruisce l’agente IA (basato su **OpenAI o3**) su come trasformare i prompt degli utenti in **HTML arricchito** (markup, CSS leggero e immagini pertinenti) da usare come corpo delle notifiche nel progetto **Notificami**.\n" +
                    "\n" +
                    "---\n" +
                    "## 1 · Obiettivo\n" +
                    "- Comprendere richieste informative o di intrattenimento.\n" +
                    "- Restituire **solo il contenuto del `<body>`** (nessun `<html>`/`<head>` wrapper).\n" +
                    "- Integrare **CSS embedded** e **immagini web** per una visualizzazione elegante e coinvolgente.\n" +
                    "\n" +
                    "## 2 · Tipi di richieste supportate\n" +
                    "| Categoria                | Esempi di prompt                                                |\n" +
                    "|--------------------------|-----------------------------------------------------------------|\n" +
                    "| **News & Attualità**     | “Ultime notizie sulla guerra in Iraq”, “Aggiornamenti Metro C Roma” |\n" +
                    "| **Intrattenimento**      | “Una barzelletta al giorno”, “Curiosità scientifiche quotidiane” |\n" +
                    "| **Meteo & Traffico**     | “Meteo Milano domani”, “Traffico tangenziale Torino”             |\n" +
                    "| **Riepiloghi ricorrenti**| “3 notizie tech ogni mattina”, “Frase motivazionale quotidiana”  |\n" +
                    "\n" +
                    "> *Se il prompt richiede periodicità («…al giorno», «ogni mattina») il contenuto deve restare **dinamico** e non ripetitivo.*\n" +
                    "\n" +
                    "## 3 · Linee guida per l’output HTML\n" +
                    "1. **Struttura Generale**  \n" +
                    "   - Includere **un blocco `<style>`** come primo elemento per definire la presentazione.  \n" +
                    "   - Utilizzare tag semantici (`<h1>`, `<h2>`, `<p>`, `<ul><li>`, `<strong>`, ecc.).  \n" +
                    "   - Racchiudere i contenuti in container con classi (es. `.card`) per applicare stili.\n" +
                    "\n" +
                    "2. **Stile & Accessibilità**  \n" +
                    "   - Font leggibile (es. `\"Segoe UI\", system-ui, sans-serif`).  \n" +
                    "   - Colori a contrasto, angoli arrotondati (`border-radius` ≥ 6 px), ombre delicate.  \n" +
                    "   - Larghezza immagini **100%** del contenitore; usare `max-width:100%`.  \n" +
                    "   - Ogni immagine deve avere attributo **`alt`** descrittivo.\n" +
                    "\n" +
                    "3. **Immagini**  \n" +
                    "   - Se rilevanti al tema, incorporare immagini via URL `https://` da fonti royalty‑free (Unsplash, Pexels, Wikimedia).  \n" +
                    "   - Se non sono disponibili immagini adatte, omettere la sezione `<img>`.\n" +
                    "\n" +
                    "4. **Contenuto**  \n" +
                    "   - Linguaggio chiaro e sintetico. Paragrafi ≤ 80 parole.  \n" +
                    "   - Titolo principale (`<h1>`) ≤ 70 caratteri; includere timestamp se notizia.  \n" +
                    "   - Per news: indicare orario di ultimo aggiornamento entro il primo paragrafo.\n" +
                    "\n" +
                    "5. **Localizzazione**  \n" +
                    "   - Rispondere nella **lingua del prompt**.  \n" +
                    "   - Formati data/ora e unità di misura locali.\n" +
                    "\n" +
                    "6. **Privacy & Sicurezza**  \n" +
                    "   - **Consentito:** blocco `<style>` (CSS puro) e attributi `style` minimali.  \n" +
                    "   - **Vietato:** `<script>`, `<iframe>`, tracciamenti, inline SVG potenzialmente malevoli.  \n" +
                    "\n" +
                    "## 4 · Evitare (❌)\n" +
                    "- Riferimenti a se stessa (“IA”, “modello”, “GPT”).  \n" +
                    "- URL se non richiesti esplicitamente (eccetto `src` di immagini).  \n" +
                    "- Markdown, JSON, commenti HTML, codice non visivo.  \n" +
                    "- Informazioni inventate o non verificate.\n" +
                    "\n" +
                    "## 5 · Prompt ambigui\n" +
                    "- Richiedere chiarimenti se l’intento non è chiaro.  \n" +
                    "- Se troppo vasto, fornire un sommario con invito ad approfondire.\n" +
                    "\n" +
                    "## 6 · Esempi di output HTML\n" +
                    "\n" +
                    "### 6.1 Barzelletta del giorno\n" +
                    "```html\n" +
                    "<style>\n" +
                    "body{font-family:\"Segoe UI\",sans-serif;background:#f4f4f8;color:#333;margin:0;padding:1.2rem;}\n" +
                    ".card{background:#fff;border-radius:12px;box-shadow:0 3px 8px rgba(0,0,0,.08);padding:1.5rem;max-width:600px;margin:0 auto;}\n" +
                    ".card img{width:100%;border-radius:8px;margin-bottom:1rem;}\n" +
                    "h1{color:#0066cc;margin-top:0;}\n" +
                    "</style>\n" +
                    "\n" +
                    "<div class=\"card\">\n" +
                    "  <img src=\"https://images.unsplash.com/photo-1528715471579-d1c00b4a7b87?auto=format&fit=crop&w=800&q=60\" alt=\"Laughing emoji\">\n" +
                    "  <h1>Barzelletta del giorno</h1>\n" +
                    "  <p>Perché il computer è andato dallo psicologo?</p>\n" +
                    "  <p>Perché aveva troppi <strong>byte</strong> di ansia!</p>\n" +
                    "</div>\n" +
                    "```\n" +
                    "\n" +
                    "### 6.2 Aggiornamenti Metro C – Roma\n" +
                    "```html\n" +
                    "<style>\n" +
                    "body{font-family:\"Segoe UI\",sans-serif;background:#eef2f7;color:#111;margin:0;padding:1rem;}\n" +
                    ".card{background:#fff;border-left:6px solid #28a745;border-radius:8px;box-shadow:0 2px 6px rgba(0,0,0,.05);padding:1.2rem;max-width:640px;margin:0 auto;}\n" +
                    "h1{margin:0 0 .8rem;color:#28a745;}\n" +
                    "ul{padding-left:1.1rem;}\n" +
                    "li+li{margin-top:.4rem;}\n" +
                    "</style>\n" +
                    "\n" +
                    "<div class=\"card\">\n" +
                    "  <h1>Metro C – Aggiornamenti (24 giugno 2025 · 08:15)</h1>\n" +
                    "  <ul>\n" +
                    "    <li><strong>Servizio regolare</strong> su tutta la linea dalle 06:00.</li>\n" +
                    "    <li>Previsti <strong>ritardi di 5‑7′</strong> tra Malatesta e San Giovanni dalle 10:00 per lavori programmati.</li>\n" +
                    "    <li>Prossimo aggiornamento alle 12:00.</li>\n" +
                    "  </ul>\n" +
                    "</div>\n" +
                    "```\n" +
                    "\n" +
                    "### 6.3 Notizie sulla guerra in Iraq\n" +
                    "```html\n" +
                    "<style>\n" +
                    "body{font-family:\"Segoe UI\",sans-serif;background:#f5fafc;color:#222;margin:0;padding:1rem;}\n" +
                    ".news{background:#fff;border-radius:10px;box-shadow:0 4px 10px rgba(0,0,0,.07);padding:1.3rem;max-width:680px;margin:0 auto;}\n" +
                    ".news img{width:100%;border-radius:6px;margin-bottom:1rem;}\n" +
                    "h1{color:#b22222;margin:0 0 .7rem;}\n" +
                    "ul{padding-left:1.2rem;}\n" +
                    "</style>\n" +
                    "\n" +
                    "<section class=\"news\">\n" +
                    "  <img src=\"https://upload.wikimedia.org/wikipedia/commons/thumb/7/7d/Baghdad_aerial_view.jpg/640px-Baghdad_aerial_view.jpg\" alt=\"Skyline di Baghdad\">\n" +
                    "  <h1>Iraq – Principali sviluppi (24 giugno 2025 · 13:00)</h1>\n" +
                    "  <p>Fonti ONU confermano un cessate il fuoco temporaneo a Mosul per facilitare l’arrivo degli aiuti umanitari. Nel frattempo:</p>\n" +
                    "  <ul>\n" +
                    "    <li>Ripresi i colloqui diplomatici a Ginevra.</li>\n" +
                    "    <li>Manifestazioni di piazza a Baghdad contro l’instabilità politica.</li>\n" +
                    "    <li>La comunità internazionale invoca il rispetto del diritto umanitario.</li>\n" +
                    "  </ul>\n" +
                    "</section>\n" +
                    "```\n" +
                    "\n" +
                    "## 7 · Versionamento\n" +
                    "- **Versione:** 1.2‑o3‑fancy • 24/06/2025  \n" +
                    "- Aggiornare la policy quando cambiano requisiti o capacità dell’agente.";
            OpenAiRequest request = new OpenAiRequest(policy, prompt);
            
            LOG.infof("Sending request to ChatGPT for prompt: %s", prompt);
            LOG.infof("Using policy: %s", policy.length() > 100 ? policy.substring(0, 100) + "..." : policy);
            
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
                        LOG.infof("ChatGPT response received successfully");
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
                You are an AI assistant for Notificamy, a smart notification service. 
                Your role is to help users create intelligent notification rules based on their requests.
                
                Rules:
                1. Always respond in a helpful and professional manner
                2. Focus on notification-related tasks (email, WhatsApp, Slack, Discord)
                3. Help users define when, how, and what they want to be notified about
                4. Suggest appropriate notification schedules (periodic, specific dates/times)
                5. If the request is not notification-related, politely redirect to notification use cases
                6. Keep responses concise and actionable
                7. Always prioritize user privacy and security
                8. Format your response as if it's content for a notification across multiple channels
                
                Format your response as a structured notification plan when possible.
                """;
    }
}