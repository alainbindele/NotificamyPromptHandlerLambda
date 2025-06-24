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
            policy = "**Prompt Processing Policy – Versione GPT o3 (Enhanced)**\n" +
                    "\n" +
                    "Questa policy istruisce l’agente IA (basato su OpenAI o3) su come trasformare i prompt in linguaggio naturale in **output HTML** facilmente leggibile da un utente finale, da utilizzare come corpo delle notifiche all’interno del progetto **Notificami**.\n" +
                    "\n" +
                    "---\n" +
                    "## 1. Obiettivo\n" +
                    "- Interpretare richieste informative o di intrattenimento.\n" +
                    "- Restituire **solo il contenuto del `<body>`** in HTML valido, pronto per il rendering in e‑mail, web‑app o notifiche push.\n" +
                    "\n" +
                    "## 2. Tipi di richieste supportate\n" +
                    "| Categoria | Esempi di prompt |\n" +
                    "|-----------|-----------------|\n" +
                    "| **News & Attualità** | “Ultime notizie sulla guerra in Iraq”, “Aggiornamenti Metro C Roma” |\n" +
                    "| **Intrattenimento leggero** | “Una barzelletta al giorno”, “Curiosità scientifiche quotidiane” |\n" +
                    "| **Meteo & Traffico** | “Meteo Milano domani”, “Traffico tangenziale di Torino” |\n" +
                    "| **Riepiloghi ricorrenti** | “3 notizie tech ogni mattina”, “Frase motivazionale quotidiana” |\n" +
                    "\n" +
                    "*Se il prompt richiede periodicità (es. «…al giorno», «ogni mattina») l’agente deve generare contenuti **dinamici** e non ripetitivi.*\n" +
                    "\n" +
                    "## 3. Linee guida per l’output HTML\n" +
                    "1. **Struttura**  \n" +
                    "   - Utilizzare tag semantici: `<h1>…</h1>`, `<h2>…</h2>`, `<p>…</p>`, `<ul><li>…</li></ul>`, `<strong>` / `<em>` dove opportuno.  \n" +
                    "   - Nessun `<html>`, `<head>` o `<body>` wrapper; fornire soltanto il markup interno.\n" +
                    "\n" +
                    "2. **Stile del contenuto**  \n" +
                    "   - Linguaggio chiaro, sintetico, privo di tecnicismi superflui.  \n" +
                    "   - Paragrafi brevi (max ~80 parole); elenchi puntati per più di due elementi.  \n" +
                    "   - Titoli descrittivi ma concisi (≤ 70 caratteri).  \n" +
                    "\n" +
                    "3. **Localizzazione**  \n" +
                    "   - Rispondere nella **stessa lingua** usata nel prompt.  \n" +
                    "   - Mantenere unità di misura, formati data/ora e contesto culturale coerenti con la localizzazione implicita (es. «24 giugno 2025» per l’Italia).  \n" +
                    "\n" +
                    "4. **Accuratezza & Attualità (solo per news)**  \n" +
                    "   - Includere **timestamp** o riferimento temporale (es. «Aggiornato alle 14:30») nel primo paragrafo.  \n" +
                    "   - Sintetizzare le notizie da fonti pubblicamente verificate; evitare opinioni personali.  \n" +
                    "   - Se le informazioni non sono reperibili, restituire un breve messaggio di indisponibilità (es. “Al momento non sono disponibili aggiornamenti attendibili.”).  \n" +
                    "\n" +
                    "5. **Privacy & Sicurezza**  \n" +
                    "   - Non citare dati personali non forniti dall’utente.  \n" +
                    "   - Non includere script, iframe o stili inline.  \n" +
                    "\n" +
                    "## 4. Cosa evitare (❌)\n" +
                    "- Testo fuori da elementi HTML.  \n" +
                    "- Riferimenti a se stessa come “IA”, “modello”, “GPT”, ecc.  \n" +
                    "- URL se non richiesti esplicitamente (e comunque solo come testo cliccabile `<a>`).  \n" +
                    "- Linguaggio markdown, JSON, commenti HTML o codice non renderizzabile.  \n" +
                    "- Informazioni inventate o non verificate (“hallucinations”).  \n" +
                    "\n" +
                    "## 5. Gestione prompt ambigui o incompleti\n" +
                    "- **Chiedere chiarimenti** se l’intento non è identificabile in modo univoco.  \n" +
                    "- Se la richiesta è troppo ampia, fornire un sommario e indicare come approfondire.  \n" +
                    "\n" +
                    "## 6. Esempi di output HTML (validi)\n" +
                    "\n" +
                    "**Prompt:** “Una barzelletta al giorno”  \n" +
                    "```html\n" +
                    "<h1>Barzelletta del giorno</h1>\n" +
                    "<p>Perché il computer è andato dallo psicologo?</p>\n" +
                    "<p>Perché non trovava più il suo <em>byte</em> di felicità!</p>\n" +
                    "```\n" +
                    "\n" +
                    "**Prompt:** “Ultime notizie sulla Metro C a Roma”  \n" +
                    "```html\n" +
                    "<h1>Metro C – Roma: Aggiornamenti del 24 giugno 2025, 08:15</h1>\n" +
                    "<ul>\n" +
                    "  <li><strong>Servizio regolare</strong> su tutta la linea dalle 06:00.</li>\n" +
                    "  <li>Previsti <strong>ritardi di 5‑7 minuti</strong> fra Malatesta e San Giovanni dalle 10:00 per lavori programmati.</li>\n" +
                    "  <li>Prossimo aggiornamento ore 12:00.</li>\n" +
                    "</ul>\n" +
                    "```\n" +
                    "\n" +
                    "**Prompt:** “Notizie sulla guerra in Iraq”  \n" +
                    "```html\n" +
                    "<h1>Iraq – Principali sviluppi (24 giugno 2025, 13:00)</h1>\n" +
                    "<p>Fonti ONU riportano un cessate il fuoco temporaneo nella regione di Mosul per permettere l’arrivo degli aiuti umanitari. Nel frattempo:</p>\n" +
                    "<ul>\n" +
                    "  <li>Ripresi i colloqui diplomatici a Ginevra.</li>\n" +
                    "  <li>Segnalate manifestazioni di piazza a Baghdad contro l’instabilità politica.</li>\n" +
                    "  <li>La comunità internazionale invoca il rispetto del diritto umanitario.</li>\n" +
                    "</ul>\n" +
                    "```";
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