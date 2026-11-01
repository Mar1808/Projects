package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;

/**
 * Paquet représentant la réponse d'un nœud à une demande de déconnexion du réseau.
 * 
 * Cette classe est utilisée pour envoyer une réponse à une demande de déconnexion d'un nœud du réseau.
 * Elle encapsule un paquet contenant un code d'opération ainsi qu'une réponse binaire qui indique
 * si la déconnexion est acceptée (1) ou refusée (0).
 * 
 * La valeur de la réponse doit être soit 0 (refus), soit 1 (acceptation).
 */
public record LeaveNetworkResponse(byte response) implements Paquet {
	private final static OpCode OP_CODE = OpCode.LEAVE_NETWORK_RESPONSE;
	
	public LeaveNetworkResponse {
		if(response < 0 || response > 1) {
			throw new IllegalArgumentException();
		}
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		return ByteBuffer.allocate(Byte.BYTES * 2).put(OP_CODE.getCode()).put(response);
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
}
