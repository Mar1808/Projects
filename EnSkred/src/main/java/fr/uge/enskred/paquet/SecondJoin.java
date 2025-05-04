package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.util.Objects;

import fr.uge.enskred.opcode.OpCode;

/**
 * Représente un paquet de type "SecondJoin" qui est utilisé dans le processus de connexion
 * pour un nœud qui rejoint le réseau après une première tentative.
 * <p>
 * Ce paquet contient un {@link Node} qui représente le nœud rejoignant le réseau.
 * </p>
 * <p>
 * Le paquet "SecondJoin" est sérialisé avec le code d'opération {@link OpCode#SECOND_JOIN}
 * suivi des données du nœud qui rejoint le réseau.
 * </p>
 */
public record SecondJoin(Node node) implements Paquet {
	private static final OpCode OP_CODE = OpCode.SECOND_JOIN;
	
	public SecondJoin {
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
