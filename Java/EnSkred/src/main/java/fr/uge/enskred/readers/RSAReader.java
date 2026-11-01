package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;

/**
 * La classe {@code RSAReader} implémente l'interface {@code Reader<ByteBuffer>} et est responsable de la lecture d'un message chiffré en RSA. 
 * Elle découpe le message en blocs de taille fixe, lit ces blocs depuis un buffer d'entrée et renvoie le résultat dans un buffer de sortie.
 * 
 * Le processus de lecture se déroule en plusieurs étapes :
 * 1. La classe attend d'abord la taille du message (le nombre de blocs).
 * 2. Ensuite, elle lit les blocs de données chiffrées un par un, chaque bloc ayant une taille fixe.
 * 3. Une fois tous les blocs lus, elle marque la lecture comme terminée.
 * 
 * Le message chiffré est structuré comme suit :
 * - La taille du message en blocs (INT) suivie des blocs de données chiffrées (chaque bloc ayant une taille de 256 octets).
 * 
 * Exemple de format RSA:
 * RSA(public_key, payload) = nb_blocs (INT) + encrypted_bloc1 (256 BYTES) + ... + encrypted_blocn (256 BYTES)
 * 
 * @see UGEncrypt#encryptRSA(ByteBuffer, PublicKeyRSA)
 * @see UGEncrypt#decryptRSA(ByteBuffer, PrivateKeyRSA)
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class RSAReader implements Reader<ByteBuffer> {
	/**********************************************************************************************************
	 * RSA(public_key, payload) = nb_blocs (INT) + encrypted_bloc1 (256 BYTES) + ... + encrypted_blocn (256 BYTES)
	 **********************************************************************************************************/

	private enum State {
		DONE, WAITING_SIZE, WAITING_BLOCK, ERROR
	}

	private static final int BLOCK_SIZE = 256; // Taille des blocs chiffrés

	private final ByteBuffer internalBuffer = ByteBuffer.allocate(Integer.BYTES);
	private final IntReader intReader = new IntReader();

	private State state = State.WAITING_SIZE;
	private int nbBlocks;
	private ByteBuffer encryptedBuffer;

	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}

		switch (state) {
		case WAITING_SIZE:
			var status = intReader.process(buffer);
			if (status != ProcessStatus.DONE) {
				return status;
			}
			nbBlocks = intReader.get();
			//mise en commentaire car empêche de faire des messages de plus de 4 blocs
			if(nbBlocks <= 0/* || (nbBlocks-1) * BLOCK_SIZE + LAST_BLOCK_SIZE > BUFFER_SIZE*/) {
				state = State.ERROR;
				return ProcessStatus.ERROR;
			}
			encryptedBuffer = ByteBuffer.allocate(BLOCK_SIZE * nbBlocks);
			state = State.WAITING_BLOCK;
		case WAITING_BLOCK:
			try {
				buffer.flip();
				processForFillBlock(buffer, encryptedBuffer);
				if(encryptedBuffer.hasRemaining()) {
					return ProcessStatus.REFILL;
				}
				state = State.DONE;
				return ProcessStatus.DONE;
			} finally {
				buffer.compact();
			}

		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}

	private void processForFillBlock(ByteBuffer buffer, ByteBuffer encryptedBuffer) {
		//        int remaining = dest.remaining();
		//        if (src.remaining() < remaining) {
		//            dest.put(src);
		//            return false;
		//        }
		//        for (int i = 0; i < remaining; i++) {
		//            dest.put(src.get());
		//        }
		//        return true;
		var remaining = Math.min(buffer.remaining(), encryptedBuffer.remaining());
		var oldLimit = buffer.limit();
		buffer.limit(buffer.position() + remaining);
		encryptedBuffer.put(buffer);
		buffer.limit(oldLimit);
	}

	@Override
	public ByteBuffer get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		/*
        ByteBuffer decryptedBuffer = ByteBuffer.allocate(nbBlocks * UGEncrypt.MAX_ENCRYPT_BLOCK_SIZE);
        for (ByteBuffer encryptedBlock : encryptedBlocks) {
            encryptedBlock.flip(); // Préparer pour la lecture
            ByteBuffer decryptBlock = ByteBuffer.allocate(UGEncrypt.MAX_ENCRYPT_BLOCK_SIZE);
            try {
                privateKey.decrypt(encryptedBlock, decryptBlock);
            } catch (ShortBufferException | IllegalBlockSizeException e) {
                throw new RuntimeException("Erreur lors du déchiffrement", e);
            }
            decryptBlock.flip(); // Préparer pour la lecture
            decryptedBuffer.put(decryptBlock);
        }
        decryptedBuffer.flip();
        return StandardCharsets.UTF_8.decode(decryptedBuffer).toString();*/
		var size = encryptedBuffer.flip().remaining();
		return ByteBuffer.allocate(Integer.BYTES + size).putInt(nbBlocks).put(encryptedBuffer);
	}

	@Override
	public void reset() {
		state = State.WAITING_SIZE;
		internalBuffer.clear();
		intReader.reset();
		nbBlocks = 0;
		encryptedBuffer = null;
	}

	public static void main(String[] args) throws NoSuchAlgorithmException, ShortBufferException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException {
		System.out.println("TEST 1 : Lecture complète d'un message RSA chiffré");

		//génération des clés RSA
		var keyPair = UGEncrypt.KeyPairRSA.generate();
		var publicKey = keyPair.publicKey();
		var privateKey = keyPair.privateKey();

		//message original
		var originalMessage = "Hello, this is an RSA test! This message is split into multiple encrypted blocks.".repeat(1000);
		var messageBuffer = StandardCharsets.UTF_8.encode(originalMessage);
		var encryptedBuffer = UGEncrypt.encryptRSA(messageBuffer, publicKey);

		//lecture du message chiffré
		var rsaReader = new RSAReader();
		var status = rsaReader.process(encryptedBuffer);

		if(status != ProcessStatus.DONE) { System.out.println("Test 1 échoué. Statut : " + status); }
		var encryptedBlocksBuffer = rsaReader.get(); // Un seul ByteBuffer avec tous les blocs concaténés
		//rsaReader.reset();
		encryptedBlocksBuffer.flip();
		System.out.println("Test 1 réussi. Taille totale des blocs lus : " + encryptedBlocksBuffer.remaining() + " octets");

		// Déchiffrement et reconstruction du message
		var decryptedMessage = new StringBuilder();
		var decodedBufferFLipped = UGEncrypt.decryptRSA(encryptedBlocksBuffer, privateKey);
		decryptedMessage.append(StandardCharsets.UTF_8.decode(decodedBufferFLipped.flip()));
		// Affichage du message déchiffré
		System.out.println("Message déchiffré : " + decryptedMessage.substring(0, Math.min(500, decryptedMessage.length())) + "...");
		System.out.println("(affiché tronqué)");

		// Vérification de l'intégrité
		if (originalMessage.equals(decryptedMessage.toString())) {
			System.out.println("Déchiffrement réussi !");
		} else {
			System.out.println("Échec du déchiffrement. Message incorrect.");
		}
		//        var keyPair = UGEncrypt.KeyPairRSA.generate();
		//        var publicKey = keyPair.publicKey();   // For encryption
		//        var privateKey = keyPair.privateKey(); // For decryption
		//
		//        System.out.println("Public key: " + publicKey);
		//        System.out.println("Private key: " + privateKey);
		//
		//        // Message à chiffrer
		//        String originalMessage = "Test class, RSAReader";
		//        System.out.println("Original message: " + originalMessage);
		//
		//        // Chiffrement du message
		//        ByteBuffer messageBuffer = StandardCharsets.UTF_8.encode(originalMessage);
		//        ByteBuffer encryptedBuffer = ByteBuffer.allocate(UGEncrypt.KEY_SIZE_BYTES);
		//        publicKey.encrypt(messageBuffer, encryptedBuffer);
		//        //encryptedBuffer.flip();
		//
		//        System.out.println("Encrypted message: " + encryptedBuffer);
		//
		//        // Test de déchiffrement avec RSAReader
		//        RSAReader rsaReader = new RSAReader();
		//        ProcessStatus status = rsaReader.process(encryptedBuffer);
		//        System.out.println(UTF8.decode(encryptedBuffer));
		//        /*if (status == ProcessStatus.DONE) {
		//            String decryptedMessage = rsaReader.get();
		//            System.out.println("Decrypted message: " + decryptedMessage);
		//        } else {
		//            System.out.println("Decryption failed.");
		//        }*/
	}
}
