package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.enskred.opcode.OpCode;

/**
 * Représente un paquet de type "PreJoin" utilisé lors de la tentative d'un nœud de rejoindre un réseau.
 * <p>
 * Ce paquet contient un {@link Node} qui représente le nœud qui demande à rejoindre le réseau
 * avant la validation finale de la connexion.
 * </p>
 * <p>
 * Le paquet "PreJoin" est sérialisé avec le code d'opération {@link OpCode#PRE_JOIN}, ainsi que
 * les informations relatives au nœud tentant de rejoindre.
 * </p>
 */
public record PreJoin(Node node) implements Paquet {
	private static final OpCode OP_CODE = OpCode.PRE_JOIN;
	
	public PreJoin {
		Objects.requireNonNull(node);
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		var buffer = node.getWriteModeBuffer().flip();
		var finalBuffer = ByteBuffer.allocate(buffer.remaining() + Byte.BYTES);
		return finalBuffer.put(OP_CODE.getCode()).put(buffer);
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}

}
