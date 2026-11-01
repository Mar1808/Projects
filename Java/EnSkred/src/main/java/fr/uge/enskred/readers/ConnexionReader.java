package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import fr.uge.enskred.paquet.Connexion;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;


/**
 * ConnexionReader est un {@link Reader} permettant de lire une instance de {@link Connexion}
 * à partir d’un {@link ByteBuffer}. Une Connexion est composée de deux clés publiques RSA
 * ({@link PublicKeyRSA}), chacune lue à l’aide d’un {@link PublicKeyReader}.
 * 
 * ---
 * 
 * États internes :
 * <ul>
 *   <li>{@code WAITING_FIRST_RSA} : en attente de la première clé publique</li>
 *   <li>{@code WAITING_SECOND_RSA} : en attente de la deuxième clé publique</li>
 *   <li>{@code DONE} : les deux clés ont été lues et une Connexion a été construite</li>
 *   <li>{@code ERROR} : une erreur est survenue durant le traitement</li>
 * </ul>
 * 
 * ---
 * 
 * @method process(ByteBuffer buffer) : lit deux clés publiques RSA depuis le buffer. Chaque clé
 * est traitée séquentiellement via un {@link PublicKeyReader}. Retourne {@link ProcessStatus#REFILL}
 * tant que toutes les données ne sont pas disponibles, puis {@link ProcessStatus#DONE} une fois
 * la Connexion complètement reconstruite.
 * 
 * @method get() : retourne la Connexion lue. Doit être appelé uniquement si {@code process()} a
 * retourné {@code DONE}. Sinon, une exception {@link IllegalStateException} est levée.
 * 
 * @method reset() : réinitialise l’état interne pour permettre une nouvelle lecture.
 * 
 * ---
 * 
 * @implNote La lecture utilise un seul {@link PublicKeyReader} pour lire les deux clés
 * de manière séquentielle. Le buffer est manipulé de façon non destructive, assurant
 * que les données peuvent être lues progressivement.
 * 
 * ---
 * 
 * Exemple d’utilisation :
 * <pre>
 * ByteBuffer buffer = connexion.getWriteModeBuffer();
 * var reader = new ConnexionReader();
 * if (reader.process(buffer) == ProcessStatus.DONE) {
 *     Connexion c = reader.get();
 * }
 * </pre>
 * 
 * ---
 * 
 * @throws IllegalStateException si {@code get()} est appelé alors que l’état n’est pas {@code DONE}
 * 
 * ---
 * 
 * Méthodes de test incluses :
 * <ul>
 *   <li>{@code testSingleConnexionInLargeBuffer()} : lecture d’une Connexion avec un buffer contenant toutes les données</li>
 *   <li>{@code testSingleConnexionWithSmallBuffer()} : lecture d’une Connexion avec un buffer simulant une lecture fragmentée</li>
 *   <li>{@code testMultipleConnexions()} : lecture de plusieurs Connexions consécutives dans le même buffer</li>
 * </ul>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class ConnexionReader implements Reader<Connexion> {
	private final PublicKeyReader publicKeyReader = new PublicKeyReader();
	private State state = State.WAITING_FIRST_RSA;
	private PublicKeyRSA secondPublicKey;
	private PublicKeyRSA firstPublicKey;
	private Connexion connexion;

	private enum State {
		WAITING_FIRST_RSA, WAITING_SECOND_RSA, DONE, ERROR 
	}

	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException("State: " + state);
		}
		switch(state) {
		case WAITING_FIRST_RSA:
			switch(publicKeyReader.process(buffer)) {
			case REFILL: return ProcessStatus.REFILL;
			case DONE:
				firstPublicKey = publicKeyReader.get();
				publicKeyReader.reset();
				state = State.WAITING_SECOND_RSA;
				break;
			default:	
				state = State.ERROR; 
				return ProcessStatus.ERROR;
			}
		case WAITING_SECOND_RSA:
			switch(publicKeyReader.process(buffer)) {
			case REFILL: return ProcessStatus.REFILL;
			case DONE:
				secondPublicKey = publicKeyReader.get();
				state = State.DONE;
				connexion = new Connexion(firstPublicKey, secondPublicKey);
				return ProcessStatus.DONE;
			default:	
				state = State.ERROR; 
				return ProcessStatus.ERROR;
			}
		default:
			state = State.ERROR;
			return ProcessStatus.ERROR;
		}
	}

	@Override
	public Connexion get() {
		if(state != State.DONE) {
			throw new IllegalStateException("State is not DONE");
		}
		return connexion;
	}


	@Override
	public void reset() {
		state = State.WAITING_FIRST_RSA;
		publicKeyReader.reset();
		firstPublicKey = null;
		secondPublicKey= null;
	}

	//MAIN-------------
    public static void main(String[] args) {
        try {
            var keyPair1 = UGEncrypt.KeyPairRSA.generate();
            var publicKey1 = keyPair1.publicKey();
            var keyPair2 = UGEncrypt.KeyPairRSA.generate();
            var publicKey2 = keyPair2.publicKey();

            testSingleConnexionInLargeBuffer(publicKey1, publicKey2);
            testSingleConnexionWithSmallBuffer(publicKey1, publicKey2);
            testMultipleConnexions(publicKey1, publicKey2);
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur lors de la génération des clés RSA.");
        }
    }

    // Test 1 : Lire une Connexion avec un buffer suffisant
    private static void testSingleConnexionInLargeBuffer(PublicKeyRSA publicKey1, PublicKeyRSA publicKey2) {
        System.out.println("TEST 1 : Lecture d'une Connexion avec un buffer suffisant");

        var buffer = new Connexion(publicKey1, publicKey2).getWriteModeBuffer();
        var connexionReader = new ConnexionReader();
        var status = connexionReader.process(buffer);

        if(status == ProcessStatus.DONE) {
            var connexion = connexionReader.get();
            System.out.println("Test 1 réussi. Connexion lue : " + connexion);
        } else {
            System.out.println("Test 1 échoué. Statut : " + status);
        }
    }

    // Test 2 : Lire une Connexion avec un buffer trop petit (lecture en plusieurs étapes)
    private static void testSingleConnexionWithSmallBuffer(PublicKeyRSA publicKey1, PublicKeyRSA publicKey2) {
        System.out.println("TEST 2 : Lecture d'une Connexion avec un buffer trop petit");

        var fullBuffer = new Connexion(publicKey1, publicKey2).getWriteModeBuffer().flip();
        var connexionReader = new ConnexionReader();
        var smallBuffer = ByteBuffer.allocate(1);
        var status = ProcessStatus.REFILL;

        while(fullBuffer.hasRemaining() || status == ProcessStatus.REFILL) {
            smallBuffer.clear().put(fullBuffer.get());
            status = connexionReader.process(smallBuffer);
            smallBuffer.clear();
        }

        if(status == ProcessStatus.DONE) {
            var connexion = connexionReader.get();
            System.out.println("Test 2 réussi. Connexion lue : " + connexion);
        } else {
            System.out.println("Test 2 échoué. Statut : " + status);
        }
    }

    // Test 3 : Lire plusieurs Connexions dans un même buffer
    private static void testMultipleConnexions(PublicKeyRSA publicKey1, PublicKeyRSA publicKey2) throws NoSuchAlgorithmException {
        System.out.println("TEST 3 : Lecture de plusieurs Connexions consécutives");

        var keyPair3 = UGEncrypt.KeyPairRSA.generate();
        var publicKey3 = keyPair3.publicKey();
        var keyPair4 = UGEncrypt.KeyPairRSA.generate();
        var publicKey4 = keyPair4.publicKey();

        var buffer1 = new Connexion(publicKey1, publicKey2).getWriteModeBuffer().flip();
        var buffer2 = new Connexion(publicKey3, publicKey4).getWriteModeBuffer().flip();
        var buffer = ByteBuffer.allocate(buffer1.remaining() + buffer2.remaining());
        buffer.put(buffer1).put(buffer2);

        var connexionReader = new ConnexionReader();
        var status = ProcessStatus.REFILL;

        while(buffer.position() != 0 || status == ProcessStatus.REFILL) {
            status = connexionReader.process(buffer);
            if(status == ProcessStatus.DONE) {
                var connexion = connexionReader.get();
                System.out.println("Connexion lue : " + connexion);
                connexionReader.reset();
            }
        }
    }
	
}