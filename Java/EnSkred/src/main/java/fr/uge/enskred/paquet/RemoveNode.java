package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;


/**
 * Représente un paquet de type "RemoveNode" utilisé pour signaler qu'un nœud souhaite quitter le réseau.
 * <p>
 * Ce paquet contient la clé publique RSA du nœud quittant le réseau. Il est envoyé afin d'informer les autres
 * nœuds du départ d'un membre du réseau.
 * </p>
 * <p>
 * Le paquet "RemoveNode" est sérialisé avec le code d'opération {@link OpCode#REMOVE_NODE} suivi des informations
 * relatives au nœud quittant.
 * </p>
 */
public record RemoveNode(PublicKeyRSA publicKeyLeaver) implements Paquet, Payload {
	private final static OpCode OP_CODE = OpCode.REMOVE_NODE;
	
	public RemoveNode {
		Objects.requireNonNull(publicKeyLeaver);
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		//Leaver
		var pubBuffer = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		publicKeyLeaver.to(pubBuffer);
        var PKBuffer = ByteBuffer.allocate(Integer.BYTES + pubBuffer.flip().remaining());
        PKBuffer.putInt(pubBuffer.remaining()).put(pubBuffer).flip();
        
        var buffer = ByteBuffer.allocate(Byte.BYTES + PKBuffer.remaining());
		return buffer.put(OP_CODE.getCode()).put(PKBuffer);
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}

}
