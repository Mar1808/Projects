package fr.uge.enskred.paquet;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;



/**
 * Paquet représentant une instruction de transmission d'un message sécurisé à un autre destinataire.
 * <p>
 * Cette classe encapsule une instruction de type "Pass Forward" dans laquelle un message sécurisé est transmis
 * à un destinataire spécifié. Le paquet contient la clé publique du destinataire ainsi que le message sécurisé
 * à transmettre. Il est utilisé dans des scénarios où les données doivent être transmises à un autre nœud
 * de manière sécurisée.
 * </p>
 * 
 * <p>
 * Le paquet "Pass Forward" est associé à un code d'opération {@link OpCode#PASS_FORWARD} et comprend les informations suivantes :
 * </p>
 * <ul>
 *   <li><b>receiver</b> : La clé publique du destinataire (récepteur) à qui le message sécurisé sera envoyé.</li>
 *   <li><b>secureMessage</b> : Le message sécurisé à transmettre, qui est un autre paquet encapsulé dans celui-ci.</li>
 * </ul>
 * 
 * <p>
 * Ce paquet est utilisé principalement dans les systèmes de communication sécurisée pour garantir que les messages
 * sont envoyés de manière fiable et protégée.
 * </p>
 * 
 * @param receiver La clé publique du destinataire du message sécurisé.
 * @param secureMessage Le message sécurisé à transmettre.
 * 
 * @see OpCode#PASS_FORWARD
 * @see Paquet
 */
public record PassForward(PublicKeyRSA receiver, Paquet secureMessage) implements Paquet, Instruction {
	private final static OpCode OP_CODE = OpCode.PASS_FORWARD;
	
	public PassForward {
		Utils.requireNonNulls(receiver, secureMessage);
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		//PK
		var pubBuffer = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		receiver.to(pubBuffer);
        var PKBuffer = ByteBuffer.allocate(Integer.BYTES + pubBuffer.flip().remaining());
        PKBuffer.putInt(pubBuffer.remaining()).put(pubBuffer).flip();
        
        var secureMsgBuffer = secureMessage.getWriteModeBuffer().flip();
        
        var buffer = ByteBuffer.allocate(Byte.BYTES + PKBuffer.remaining() + secureMsgBuffer.remaining());
        buffer.put(OP_CODE.getCode()).put(PKBuffer).put(secureMsgBuffer);
        
        return buffer;
	}
	
	
	@Override
	public String toString() {
		return "PublicKey: " + receiver;
	}

	public static void main(String[] args) {
		try {
            var keyPair1 = UGEncrypt.KeyPairRSA.generate();
            var publicKey = keyPair1.publicKey();
            new Node(publicKey, new InetSocketAddress(9)).getWriteModeBuffer();
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur lors de la génération des clés RSA.");
        }
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
	
}
