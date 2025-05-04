package fr.uge.enskred.readers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;

import fr.uge.enskred.paquet.Node;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;



/**
 * Reader pour la lecture d'un {@link Node} depuis un {@link ByteBuffer}.
 * 
 * <p>Ce lecteur permet de lire un objet {@link Node} en deux étapes : d'abord la clé publique (via un {@link PublicKeyReader}), puis l'adresse du socket (via un {@link SocketAddressReader}).</p>
 * 
 * <p>Les étapes de lecture sont effectuées dans un ordre précis. Le résultat final, un objet {@link Node}, est disponible après avoir traité toutes les informations nécessaires dans le buffer.</p>
 * 
 * <p>Les états possibles pour cette classe sont les suivants :</p>
 * <ul>
 *   <li>WAITING_KEY_RSA : en attente de la clé publique du {@link Node}.</li>
 *   <li>WAITING_SOCKET_ADDRESS : en attente de l'adresse du socket du {@link Node}.</li>
 *   <li>DONE : lecture terminée, un {@link Node} peut être récupéré via {@code get()}.</li>
 *   <li>ERROR : une erreur s’est produite, généralement en cas d'appel invalide ou de données incorrectes lues.</li>
 * </ul>
 * 
 * <p>Un exemple d'utilisation consiste à appeler la méthode {@code process()} sur un buffer contenant les données d'un {@link Node}, et une fois la lecture terminée,
 * récupérer le {@link Node} avec {@code get()}.</p>
 * 
 * ---
 * 
 * Exemple d'utilisation :
 * <pre>{@code
 *   NodeReader reader = new NodeReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {//ATTENTE ACTIVE
 *     // Attendre plus de données
 *   }
 *   Node node = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class NodeReader implements Reader<Node> {
	private final SocketAddressReader socketAddressReader = new SocketAddressReader();
	private final PublicKeyReader publicKeyReader = new PublicKeyReader();
	private State state = State.WAITING_KEY_RSA;
	private InetSocketAddress socketAddress;
	private PublicKeyRSA publicKey;
	private Node node;
	
	private enum State {
		WAITING_KEY_RSA, WAITING_SOCKET_ADDRESS, DONE, ERROR 
	}

	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException("State: " + state);
		}
		switch(state) {
			case WAITING_KEY_RSA:
				switch(publicKeyReader.process(buffer)) {
					case REFILL: return ProcessStatus.REFILL;
					case DONE:
						publicKey = publicKeyReader.get();
						state = State.WAITING_SOCKET_ADDRESS;
						break;
					default:	
						state = State.ERROR; 
						return ProcessStatus.ERROR;
				}
			case WAITING_SOCKET_ADDRESS:
				switch(socketAddressReader.process(buffer)) {
					case REFILL: return ProcessStatus.REFILL;
					case DONE:
						socketAddress = socketAddressReader.get();
						state = State.DONE;
						node = new Node(publicKey, socketAddress);
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
	public Node get() {
		if(state != State.DONE) {
			throw new IllegalStateException("State is not DONE");
		}
		return node;
	}


	@Override
	public void reset() {
		publicKey = null;
		socketAddress = null;
		publicKeyReader.reset();
		socketAddressReader.reset();
		state = State.WAITING_KEY_RSA;
	}
	
	
	
	//MAIN-------------
	public static void main(String[] args) {
        try {
            var keyPair = UGEncrypt.KeyPairRSA.generate();
            var publicKey = keyPair.publicKey();
            
            testSingleNodeInLargeBuffer(publicKey);
            testSingleNodeWithSmallBuffer(publicKey);
            testMultipleNodes(publicKey);

        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur lors de la génération de la clé RSA.");
        }
    }

    // Test 1 : Lire un Node dans un buffer assez grand
    private static void testSingleNodeInLargeBuffer(PublicKeyRSA publicKey) {
        System.out.println("TEST 1 : Lecture d'un Node avec un buffer suffisant");

        var socketAddress = new InetSocketAddress("192.168.1.50", 8080);
        var buffer = new Node(publicKey, socketAddress).getWriteModeBuffer();
        var nodeReader = new NodeReader();
        var status = nodeReader.process(buffer);

        if (status == ProcessStatus.DONE) {
            var node = nodeReader.get();
            System.out.println("Test 1 réussi. Node lu : " + node);
        } else {
            System.out.println("Test 1 échoué. Statut : " + status);
        }
    }

    // Test 2 : Lire un Node avec un buffer trop petit (lecture en plusieurs étapes)
    private static void testSingleNodeWithSmallBuffer(PublicKeyRSA publicKey) {
        System.out.println("TEST 2 : Lecture d'un Node avec un buffer trop petit");

        var socketAddress = new InetSocketAddress("10.0.0.1", 1234);
        var fullBuffer = new Node(publicKey, socketAddress).getWriteModeBuffer().flip();
        var nodeReader = new NodeReader();
        var smallBuffer = ByteBuffer.allocate(1);
        var status = ProcessStatus.REFILL;

        while (fullBuffer.hasRemaining() || status == ProcessStatus.REFILL) {
            smallBuffer.clear().put(fullBuffer.get());
            status = nodeReader.process(smallBuffer);
            smallBuffer.clear();
        }

        if (status == ProcessStatus.DONE) {
            var node = nodeReader.get();
            System.out.println("Test 2 réussi. Node lu : " + node);
        } else {
            System.out.println("Test 2 échoué. Statut : " + status);
        }
    }

    // Test 3 : Lire plusieurs Nodes dans un même buffer
    private static void testMultipleNodes(PublicKeyRSA publicKey) throws NoSuchAlgorithmException {
        System.out.println("TEST 3 : Lecture de plusieurs Nodes consécutifs");

        var keyPair2 = UGEncrypt.KeyPairRSA.generate();
        var publicKey2 = keyPair2.publicKey();

        var socketAddress1 = new InetSocketAddress("192.168.1.60", 5000);
        var socketAddress2 = new InetSocketAddress("172.16.0.1", 7000);

        var buffer1 = new Node(publicKey, socketAddress1).getWriteModeBuffer().flip();
        var buffer2 = new Node(publicKey2, socketAddress2).getWriteModeBuffer().flip();
        var buffer = ByteBuffer.allocate(buffer1.remaining() + buffer2.remaining());
        buffer.put(buffer1).put(buffer2);

        var nodeReader = new NodeReader();
        var status = ProcessStatus.REFILL;

        while(buffer.position() != 0 || status == ProcessStatus.REFILL) {
            status = nodeReader.process(buffer);
            if (status == ProcessStatus.DONE) {
                var node = nodeReader.get();
                System.out.println("Node lu : " + node);
                nodeReader.reset();
            }
        }
    }

}
	
