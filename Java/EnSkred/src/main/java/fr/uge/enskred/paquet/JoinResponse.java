package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.util.List;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;


/**
 * Représente une réponse à une requête de jointure dans le réseau.
 * 
 * Cette classe encapsule la réponse à une demande de connexion ou de jonction dans le réseau. 
 * Elle contient la clé publique du récepteur, une liste de nœuds, et une liste de connexions 
 * associées. Elle est utilisée pour envoyer une réponse formée de plusieurs parties, chacune 
 * correspondant à des informations spécifiques, telles que les nœuds disponibles ou les connexions 
 * possibles.
 * 
 * Le paquet résultant est utilisé pour envoyer ces informations sous forme de données sérialisées 
 * via un buffer en mode d'écriture.
 */
public record JoinResponse(PublicKeyRSA publicKeyReceiver, List<Node> nodes, List<Connexion> connexions) implements Paquet {
	private final static OpCode OP_CODE = OpCode.JOIN_RESPONSE;
	private final static int MAX_SIZE_SOCKETADDRESS = 21; //(byte)1o + (ip)[4/16]o + (port)4o
	
	public JoinResponse {
		Utils.requireNonNulls(publicKeyReceiver, nodes, connexions);
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		//pour la clé publique
		var pubBufferReceiver = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		publicKeyReceiver.to(pubBufferReceiver);
        var PKBufferReceiver = ByteBuffer.allocate(Integer.BYTES + pubBufferReceiver.flip().remaining());
        PKBufferReceiver.putInt(pubBufferReceiver.remaining()).put(pubBufferReceiver).flip();
        
		//pour la liste de node
		var bufferNode = ByteBuffer.allocate(Integer.BYTES + nodes.size() * (UGEncrypt.MAX_PUBLIC_KEY_SIZE + MAX_SIZE_SOCKETADDRESS));
		bufferNode.putInt(nodes.size());
		nodes.forEach(c -> bufferNode.put(c.getWriteModeBuffer().flip()));
		bufferNode.flip();
		
		//pour la liste de connexion
		var bufferConnexion = ByteBuffer.allocate(Integer.BYTES + connexions.size() * (UGEncrypt.MAX_PUBLIC_KEY_SIZE * 2));
		bufferConnexion.putInt(connexions.size());
		connexions.forEach(c -> bufferConnexion.put(c.getWriteModeBuffer().flip()));
		bufferConnexion.flip();
		
		//Buffer final
		var buffer = ByteBuffer.allocate(Byte.BYTES + PKBufferReceiver.remaining() + bufferNode.remaining() + bufferConnexion.remaining());
		return buffer.put(OP_CODE.getCode()).put(PKBufferReceiver).put(bufferNode).put(bufferConnexion);
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
	

	
}
