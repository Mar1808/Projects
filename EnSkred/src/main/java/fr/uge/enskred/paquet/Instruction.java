package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;

/**
 * Interface représentant une instruction dans le système de paquets.
 * 
 * Cette interface est utilisée pour définir les différents types d'instructions qui peuvent être
 * envoyées dans le cadre du système de communication. Elle impose aux classes qui l'implémentent de 
 * fournir des méthodes permettant d'obtenir le buffer de données sous forme de mode d'écriture ainsi 
 * que le code d'opération associé.
 * 
 * Les classes suivantes sont autorisées à implémenter cette interface :
 * <ul>
 *     <li>{@link PassForward}</li>
 *     <li>{@link MessageToSecure}</li>
 * </ul>
 * L'implémentation de cette interface permet d'encapsuler le comportement nécessaire pour gérer la
 * sérialisation des données et l'identification des types d'instructions dans le système.
 */
public sealed interface Instruction permits 
	//Instruction
	PassForward, MessageToSecure {
	
	//public Methods
    /**
     * Méthode permettant de récupérer le buffer de données dans un format de mode d'écriture.
     * 
     * Cette méthode est utilisée pour obtenir le buffer des données sérialisées en mode d'écriture,
     * afin qu'elles puissent être envoyées ou stockées. Le format de ce buffer dépend de l'implémentation
     * spécifique de l'instruction.
     * 
     * @return Le buffer de données en mode d'écriture.
     */
	ByteBuffer getWriteModeBuffer();
	
	
    /**
     * Méthode permettant de récupérer le code d'opération associé à l'instruction.
     * 
     * Le code d'opération {@link OpCode} permet d'identifier le type d'instruction et de déterminer 
     * l'action à effectuer. Chaque type d'instruction doit avoir son propre code d'opération distinct.
     * 
     * @return Le code d'opération associé à l'instruction.
     */
	OpCode getOpCode();
}
