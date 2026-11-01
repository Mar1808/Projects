package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.RSAReader;
import fr.uge.enskred.readers.Reader.ProcessStatus;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * Représente un défi contenant une clé publique et un long associé, dans le cadre du protocole EnSkred.
 * 
 * Cette classe encapsule les informations suivantes :
 * - La clé publique utilisée pour le chiffrement du défi ({@code publicKey}).
 * - Un nombre long challenge ({@code longChallenge}) qui est chiffré avec la clé publique.
 * - Le code d'opération associé à ce défi : {@link OpCode#CHALLENGE_PUBLIC_KEY}.
 */
public record ChallengePublicKey(PublicKeyRSA publicKey, long longChallenge) implements Paquet {
	private final static OpCode OP_CODE = OpCode.CHALLENGE_PUBLIC_KEY;
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		var payloadEncodedBuffer = Utils.encodeLongWithPublicKeyInWriteMode(publicKey, longChallenge).flip();
		var buffer = ByteBuffer.allocate(payloadEncodedBuffer.remaining() + Byte.BYTES);
		return buffer.put(OP_CODE.getCode()).put(payloadEncodedBuffer);
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
	
	//MAIN
    public static void main(String[] args) throws Exception {
        var keyPair = UGEncrypt.KeyPairRSA.generate();
        var publicKey = keyPair.publicKey();
        var privateKey = keyPair.privateKey();

        var originalLong = 1234567890L;
        System.out.println("Valeur originale : " + originalLong);

        var challenge = new ChallengePublicKey(publicKey, originalLong);
        var encryptedBuffer = challenge.getWriteModeBuffer();

        var rsaReader = new RSAReader();
        var status = rsaReader.process(encryptedBuffer.position(1).compact());

        if(status == ProcessStatus.DONE) {
            var encryptedBlocks = rsaReader.get();
            rsaReader.reset();
            System.out.println("Test réussi. Nombre de blocs lus : " + encryptedBlocks.flip().remaining());

            var decryptedBlock = ByteBuffer.allocate(UGEncrypt.KEY_SIZE_BYTES);
            privateKey.decrypt(encryptedBlocks.flip(), decryptedBlock);
            decryptedBlock.flip();

            var decodedLong = decryptedBlock.getLong();
            System.out.println("Valeur déchiffrée : " + decodedLong);

            if(originalLong == decodedLong) {
                System.out.println("Succès ! Le long est bien encodé et décodé.");
            } else {
                System.out.println("Échec : la valeur ne correspond pas !");
            }
        } else {
            System.out.println("Test échoué. Statut : " + status);
        }
    }
	
}
