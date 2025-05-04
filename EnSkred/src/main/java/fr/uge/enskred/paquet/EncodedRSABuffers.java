package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.utils.Utils;

/**
 * Représente un paquet contenant un buffer encodé RSA et son code d'opération associé.
 * 
 * Cette classe est utilisée pour encapsuler un bloc de données chiffrées en RSA, ainsi que l'opcode associé à
 * ce paquet (pour le challenge et les messages cachés). 
 * Elle sert de lien entre le processus de lecture et l'utilisateur de l'application.
 * 
 * Le buffer contient les données encodées, et le {@link OpCode} spécifie le type d'opération attendue pour
 * ces données.
 */
public record EncodedRSABuffers(ByteBuffer buffer, OpCode opCode) implements Paquet {
	
	public EncodedRSABuffers {
		Utils.requireNonNulls(opCode, buffer);
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
