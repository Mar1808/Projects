package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;

/**
 * Représente la confirmation de la déconnexion d'un nœud du réseau.
 * 
 * Cette classe encapsule un paquet contenant la liste des nœuds qui doivent être informés de la 
 * confirmation de déconnexion dans un réseau. Elle est utilisée pour envoyer la confirmation que 
 * la déconnexion d'un nœud a été réalisée et que les autres nœuds peuvent maintenant mettre à jour 
 * leurs informations en conséquence.
 * 
 * Le paquet contient un code d'opération associé et une liste de nœuds (nodes) qui ont été notifiés 
 * de cette confirmation.
 */
public record LeaveNetworkConfirm(List<Node> nodes) implements Paquet {
	private static final OpCode OP_CODE = OpCode.LEAVE_NETWORK_CONFIRM;
	private final static int MAX_SIZE_SOCKETADDRESS = 21; //(byte)1o + (ip)[4/16]o + (port)4o
	
	public LeaveNetworkConfirm {
		Objects.requireNonNull(nodes);
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		//pour la liste de node
		var bufferNode = ByteBuffer.allocate(Integer.BYTES + nodes.size() * (UGEncrypt.MAX_PUBLIC_KEY_SIZE + MAX_SIZE_SOCKETADDRESS));
		bufferNode.putInt(nodes.size());
		nodes.forEach(c -> bufferNode.put(c.getWriteModeBuffer().flip()));
		bufferNode.flip();
		
		return ByteBuffer.allocate(Byte.BYTES + bufferNode.remaining()).put(OP_CODE.getCode()).put(bufferNode);
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
	
}
