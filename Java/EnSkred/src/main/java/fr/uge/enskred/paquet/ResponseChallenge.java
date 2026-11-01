package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt.PrivateKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * Représente un paquet de type "ResponseChallenge" qui est utilisé pour envoyer une réponse à un défi.
 * <p>
 * Ce paquet contient une clé privée RSA utilisée pour déchiffrer un message crypté et obtenir un nombre
 * long déchiffré, qui est ensuite envoyé en réponse au défi.
 * </p>
 * <p>
 * Le paquet "ResponseChallenge" est sérialisé avec le code d'opération {@link OpCode#RESPONSE_CHALLENGE}
 * suivi de la valeur déchiffrée du message.
 * </p>
 */
public record ResponseChallenge(PrivateKeyRSA privateKey, ByteBuffer encryptedBuffer) implements Paquet {
	private final static OpCode OP_CODE = OpCode.RESPONSE_CHALLENGE;
	
	public ResponseChallenge {
		Utils.requireNonNulls(privateKey, encryptedBuffer);
	}
	
	
	private long decodedLongMessage() {
		return Utils.decodeLongWithPrivateKey(privateKey, encryptedBuffer.flip());
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		return ByteBuffer.allocate(Byte.BYTES + Long.BYTES).put(OP_CODE.getCode()).putLong(decodedLongMessage());
	}
	
	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
	
}
