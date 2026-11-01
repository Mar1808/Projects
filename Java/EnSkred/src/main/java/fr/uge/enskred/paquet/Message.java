package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.utils.Utils;


/**
 * Paquet représentant un message à envoyer entre les composants internes de l'application.
 * 
 * Cette classe est utilisée exclusivement en interne pour encapsuler une expression (exp) et un message
 * (message). Elle permet de sérialiser ces informations dans un format binaire pour les transmettre au sein
 * de l'application.
 */
public record Message(String exp, String message) implements Paquet {
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	public Message {
		Utils.requireNonNulls(exp , message);
	}

	@Override
	public ByteBuffer getWriteModeBuffer() {
		var encodedExp = UTF8.encode(exp);
		var encodedMsg = UTF8.encode(message);
		var buffer = ByteBuffer.allocate(Integer.BYTES * 2 + encodedExp.remaining() + encodedMsg.remaining());
		buffer.putInt(encodedExp.remaining()).put(encodedExp);
		buffer.putInt(encodedMsg.remaining()).put(encodedMsg);
		return buffer;
	}
	
	public String toString() {
		return exp + ": " + message;
	}

	@Override
	public OpCode getOpCode() {
		return OpCode.NO_STATE;
	}
}
