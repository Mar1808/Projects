package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;

/**
 * Représente une réponse positive à un défi dans le protocole EnSkred.
 * 
 * Cette classe encapsule les informations suivantes :
 * - La clé publique du récepteur du défi ({@code publicKeyReceiver}).
 * - Le code d'opération spécifique à cette réponse : {@link OpCode#CHALLENGE_OK}.
 */
public record ChallengeOk(PublicKeyRSA publicKeyReceiver) implements Paquet {
	final static private OpCode OP_CODE = OpCode.CHALLENGE_OK;

	public ChallengeOk { Objects.requireNonNull(publicKeyReceiver); }
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		var pubBufferReceiver = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		publicKeyReceiver.to(pubBufferReceiver);
        var PKBufferReceiver = ByteBuffer.allocate(Integer.BYTES + pubBufferReceiver.flip().remaining());
        PKBufferReceiver.putInt(pubBufferReceiver.remaining()).put(pubBufferReceiver).flip();
        var buffer = ByteBuffer.allocate(Byte.BYTES + PKBufferReceiver.remaining());
		return buffer.put(OP_CODE.getCode()).put(PKBufferReceiver);
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
}
