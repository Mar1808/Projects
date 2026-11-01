package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * Représente un paquet de type "SecureMessage" qui contient un message sécurisé pour un destinataire.
 * <p>
 * Ce paquet peut contenir soit un message chiffré {@code encryptedBuffer}, soit une {@link Instruction}
 * qui sera ensuite chiffrée avant l'envoi.
 * </p>
 * <p>
 * Le paquet "SecureMessage" est sérialisé avec le code d'opération {@link OpCode#SECURE_MESSAGE}
 * suivi du message sécurisé chiffré.
 * </p>
 * <p>
 * Il est supposé que le {@code encryptedBuffer} est en mode lecture avant d'être utilisé.
 * </p>
 */
public record SecureMessage(PublicKeyRSA recipient, Instruction instruction, ByteBuffer encryptedBuffer) implements Paquet {
    private static final OpCode OP_CODE = OpCode.SECURE_MESSAGE;
	
    public SecureMessage {
        Objects.requireNonNull(recipient);
        if (encryptedBuffer == null && instruction == null) {
            throw new IllegalArgumentException("Must have either instruction or encryptedBuffer");
        }
    }
    
    //A -> B :   RSA( receveur, ( 101,  PF( PK_F, ( 100, RSA( 103, PK_A,  time , message ) ) ) )

    @Override
    public ByteBuffer getWriteModeBuffer() {
    	ByteBuffer encryptedPayload = null;
    	if(encryptedBuffer == null) {
    		var payload = instruction.getWriteModeBuffer().flip();
            encryptedPayload = Utils.safeEncryptRSA(payload, recipient);
    	}
    	var encrypted = (encryptedPayload == null) ? encryptedBuffer : encryptedPayload.flip();
        if(encrypted == null){ return ByteBuffer.allocate(Byte.BYTES).put(OP_CODE.getCode()); }
        var finalBuffer = ByteBuffer.allocate(Byte.BYTES + encrypted.remaining());
        finalBuffer.put(OP_CODE.getCode()).put(encrypted);

        return finalBuffer;
    }


    @Override
    public OpCode getOpCode() {
        return OP_CODE;
    }
}
