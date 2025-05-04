package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * Représente un paquet de type Broadcast dans le protocole EnSkred. Ce paquet contient un message envoyé par un expéditeur à un ou plusieurs destinataires via un message chiffré.
 * 
 * La classe encapsule les informations suivantes :
 * - La clé publique de l'expéditeur {@link PublicKeyRSA}.
 * - L'ID unique du message {@code messageID}.
 * - La taille du message {@code size}.
 * - Le contenu du message sous forme d'un {@link ByteBuffer} nommé {@code payload}.
 * 
 * <p>Note importante :</p>
 * Le {@code payload} est un {@link ByteBuffer} qui arrive en mode écriture, et la manipulation de sa position doit être soigneusement gérée pour éviter des erreurs.
 * Deux solutions sont proposées : 
 * - Réinitialiser manuellement la position du buffer après utilisation.
 * - Créer une copie du buffer pour éviter les manipulations manuelles.
 */
public record Broadcast(PublicKeyRSA publicKeySender, long messageID, int size, ByteBuffer payload) implements Paquet {
	
	private final static OpCode OP_CODE = OpCode.BROADCAST;
	
	public Broadcast {
		Utils.requireNonNulls(publicKeySender, payload);
		if(size < 0) {
			throw new IllegalArgumentException();
		}
	}
	
	public ByteBuffer payload() {
		return ByteBuffer.allocate(payload.capacity()).put(payload.flip());
		
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		//SENDER PK
		var pubBufferSender = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		publicKeySender.to(pubBufferSender);
		var PKBufferSender = ByteBuffer.allocate(Integer.BYTES + pubBufferSender.flip().remaining());
		PKBufferSender.putInt(pubBufferSender.remaining()).put(pubBufferSender).flip();
		//longMessage + size + payload
//		System.out.println("\nn="+payload.remaining());
		var buffer = ByteBuffer.allocate(Byte.BYTES + PKBufferSender.remaining() + Long.BYTES + Integer.BYTES + payload.flip().remaining());
//		System.out.println("\nn="+payload.remaining());
		return buffer.put(OP_CODE.getCode()).put(PKBufferSender).putLong(messageID).putInt(size).put(payload);
		
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}

	@Override
	public String toString() {
		return "Broadcast[PK:" + publicKeySender + " ___ mID: " + messageID + " ___ size: " + size + " ___ payload: " + payload; 
	}
	
}
