package fr.uge.enskred.readers;

import java.nio.ByteBuffer;

import fr.uge.enskred.paquet.MessageToSecure;
import fr.uge.enskred.readers.UGEncrypt.KeyPairRSA;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;



/**
 * Reader pour la lecture d'un {@link MessageToSecure} depuis un {@link ByteBuffer}.
 * 
 * <p>Ce lecteur lit un message sécurisé en plusieurs étapes : d'abord la clé publique du sender (via un {@link PublicKeyReader}),
 * ensuite l'ID du message (via un {@link LongReader}), et enfin le message lui-même (via un {@link StringReader}).</p>
 * 
 * <p>Le processus de lecture se fait de manière incrémentale, chaque étape étant gérée indépendamment.</p>
 * 
 * <p>Les états possibles pour cette classe sont les suivants :</p>
 * <ul>
 *   <li>WAITING_PUBLIC_KEY : en attente de la clé publique du sender.</li>
 *   <li>WAITING_ID_MESSAGE : en attente de l'ID du message.</li>
 *   <li>WAITING_MESSAGE : en attente des données du message.</li>
 *   <li>DONE : lecture terminée, un {@link MessageToSecure} peut être récupéré via {@code get()}.</li>
 *   <li>ERROR : une erreur s’est produite, généralement en cas d'appel invalide ou de mauvaise donnée lue.</li>
 * </ul>
 * 
 * <p>Un exemple d'utilisation typique consiste à appeler la méthode {@code process()} sur le buffer de données, 
 * et une fois que la lecture est terminée, récupérer le message avec {@code get()}.</p>
 * 
 * ---
 * 
 * Exemple d'utilisation :
 * <pre>{@code
 *   MessageToSecureReader reader = new MessageToSecureReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {//ATENTE ACTIVE
 *     // Attendre plus de données
 *   }
 *   MessageToSecure message = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class MessageToSecureReader implements Reader<MessageToSecure> {
    private enum State {
        DONE, WAITING_PUBLIC_KEY,  WAITING_ID_MESSAGE, WAITING_MESSAGE, ERROR
    }

    private final PublicKeyReader pkReader = new PublicKeyReader();
    private final StringReader stringReader = new StringReader();
    private final LongReader longReader = new LongReader();
    private State state = State.WAITING_PUBLIC_KEY;
    private MessageToSecure result;
    private ProcessStatus status;
    private PublicKeyRSA sender;
    private long idMessage;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        switch (state) {
            case WAITING_PUBLIC_KEY :
                status = pkReader.process(buffer);
                if(status != ProcessStatus.DONE) {
                	return status;
                }
                sender = pkReader.get();
                state = State.WAITING_ID_MESSAGE;
            case WAITING_ID_MESSAGE:
                status = longReader.process(buffer);
                if(status != ProcessStatus.DONE) {
                    return status;
                }
            	idMessage = longReader.get();
                state = State.WAITING_MESSAGE;
            case WAITING_MESSAGE :
                status = stringReader.process(buffer);
                if(status != ProcessStatus.DONE) {
                    return status;
                }
                var message = stringReader.get();
                result = new MessageToSecure(sender, idMessage, message);
                state = State.DONE;
                return ProcessStatus.DONE;
            default :
                state = State.ERROR;
                return ProcessStatus.ERROR;
        }
    }

    @Override
    public MessageToSecure get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return result;
    }

    @Override
    public void reset() {
        state = State.WAITING_PUBLIC_KEY;
        stringReader.reset();
        longReader.reset();
        pkReader.reset();
        result = null;
    }
    
    public static void main(String[] args) throws Exception {
        // Étape 1 : Générer une paire de clés
        var keyPair = KeyPairRSA.generate();
        var senderPublicKey = keyPair.publicKey();

        // Étape 2 : Créer un MessageToSecure avec un message
        String content = "Hello world!";
        MessageToSecure message = new MessageToSecure(senderPublicKey, System.currentTimeMillis(), content);
        var buff = message.getWriteModeBuffer();
        System.out.println("Deb! " + buff.flip().remaining() + " __ " + buff.remaining());
        var BB = buff.get();
        
        buff.compact();
        System.out.println("Fin! " + BB + " _-_ "+ buff.remaining());

        // Étape 4 : Lire le message via le reader
        var reader = new MessageToSecureReader();
        reader.process(buff);
        var result = reader.get();
        
        // Étape 5 : Afficher le résultat
        if (result != null) {
            System.out.println("Message reçu : " + result.message());
            System.out.println("Clé publique du sender (hash) : " + result.sender().hashCode());
        } else {
            System.out.println("Lecture incomplète ou échouée.");
        }
    }	
}
