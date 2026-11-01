package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;

/**
 * Paquet représentant la fin du processus de déconnexion d'un nœud du réseau.
 * 
 * Cette classe est utilisée pour indiquer qu'un nœud a terminé sa déconnexion du réseau. 
 * Elle encapsule un paquet contenant le code d'opération associé et marque l'achèvement 
 * du processus de déconnexion pour le nœud concerné.
 * 
 * Le paquet contient uniquement le code d'opération pour signaler la fin de l'opération 
 * de déconnexion, sans données supplémentaires.
 */
public record LeaveNetworkDone() implements Paquet {
	private final static OpCode OP_CODE = OpCode.LEAVE_NETWORK_DONE;
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		return ByteBuffer.allocate(Byte.BYTES).put(OP_CODE.getCode());
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
}
