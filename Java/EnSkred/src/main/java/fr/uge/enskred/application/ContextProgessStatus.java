package fr.uge.enskred.application;

/**
 * Énumération représentant l'état de progression d'un contexte dans le protocole de connexion.
 * Chaque état indique une étape du processus de vérification d’un client externe ou interne.
 * ---
 * Les états ont les significations suivantes :
 * 
 * - {@code UNVERIFIED_PRE_JOIN} : La connexion est établie mais le challenge n’est pas encore vérifié.
 *   Aucune action réseau (broadcast, 2e connexion, message) n’est encore autorisée.
 *
 * - {@code UNVERIFIED_CHALLENGE} : Le challenge a été reçu mais pas encore validé.
 *
 * - {@code VERIFIED_1} : Le challenge est validé. Le broadcast est autorisé, ainsi que les messages,
 *   mais la 2e connexion reste à établir.
 *
 * - {@code VERIFIED_2} : Le challenge est validé et la 2e connexion est aussi établie.
 *   Toutes les actions réseau sont possibles (messages, échanges, etc.).
 *
 * - {@code UNCONCERNED} : Le contexte est interne à l'application.
 *   Il n’est pas concerné par le processus de vérification externe et a tous les droits par défaut.
 */
public enum ContextProgessStatus {
	UNVERIFIED_PRE_JOIN,	//Challenge non vérifier -- donc pas de broadcast, 2e connexion & message
	UNVERIFIED_CHALLENGE,
	VERIFIED_1,				//Challenge O.K. -- broadcast, 2e connexion à faire, msg O.K.
	VERIFIED_2, 			//Challenge O.K. -- 2e connexion O.K., msg O.K.
	
	UNCONCERNED;			//On est sur un client Interne donc on peut tout faire
}
