package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.enskred.opcode.OpCode;

/**
 * Représente une réponse à un défi long dans le protocole EnSkred.
 * 
 * Cette classe encapsule les informations suivantes :
 * - La réponse au défi sous forme d'un {@code long} ({@code challengeResponse}).
 * - Le code d'opération associé à cette réponse sous forme de {@link OpCode} ({@code opCode}).
 */
public record ChallengeLongResponse(long challengeResponse, OpCode opCode) implements Paquet {
	
	public ChallengeLongResponse {
		Objects.requireNonNull(opCode);
	}
	
	//Non utile---------------------------------
	@Override
	public ByteBuffer getWriteModeBuffer() {
		return ByteBuffer.allocate(0);
	}
	//------------------------------------------

	@Override
	public OpCode getOpCode() {
		return opCode;
	}
	
}
