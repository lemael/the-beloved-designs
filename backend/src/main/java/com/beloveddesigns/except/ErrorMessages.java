package com.beloveddesigns.except;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static final String INVALID_CREDENTIALS = "Numéro WhatsApp ou mot de passe incorrect";
    public static final String VALIDATION_ERROR = "Données invalides";
    public static final String INTERNAL_ERROR = "Une erreur inattendue est survenue";
    public static final String PHONE_ALREADY_USED = "Un compte existe déjà avec ce numéro WhatsApp";
    public static final String AUTHENTICATED_USER_NOT_FOUND = "Utilisateur authentifié introuvable";
    public static final String INVALID_PHONE_FORMAT =
            "Numéro WhatsApp invalide (format international requis, ex: +237612345678)";
    public static final String PASSWORD_TOO_SHORT = "Le mot de passe doit contenir au moins 8 caractères";

    public static String userNotFound(String phone) {
        return "Utilisateur introuvable : " + phone;
    }

    public static String defaultRoleMissing(String role) {
        return "Rôle par défaut manquant : " + role;
    }

    public static String categoryNotFound(String slug) {
        return "Catégorie introuvable : " + slug;
    }

    public static String productNotFound(String slug) {
        return "Produit introuvable : " + slug;
    }
}
