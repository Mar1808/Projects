package fr.uge.enskred.readers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import fr.uge.enskred.paquet.NewNode;
import fr.uge.enskred.readers.UGEncrypt.KeyPairRSA;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;



/**
 * Reader pour la lecture d'un {@link NewNode} depuis un {@link ByteBuffer}.
 * 
 * <p>Ce lecteur lit un objet {@link NewNode} en plusieurs étapes : d'abord la clé publique du sender (via un {@link PublicKeyReader}),
 * ensuite l'adresse du socket du sender (via un {@link SocketAddressReader}), puis la clé publique du receiver (via un autre {@link PublicKeyReader}).</p>
 * 
 * <p>Les étapes de lecture sont effectuées dans un ordre précis, et le résultat final peut être obtenu une fois que toutes les données ont été lues et validées.</p>
 * 
 * <p>Les états possibles pour cette classe sont les suivants :</p>
 * <ul>
 *   <li>WAITING_ID_SENDER : en attente de la clé publique du sender.</li>
 *   <li>WAITING_SOCKETADDRESS_SENDER : en attente de l'adresse du socket du sender.</li>
 *   <li>WAITING_ID_RECEIVER : en attente de la clé publique du receiver.</li>
 *   <li>DONE : lecture terminée, un {@link NewNode} peut être récupéré via {@code get()}.</li>
 *   <li>ERROR : une erreur s’est produite, généralement en cas d'appel invalide ou de mauvaise donnée lue.</li>
 * </ul>
 * 
 * <p>Un exemple d'utilisation consiste à appeler la méthode {@code process()} sur un buffer de données, et une fois que la lecture est terminée,
 * récupérer le {@link NewNode} avec {@code get()}.</p>
 * 
 * ---
 * 
 * Exemple d'utilisation :
 * <pre>{@code
 *   NewNodeReader reader = new NewNodeReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {//ATTENTE ACTIVE
 *     // Attendre plus de données
 *   }
 *   NewNode newNode = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class NewNodeReader implements Reader<NewNode> {
    private enum State {
    	WAITING_ID_SENDER, WAITING_SOCKETADDRESS_SENDER, WAITING_ID_RECEIVER, DONE, ERROR
    }

    private final SocketAddressReader socketAddressReader = new SocketAddressReader(); 
    private final PublicKeyReader PKReader = new PublicKeyReader();
    private State state = State.WAITING_ID_SENDER;
    private InetSocketAddress socketAddressSender;
    private ProcessStatus localStatus;
    private PublicKeyRSA idReceiver;
    private PublicKeyRSA idSender;
    private NewNode newNode;

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
                PKReader.reset();
                state = State.WAITING_SOCKETADDRESS_SENDER;
            case WAITING_SOCKETADDRESS_SENDER:
            	localStatus = socketAddressReader.process(buffer);
                if(localStatus == ProcessStatus.REFILL) {
                    return ProcessStatus.REFILL;
                }
                socketAddressSender = socketAddressReader.get();
                state = State.WAITING_ID_RECEIVER;
            case WAITING_ID_RECEIVER:
            	localStatus = PKReader.process(buffer);
                if(localStatus == ProcessStatus.REFILL) {
                    return ProcessStatus.REFILL;
                }
                idReceiver = PKReader.get();
                newNode = new NewNode(idSender, socketAddressSender, idReceiver);
                state = State.DONE;
                return ProcessStatus.DONE;
            
            default:
                state = State.ERROR;
                return ProcessStatus.ERROR;
        }
    }

    @Override
    public NewNode get() {
        if (state != State.DONE) {
            throw new IllegalStateException("State is not DONE");
        }
        return newNode;
    }

    @Override
    public void reset() {
        state = State.WAITING_ID_SENDER;
        socketAddressReader.reset();
        socketAddressSender = null;
        idReceiver = null;
        PKReader.reset();
        idSender = null;
        newNode = null;
    }

    
    //MAIN--------------
    public static void main(String[] args) {
        try {
            var keyPairSender = KeyPairRSA.generate();
            var publicKeySender = keyPairSender.publicKey();

            var keyPairReceiver = KeyPairRSA.generate();
            var publicKeyReceiver = keyPairReceiver.publicKey();

            var socketAddressSender = new InetSocketAddress("127.0.0.1", 8080);

            var newNode = new NewNode(publicKeySender, socketAddressSender, publicKeyReceiver);
            var buffer = newNode.getWriteModeBuffer().flip();
            buffer.get();
            buffer.compact();
            var reader = new NewNodeReader();
            var status = reader.process(buffer);

            if(status == ProcessStatus.DONE) {
                var result = reader.get();
                System.out.println("Test réussi ! NewNode lu : \n" + result);
            } else {
                System.out.println("Test échoué. Statut : " + status);
            }
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur lors de la génération des clés RSA : " + e.getMessage());
        }
    }
    
}
