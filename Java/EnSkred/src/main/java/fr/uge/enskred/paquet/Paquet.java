package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;


/**
 * Interface représentant un paquet dans le système de communication réseau.
 * <p>
 * Un paquet est une unité de données qui peut être envoyée ou reçue dans le réseau. Chaque paquet est
 * associé à un {@link OpCode} unique qui permet de l'identifier et de déterminer son type et son traitement.
 * Les paquets peuvent être de différents types, tels que des messages, des demandes de connexion, des réponses,
 * des commandes de déconnexion, etc.
 * </p>
 * <p>
 * Chaque paquet implémente deux méthodes principales :
 * </p>
 * <ul>
 *   <li>{@link #getWriteModeBuffer()} : Sérialise les données du paquet dans un buffer binaire pour l'envoi.</li>
 *   <li>{@link #getOpCode()} : Retourne le code d'opération associé à ce paquet.</li>
 * </ul>
 * <p>
 * Cette interface est implémentée par plusieurs classes concrètes qui définissent des types spécifiques de paquets.
 * Ces paquets peuvent être catégorisés comme suit :
 * </p>
 * <ul>
 *   <li><b>Paquets de message</b> : Paquets tels que {@link Message}, {@link MessageToSecure}, qui contiennent des informations à transmettre.</li>
 *   <li><b>Paquets de déconnexion</b> : Paquets comme {@link LeaveNetworkAsk}, {@link LeaveNetworkResponse}, gérant la déconnexion des nœuds du réseau.</li>
 *   <li><b>Paquets de connexion</b> : Paquets comme {@link NewNode}, {@link NewConnection}, qui gèrent l'établissement et la gestion des connexions.</li>
 *   <li><b>Paquets d'instruction</b> : Paquets tels que {@link PassForward}, {@link SecureMessage}, qui contiennent des commandes spécifiques pour le traitement de données.</li>
 * </ul>
 * 
 * <p>
 * Les paquets doivent implémenter cette interface afin d'assurer une gestion uniforme des données transmises dans
 * le système.
 * </p>
 * 
 * @see OpCode
 * @see ByteBuffer
 */
public sealed interface Paquet permits 
	//Paquet
	Broadcast, PreJoin, SecondJoin, ChallengePublicKey, 
	ChallengeLongResponse, ResponseChallenge, ChallengeOk,
	JoinResponse, Connexion, EncodedRSABuffers,	ListConnected, 
	Message, MessagePublic, Node, MessageToSecure,
	//déconnexion
	LeaveNetworkAsk, LeaveNetworkResponse, LeaveNetworkCancel,
	LeaveNetworkConfirm, LeaveNetworkDone,
	//Payload
	NewNode, NewConnection, RemoveNode,
	//Instruction
	PassForward, SecureMessage
	{
	
    /**
     * Sérialise le contenu du paquet dans un buffer binaire pour l'envoi.
     * <p>
     * Cette méthode transforme les données internes du paquet en un format binaire (ByteBuffer) qui peut être
     * transmis sur le réseau. Elle est utilisée lors de la préparation des données à envoyer à un autre nœud.
     * </p>
     * 
     * @return Un {@link ByteBuffer} contenant les données sérialisées du paquet prêtes à être envoyées.
     */
	ByteBuffer getWriteModeBuffer();
	
    /**
     * Retourne le code d'opération associé à ce paquet.
     * <p>
     * Chaque paquet est associé à un code d'opération unique, qui permet de l'identifier et de déterminer son
     * traitement lors de la réception. Ce code est utilisé pour comprendre le type du paquet et la manière de
     * le traiter dans le système.
     * </p>
     * 
     * @return Le {@link OpCode} représentant le code d'opération associé à ce paquet.
     */
	OpCode getOpCode();	
}
