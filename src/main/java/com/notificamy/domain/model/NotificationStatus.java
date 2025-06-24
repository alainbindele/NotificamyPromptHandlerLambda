package com.notificamy.domain.model;

public enum NotificationStatus {
    PENDING,     // In attesa di essere inviata
    SUCCESS,     // Inviata con successo
    FAILED,      // Fallita (errore temporaneo, può essere ritentata)
    ERROR,       // Errore permanente (non ritentare)
    PARTIAL,     // Parzialmente inviata (alcuni canali ok, altri no)
    CANCELLED    // Cancellata dall'utente
}