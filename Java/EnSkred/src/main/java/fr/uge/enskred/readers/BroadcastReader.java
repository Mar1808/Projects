package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import fr.uge.enskred.paquet.Broadcast;
import fr.uge.enskred.readers.UGEncrypt.KeyPairRSA;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * BroadcastReader est un {@link Reader} permettant de désérialiser un objet {@link fr.uge.enskred.paquet.Broadcast}
 * à partir d'un {@link ByteBuffer}, selon le format suivant :
 * 
 * <pre>
 * [PublicKeyRSA] [long messageID] [int taille du payload] [byte[] payload]
 * </pre>
 * 
 * ---
 * Ce reader lit :
 * <ul>
 *   <li>Une clé publique RSA du sender (via {@link PublicKeyReader})</li>
 *   <li>Un identifiant de message (long)</li>
 *   <li>La taille du message en octets (int)</li>
 *   <li>Le contenu du message sous forme de {@code ByteBuffer}</li>
 * </ul>
 * et retourne un objet {@link Broadcast} une fois tous les champs correctement lus.
 * ---
 * États internes :
 * <ul>
 *   <li>{@code WAITING_ID_SENDER} : en attente de lecture de la clé publique</li>
 *   <li>{@code WAITING_MESSAGE_ID} : en attente de lecture de l'identifiant du message</li>
 *   <li>{@code WAITING_SIZE_PAYLOAD} : en attente de lecture de la taille du message</li>
 *   <li>{@code WAITING_PAYLOAD} : en attente de lecture du contenu du message</li>
 *   <li>{@code DONE} : message lu avec succès</li>
 *   <li>{@code ERROR} : erreur détectée lors de la lecture</li>
 * </ul>
 * 
 * ---

 * Méthodes principales :
 * @method process(ByteBuffer buffer) : traite le contenu du buffer étape par étape jusqu’à lecture complète du Broadcast.
 * @method get() : retourne le {@link Broadcast} construit une fois la lecture terminée (état DONE).
 * @method reset() : réinitialise complètement l'état pour permettre une nouvelle lecture.
 * ---
 * 
 * @implNote La lecture du payload se fait en plusieurs passes si nécessaire. La méthode utilise {@code buffer.flip()} puis
 * {@code buffer.compact()} dans une section {@code try/finally} pour garantir une gestion correcte du buffer.
 * ---
 * Exemple d’utilisation :
 * <pre>
 * ByteBuffer buffer = ...; // contient un ou plusieurs objets Broadcast
 * var reader = new BroadcastReader();
 * if (reader.process(buffer) == ProcessStatus.DONE) {
 *     Broadcast message = reader.get();
 * }
 * </pre>
 * 
 * ---
 * 
 * @throws IllegalStateException si {@code get()} est appelé alors que l’état interne n’est pas {@code DONE}
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */

public final class BroadcastReader implements Reader<Broadcast> {
    private enum State {
    	WAITING_ID_SENDER, WAITING_MESSAGE_ID, WAITING_SIZE_PAYLOAD, WAITING_PAYLOAD, DONE, ERROR
    }
    
    private final PublicKeyReader PKReader = new PublicKeyReader();
    private final LongReader longReader = new LongReader();
    private final IntReader intReader = new IntReader();
    private State state = State.WAITING_ID_SENDER;
    private ProcessStatus localStatus;
    private PublicKeyRSA idSender;
    private Broadcast broadcast;
    private ByteBuffer payload;
    private long messageID;
    private int size;


    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("State: " + state);
        }
        
        switch (state) {
            case WAITING_ID_SENDER:
                localStatus = PKReader.process(buffer);
                if(localStatus == ProcessStatus.REFILL) {
                    return ProcessStatus.REFILL;
                }
                idSender = PKReader.get();
                state = State.WAITING_MESSAGE_ID;
            case WAITING_MESSAGE_ID:
                localStatus = longReader.process(buffer);
                if(localStatus == ProcessStatus.REFILL) {
                    return ProcessStatus.REFILL;
                }
                messageID = longReader.get();
                state = State.WAITING_SIZE_PAYLOAD;
            case WAITING_SIZE_PAYLOAD:
                localStatus = intReader.process(buffer);
                if(localStatus == ProcessStatus.REFILL) {
                    return ProcessStatus.REFILL;
                }
                size = intReader.get();
                if(size < 0) {
                	state = State.ERROR;
                	return ProcessStatus.ERROR;
                }
                state = State.WAITING_PAYLOAD;
                payload = ByteBuffer.allocate(size);
            case WAITING_PAYLOAD:
            	try {
                	buffer.flip();
                	var reading = Utils.min(buffer.remaining(), payload.remaining());
                	payload.put(buffer.array(), buffer.position(), reading);
                	buffer.position(buffer.position() + reading);//maj
                	if(payload.hasRemaining()) {
                    	return ProcessStatus.REFILL;
                    }
                    broadcast = new Broadcast(idSender, messageID, size, payload);
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

    @Override
    public Broadcast get() {
        if (state != State.DONE) {
            throw new IllegalStateException("State is not DONE");
        }
        return broadcast;
    }

    @Override
    public void reset() {
        state = State.WAITING_ID_SENDER;
        PKReader.reset();
        intReader.reset();
        longReader.reset();
        broadcast = null;
        idSender = null;
        payload = null;
    }
    
    
    //Main
    public static void main(String[] args) {
        try {
            //génération d'une clé publique pour le sender
            var keyPairSender = KeyPairRSA.generate();
            var publicKeySender = keyPairSender.publicKey();

            //création d'un payload simple
            var message = "Hello, Broadcast!";
            var payloadBytes = message.getBytes();
            var payloadBuffer = ByteBuffer.allocate(payloadBytes.length);
            payloadBuffer.put(payloadBytes); // Passer en mode lecture

            //création de l'objet Broadcast
            var messageID = 12345L;
            var broadcast = new Broadcast(publicKeySender, messageID, payloadBytes.length, payloadBuffer);
            var broadcast2 = new Broadcast(publicKeySender, messageID+222, payloadBytes.length, payloadBuffer);

            //sérialisation en ByteBuffer
            var buffer = broadcast.getWriteModeBuffer().flip();
            var buffer2 = broadcast2.getWriteModeBuffer().flip();
            var bigus = ByteBuffer.allocate(buffer.remaining() + buffer2.remaining());
            bigus.put(buffer).put(buffer2);

            //création du BroadcastReader et test
            var reader = new BroadcastReader();
            var status = reader.process(bigus);

            if (status == ProcessStatus.DONE) {
                var result = reader.get();
                System.out.println("Test réussi ! Broadcast lu : \n" + result);
            } else {
                System.out.println("Test échoué. Statut : " + status);
            }
            reader.reset();
            status = reader.process(bigus);

            if (status == ProcessStatus.DONE) {
                var result = reader.get();
                System.out.println("Test réussi ! Broadcast lu : \n" + result);
            } else {
                System.out.println("Test échoué. Statut : " + status);
            }
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur lors de la génération des clés RSA : " + e.getMessage());
        }
    }
}
