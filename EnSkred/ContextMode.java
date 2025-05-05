package fr.uge.enskred.application;


/**
 * Énumération représentant le mode d’un contexte dans l’application.
 * Elle permet de distinguer l’origine d’un client selon qu’il est interne ou externe.
 * ---
 * Les modes disponibles sont :
 * 
 * - {@code INTERN_CLIENT} : Le contexte est lié à un client interne (celui de l’utilisateur local).
 *   Il dispose de droits complets par défaut et n’est pas soumis à un processus de vérification.
 *
 * - {@code EXTERN_CLIENT} : Le contexte représente un client externe connecté à notre application.
 *   Il doit être vérifié (via challenge) avant d’avoir un accès complet aux fonctionnalités.
 */
public enum ContextMode {
	INTERN_CLIENT, EXTERN_CLIENT;
}
