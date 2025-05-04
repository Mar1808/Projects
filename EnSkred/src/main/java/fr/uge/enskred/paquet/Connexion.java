package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.ConnexionReader;
import fr.uge.enskred.readers.Reader.ProcessStatus;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * Représente une connexion entre deux entités via des clés publiques RSA.
 * Cette classe est utilisée en interne pour gérer l'établissement de connexions dans le protocole EnSkred.
 * 
 * Elle encapsule deux clés publiques RSA : une pour l'expéditeur et une pour le récepteur.
 * Le processus de sérialisation des clés publiques est réalisé via la méthode {@link Utils#serializeTwoKPublicKeys(PublicKeyRSA, PublicKeyRSA)}.
 */
public record Connexion(PublicKeyRSA publicKeySender, PublicKeyRSA  publicKeyReceiver) implements Paquet {
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		return Utils.serializeTwoKPublicKeys(publicKeySender, publicKeyReceiver);
	}
	
	@Override
	public String toString() {
		return "Sender: " + publicKeySender + "\nReceiver: " + publicKeyReceiver;
	}
	
    public static void main(String[] args) {
        try {
            var keyPair1 = UGEncrypt.KeyPairRSA.generate();
            var publicKey1 = keyPair1.publicKey();
            var keyPair2 = UGEncrypt.KeyPairRSA.generate();
            var publicKey2 = keyPair2.publicKey();

            
            var buffer = new Connexion(publicKey1, publicKey2).getWriteModeBuffer();
            var connexionReader = new ConnexionReader();
            var status = connexionReader.process(buffer);

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

	@Override
	public OpCode getOpCode() {
		return OpCode.NO_STATE;
	}
	
}
