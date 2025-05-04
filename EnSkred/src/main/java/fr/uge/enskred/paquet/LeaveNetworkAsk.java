package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;



/**
 * Représente une demande de déconnexion d'un réseau.
 * 
 * Cette classe est utilisée pour envoyer une requête de type "LEAVE_NETWORK_ASK" dans le cadre de 
 * la déconnexion d'un nœud d'un réseau. Elle encapsule un paquet très simple qui contient seulement 
 * le code d'opération associé à cette action.
 * 
 * Ce paquet est utilisé pour initier le processus de déconnexion d'un nœud d'un réseau en demandant 
 * à être retiré du système. Le paquet est envoyé avec un format binaire très minimal, comprenant 
 * uniquement le code d'opération correspondant.
 */
public record LeaveNetworkAsk() implements Paquet {
	private final static OpCode OP_CODE = OpCode.LEAVE_NETWORK_ASK;
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		return ByteBuffer.allocate(Byte.BYTES).put(OP_CODE.getCode());
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}

}
