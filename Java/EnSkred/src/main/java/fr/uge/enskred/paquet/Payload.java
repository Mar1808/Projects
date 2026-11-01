package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;


/**
 * Interface représentant un paquet de type "Payload" (charge utile) dans le système de communication.
 * <p>
 * Cette interface définit les méthodes communes pour tous les paquets qui constituent la charge utile
 * dans le cadre de la transmission de données. Les implémentations concrètes de cette interface (comme
 * {@link NewNode}, {@link NewConnection}, et {@link RemoveNode}) définissent des paquets spécifiques
 * qui contiennent des données à transmettre dans un protocole de communication.
 * </p>
 * <p>
 * Chaque paquet de type Payload doit pouvoir être sérialisé en un {@link ByteBuffer} et doit fournir
 * son code d'opération associé à travers la méthode {@link #getOpCode()}.
 * </p>
 * 
 * @see NewNode
 * @see NewConnection
 * @see RemoveNode
 */
public sealed interface Payload permits 
	//Payload
	NewNode, NewConnection, RemoveNode
	{

	//public Methods
    /**
     * Sérialise le contenu du paquet Payload dans un buffer binaire pour l'envoi.
     * <p>
     * Cette méthode permet de convertir le paquet en un format binaire ({@link ByteBuffer}) qui peut
     * être envoyé à travers le réseau. Chaque implémentation de Payload doit définir la manière dont
     * ses données doivent être sérialisées.
     * </p>
     * 
     * @return Un {@link ByteBuffer} contenant les données sérialisées du paquet prêtes à être envoyées.
     */
	ByteBuffer getWriteModeBuffer();
	
    /**
     * Retourne le code d'opération associé à ce paquet Payload.
     * <p>
     * Le code d'opération permet d'identifier le type de paquet pendant le traitement de la communication.
     * Chaque paquet Payload a un code d'opération unique qui le distingue des autres paquets.
     * </p>
     * 
     * @return Le code d'opération associé à ce paquet Payload.
     */
	OpCode getOpCode();
	
}
