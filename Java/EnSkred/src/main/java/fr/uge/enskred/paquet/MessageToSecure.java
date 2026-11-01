package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * Paquet représentant un message sécurisé à envoyer entre deux parties.
 * 
 * Cette classe sert de trame pour les messages cachés, où le message est chiffré et l'expéditeur est
 * identifié par sa clé publique. Le message est transmis avec un identifiant unique, permettant
 * d'assurer la traçabilité et l'intégrité des communications.
 * 
 * La classe utilise un identifiant de message unique et un encodage UTF-8 pour sérialiser le message
 * avant son envoi.
 */
public record MessageToSecure(PublicKeyRSA sender, long idMessage, String message) implements Paquet, Instruction {
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	private static final OpCode code = OpCode.MESSAGE;
	
	public MessageToSecure {
		Utils.requireNonNulls(sender, message);
	}
	

	@Override
	public ByteBuffer getWriteModeBuffer() {
		
		var pubBuffer = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		sender.to(pubBuffer);
        var PKBuffer = ByteBuffer.allocate(Integer.BYTES + pubBuffer.flip().remaining());
        PKBuffer.putInt(pubBuffer.remaining()).put(pubBuffer).flip();
        
        
		var encodedMsg = UTF8.encode(message);
		var buffer = ByteBuffer.allocate(Byte.BYTES + PKBuffer.remaining() + Long.BYTES + Integer.BYTES + encodedMsg.remaining());

		buffer.put(code.getCode()).put(PKBuffer).putLong(idMessage).putInt(encodedMsg.remaining()).put(encodedMsg);
		return buffer;
	}
	
	public String toString() {
		return sender + ": " + message + "\n" + idMessage + " - " + (System.currentTimeMillis() - idMessage);
	}

	@Override
	public OpCode getOpCode() {
		return code;
	}
}
