package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;


/**
 * La classe {@code PublicKeyReader} implémente l'interface {@code Reader<PublicKeyRSA>} et est responsable de la lecture et de l'extraction d'une clé publique RSA à partir d'un buffer de données.
 * 
 * <p>Elle utilise un mécanisme de traitement en plusieurs étapes pour gérer la lecture de la taille de la clé et la clé elle-même, tout en garantissant la gestion des erreurs et la validation des données lues.</p>
 * 
 * <p>Le processus de lecture est divisé en plusieurs états :</p>
 * <ul>
 *   <li>{@code WAITING_SIZE} : En attente de la taille de la clé publique (entier).</li>
 *   <li>{@code WAITING_KEY} : En attente de la clé publique après avoir reçu sa taille.</li>
 *   <li>{@code DONE} : La lecture a été effectuée avec succès.</li>
 *   <li>{@code ERROR} : Une erreur est survenue lors de la lecture.</li>
 * </ul>
 * 
 * <p>Lors de la lecture, la classe suit un flux de travail basé sur un buffer, et la clé publique RSA est récupérée lorsque le processus de lecture est terminé avec succès.</p>
 * 
 * <p>Les principales étapes du processus sont :</p>
 * <ul>
 *   <li>La taille de la clé est lue à partir du buffer.</li>
 *   <li>Le buffer est ensuite rempli avec la clé publique correspondante à la taille lue.</li>
 *   <li>Une fois la clé lue, elle est convertie en un objet {@code PublicKeyRSA} et mise à disposition pour une utilisation ultérieure.</li>
 * </ul>
 * 
 * <p>Les méthodes {@code process}, {@code get}, et {@code reset} sont les principales interfaces pour interagir avec la classe. La méthode {@code process} traite les données dans le buffer, {@code get} renvoie la clé publique une fois que la lecture est terminée, et {@code reset} réinitialise l'état pour permettre une nouvelle lecture.</p>
 *
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class PublicKeyReader implements Reader<PublicKeyRSA> {
	private enum State {
		DONE, WAITING_SIZE, WAITING_KEY, ERROR
	}

	private final static int BUFFER_SIZE = UGEncrypt.MAX_PUBLIC_KEY_SIZE;
	private final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE);	// write-mode
	private final IntReader intReader = new IntReader();
	private State state = State.WAITING_SIZE;
	private PublicKeyRSA publicKey;


    @Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch(state) {
			//on va récuperer la taille de la chaine
			case WAITING_SIZE:
				var situation = intReader.process(buffer);
				if(situation != ProcessStatus.DONE) {
					return situation;
				}
                var size = intReader.get();
				if(size < 0 || size > BUFFER_SIZE) {
					state = State.ERROR;
					return ProcessStatus.ERROR;
				}
				state = State.WAITING_KEY;
				byteBuffer.limit(size);
				//on va récuperer la chaine à partir de la taille
			case WAITING_KEY:
				buffer.flip();
				try {
					if(!processForFillString(buffer, byteBuffer)) {
						return ProcessStatus.REFILL;
					}
					byteBuffer.flip();
					publicKey = PublicKeyRSA.from(byteBuffer);
					state = State.DONE;
					return ProcessStatus.DONE;
				} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
					throw new IllegalArgumentException("Error pending extract of rsa key ");
				} finally {
					buffer.compact();
				}
			default:
				state = State.ERROR;return ProcessStatus.ERROR;
		}		
	}

	//TODO
	@Override
	public PublicKeyRSA get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return publicKey;
	}

	//TODO
	@Override
	public void reset() {
		state = State.WAITING_SIZE;
		intReader.reset();
		byteBuffer.clear();
	}

	//////PRIVATE METHODS -----------
	private boolean processForFillString(ByteBuffer bufferIn, ByteBuffer bufferOut) {
		if(!bufferIn.hasRemaining() || !bufferOut.hasRemaining()) {
			return !bufferOut.hasRemaining();
		}
		var byteToCopy = Math.min(bufferIn.remaining(), bufferOut.remaining());
		for(var i = 0; i < byteToCopy; ++i) {
			bufferOut.put(bufferIn.get());
		}
		return !bufferOut.hasRemaining();
	}

	
	//MAIN
	
	public static void main(String[] args) {
		try {
            var keyPair = UGEncrypt.KeyPairRSA.generate();
            var publicKey = keyPair.publicKey();
//            var privateKey = keyPair.privateKey();
            
            test1(publicKey);
            test2(publicKey);
            test3(publicKey);
			
		} catch(NoSuchAlgorithmException nsae) {
			nsae.getCause();
		}

    }
	
	// Test avec un buffer normal + 1 clé
	private static void test1(PublicKeyRSA publicKey) {
		System.out.println("TEST 1");
		var pubBuffer = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
        publicKey.to(pubBuffer);

        var nbb = ByteBuffer.allocate(Integer.BYTES + pubBuffer.flip().remaining());
        nbb.putInt(pubBuffer.remaining()).put(pubBuffer);

        // Tester le PublicKeyReader
        var reader = new PublicKeyReader();
        var status = reader.process(nbb);
        if(status == ProcessStatus.DONE) {
            var recoveredPubKey = reader.get();
            System.out.println("Clé publique lue : " + recoveredPubKey);
        } else {
            System.out.println("Erreur dans le processus de lecture de la clé publique.");
        }
	}
	
	// Test avec un buffer de taille 1 + 1 clé
	private static void test2(PublicKeyRSA publicKey) {
		System.out.println("TEST 2");
		var pubBuffer = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
        publicKey.to(pubBuffer);

        var nbb = ByteBuffer.allocate(Integer.BYTES + pubBuffer.flip().remaining());
        nbb.putInt(pubBuffer.remaining()).put(pubBuffer).flip();
        // Tester le PublicKeyReader
        var reader = new PublicKeyReader();
        var buffer = ByteBuffer.allocate(1);
    	var status = ProcessStatus.ERROR;
        while(nbb.hasRemaining()) {
        	buffer.clear().put(nbb.get());
            status = reader.process(buffer);
            buffer.clear();
        }
        if(status == ProcessStatus.DONE) {
            var recoveredPubKey = reader.get();
            System.out.println("Clé publique lue : " + recoveredPubKey);
        } else {
            System.out.println("Erreur dans le processus de lecture de la clé publique.");
        }
	}
	
	// Test avec un buffer normal + 2 clés
	private static void test3(PublicKeyRSA publicKey) throws NoSuchAlgorithmException {        
		System.out.println("TEST 3");
		//CLÉ 1
		var pubBuffer = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
        publicKey.to(pubBuffer);
        //CLÉ 2
        var keyPair = UGEncrypt.KeyPairRSA.generate();
        var publicKey2 = keyPair.publicKey();
        var pubBuffer2 = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
        publicKey2.to(pubBuffer2);

        var nbb = ByteBuffer.allocate(2 * Integer.BYTES + pubBuffer.flip().remaining() + pubBuffer2.flip().remaining());
        nbb.putInt(pubBuffer.remaining()).put(pubBuffer);
        nbb.putInt(pubBuffer2.remaining()).put(pubBuffer2);
        // Tester le PublicKeyReader
        var reader = new PublicKeyReader();
        var status = ProcessStatus.REFILL;
        while(nbb.position() != 0 || status == ProcessStatus.REFILL) {
            status = reader.process(nbb);
            if(status == ProcessStatus.DONE) {
                var recoveredPubKey = reader.get();
                System.out.println("Clé publique lue : " + recoveredPubKey);
                reader.reset();
            } else {
                System.out.println("Erreur dans le processus de lecture de la clé publique.");
            }
        }
	}
}







