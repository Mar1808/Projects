package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.ConnexionReader;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.Reader.ProcessStatus;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;


/**
 * Paquet représentant une nouvelle connexion entre deux parties, identifié par les clés publiques
 * des deux participants. Ce paquet est utilisé pour l'initiation de la connexion entre un expéditeur
 * et un récepteur, et contient leurs clés publiques respectives.
 * 
 * La classe sérialise les clés publiques de l'expéditeur et du récepteur dans un format spécifique
 * qui peut être traité par un {@link ConnexionReader}.
 * 
 * Elle sert de mécanisme pour établir une nouvelle connexion sécurisée entre deux nœuds dans un
 * système distribué, en échangeant les clés publiques des deux parties.
 */
public record NewConnection(PublicKeyRSA publicKeySender, PublicKeyRSA  publicKeyReceiver) implements Paquet, Payload {
	private static final OpCode OP_CODE = OpCode.NEW_CONNECTION;
	
	public NewConnection {
		Utils.requireNonNulls(publicKeyReceiver, publicKeySender);
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		var buffer = Utils.serializeTwoKPublicKeys(publicKeySender, publicKeyReceiver).flip();
		return ByteBuffer.allocate(Byte.BYTES + buffer.remaining()).put(OP_CODE.getCode()).put(buffer);
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
	
    public static void main(String[] args) {
        try {
            var keyPair1 = UGEncrypt.KeyPairRSA.generate();
            var publicKey1 = keyPair1.publicKey();
            var keyPair2 = UGEncrypt.KeyPairRSA.generate();
            var publicKey2 = keyPair2.publicKey();

            
            var buffer = new NewConnection(publicKey1, publicKey2).getWriteModeBuffer().flip();
            buffer.get();
            var connexionReader = new ConnexionReader();
            var status = connexionReader.process(buffer.compact());

            if (status == ProcessStatus.DONE) {
                var connexion = connexionReader.get();
                System.out.println("Test 1 réussi. Connexion lue : " + connexion);
            } else {
                System.out.println("Test 1 échoué. Statut : " + status);
            }
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur lors de la génération des clés RSA.");
        }
    }
	
}
