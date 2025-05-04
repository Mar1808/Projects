package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;

/**
 * Représente une demande d'annulation de la déconnexion d'un réseau.
 * 
 * Cette classe est utilisée pour envoyer une requête de type "LEAVE_NETWORK_CANCEL" dans le cadre 
 * de l'annulation d'une demande de déconnexion d'un nœud d'un réseau. Elle encapsule un paquet simple 
 * qui contient uniquement le code d'opération associé à cette action.
 * 
 * Ce paquet est utilisé pour signaler l'annulation d'une demande de déconnexion initiée précédemment, 
 * permettant ainsi à un nœud de rester connecté au réseau au lieu d'être retiré.
 */
public record LeaveNetworkCancel() implements Paquet {
	private final static OpCode OP_CODE = OpCode.LEAVE_NETWORK_CANCEL;
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		return ByteBuffer.allocate(Byte.BYTES).put(OP_CODE.getCode());
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}

}
