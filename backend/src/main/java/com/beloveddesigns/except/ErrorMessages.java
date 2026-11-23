package com.beloveddesigns.except;

public final class ErrorMessages {

    private ErrorMessages() {
    }

    public static final String INVALID_CREDENTIALS = "Numéro WhatsApp ou mot de passe incorrect";
    public static final String VALIDATION_ERROR = "Données invalides";
    public static final String MALFORMED_REQUEST = "Requête illisible : vérifiez le format des données envoyées";
    public static final String INTERNAL_ERROR = "Une erreur inattendue est survenue";
    public static final String PHONE_ALREADY_USED = "Un compte existe déjà avec ce numéro WhatsApp";
    public static final String AUTHENTICATED_USER_NOT_FOUND = "Utilisateur authentifié introuvable";
    public static final String INVALID_PHONE_FORMAT =
            "Numéro WhatsApp invalide (format international requis, ex: +237****5678)";
    public static final String PASSWORD_TOO_SHORT = "Le mot de passe doit contenir au moins 8 caractères";
    public static final String TOKEN_INVALID = "Jeton de rafraîchissement invalide";
    public static final String TOKEN_EXPIRED = "Jeton d'accès expiré";

    // ---- Back-office admin ----
    public static final String PRODUCT_NAME_REQUIRED = "Le nom du produit est obligatoire";
    public static final String PRODUCT_PRICE_REQUIRED = "Le prix est obligatoire";
    public static final String PRODUCT_PRICE_POSITIVE = "Le prix doit être supérieur à 0";
    public static final String STOCK_NOT_NEGATIVE = "Le stock ne peut pas être négatif";
    public static final String PRODUCT_CATEGORY_REQUIRED = "Choisissez au moins une catégorie";
    public static final String INVALID_SLUG_FORMAT =
            "Le slug ne doit contenir que des minuscules, chiffres et tirets (ex: sac-cuir-noir)";
    public static final String RESTOCK_DATE_REQUIRED = "La date de ravitaillement prévue est obligatoire";
    public static final String RESTOCK_ITEMS_REQUIRED = "Ajoutez au moins un produit au ravitaillement";
    public static final String RESTOCK_PRODUCT_REQUIRED = "Le produit est obligatoire";
    public static final String QUANTITY_POSITIVE = "La quantité doit être supérieure à 0";
    public static final String UNIT_COST_NOT_NEGATIVE = "Le prix d'achat ne peut pas être négatif";
    public static final String INVALID_YEAR = "Année invalide";
    public static final String INVALID_STOCK_THRESHOLD = "Le seuil de stock bas doit être compris entre 0 et 1000";

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

    public static String productNotFoundById(Long id) {
        return "Produit introuvable : " + id;
    }

    public static String cartItemNotFound(Long itemId) {
        return "Article du panier introuvable : " + itemId;
    }

    public static String productSlugAlreadyUsed(String slug) {
        return "Un produit existe déjà avec le slug : " + slug;
    }

    public static String categoriesNotFound(java.util.Collection<String> slugs) {
        return "Catégorie(s) introuvable(s) : " + String.join(", ", slugs);
    }

    public static String variantNotFound(Long variantId) {
        return "Variante introuvable : " + variantId;
    }

    public static String variantNotInProduct(Long variantId, Long productId) {
        return "La variante " + variantId + " n'appartient pas au produit " + productId;
    }

    public static String restockNotFound(Long restockId) {
        return "Ravitaillement introuvable : " + restockId;
    }

    public static String restockNotPending(Long restockId, String status) {
        return "Le ravitaillement " + restockId + " n'est plus modifiable (statut : " + status + ")";
    }
}
