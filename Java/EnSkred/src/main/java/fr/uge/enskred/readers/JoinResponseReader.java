package fr.uge.enskred.readers;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import fr.uge.enskred.paquet.Connexion;
import fr.uge.enskred.paquet.JoinResponse;
import fr.uge.enskred.paquet.Node;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;


/**
 * JoinResponseReader est un {@link Reader} capable de reconstruire un objet {@link JoinResponse}
 * à partir d’un flux binaire dans un {@link ByteBuffer}. Ce flux contient :
 * <ol>
 *   <li>Une clé publique RSA (via {@link PublicKeyReader})</li>
 *   <li>Une liste de {@link Node} (via {@link ListReader})</li>
 *   <li>Une liste de {@link Connexion} (via {@link ListReader})</li>
 * </ol>
 * 
 * ---
 * 
 * États internes :
 * <ul>
 *   <li>{@code WAITING_PUBLIC_KEY} : en attente de la clé publique</li>
 *   <li>{@code WAITING_NODES} : en attente de la liste de nœuds</li>
 *   <li>{@code WAITING_CONNEXIONS} : en attente de la liste de connexions</li>
 *   <li>{@code DONE} : toutes les données ont été lues et l’objet a été construit</li>
 *   <li>{@code ERROR} : une erreur a été rencontrée</li>
 * </ul>
 * 
 * ---
 * 
 * @method process(ByteBuffer buffer) : lit séquentiellement les données dans l’ordre défini 
 * pour reconstruire un {@link JoinResponse}. Retourne {@link ProcessStatus#REFILL} tant que 
 * les données sont incomplètes, et {@link ProcessStatus#DONE} une fois l’objet construit.
 * 
 * @method get() : retourne le {@link JoinResponse} construit. Lève une {@link IllegalStateException}
 * si l’état n’est pas {@code DONE}.
 * 
 * @method reset() : réinitialise le lecteur et ses sous-lecteurs pour permettre une nouvelle lecture.
 * 
 * ---
 * 
 * @implNote Utilise trois lecteurs internes :
 * <ul>
 *   <li>{@link PublicKeyReader} pour la clé publique</li>
 *   <li>{@link ListReader} avec {@link NodeReader} pour la liste des nœuds</li>
 *   <li>{@link ListReader} avec {@link ConnexionReader} pour la liste des connexions</li>
 * </ul>
 * Les listes sont copiées en profondeur pour éviter les effets de bord.
 * 
 * ---
 * 
 * Exemple d’utilisation :
 * <pre>
 * var buffer = joinResponse.getWriteModeBuffer();
 * buffer.flip();
 * var reader = new JoinResponseReader();
 * if (reader.process(buffer) == ProcessStatus.DONE) {
 *     var result = reader.get();
 * }
 * </pre>
 * 
 * ---
 * 
 * Méthode de test incluse dans {@code main} :
 * <ul>
 *   <li>Construit un {@code JoinResponse} avec des données de test</li>
 *   <li>Le sérialise dans un {@code ByteBuffer}</li>
 *   <li>Utilise le {@code JoinResponseReader} pour le reconstruire</li>
 *   <li>Compare les deux objets</li>
 * </ul>
 * 
 * ---
 * 
 * @throws IllegalStateException si {@code get()} est appelé sans que le {@code process()} ait retourné {@code DONE}.
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class JoinResponseReader implements Reader<JoinResponse> {
	private enum State {
		DONE, WAITING_PUBLIC_KEY, WAITING_NODES, WAITING_CONNEXIONS, ERROR
	}
	
	private final PublicKeyReader PKReader = new PublicKeyReader();
	private final ListReader<Node> listNodeReader = new ListReader<>(new NodeReader());
	private final ListReader<Connexion> listConnexionReader = new ListReader<>(new ConnexionReader());
	private State state = State.WAITING_PUBLIC_KEY;
	private PublicKeyRSA publicKey;
	private List<Node> nodes;
	private List<Connexion> connexions;
	private JoinResponse joinResponse;
	
	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		var situation = ProcessStatus.REFILL;
		switch(state) {
			case WAITING_PUBLIC_KEY:
				situation = PKReader.process(buffer);
				if(situation != ProcessStatus.DONE) {
					return situation;
				}
				publicKey = PKReader.get();
				state = State.WAITING_NODES;
			case WAITING_NODES:
				situation = listNodeReader.process(buffer);
				if(situation != ProcessStatus.DONE) {
					return situation;
				}
				nodes = listNodeReader.get();
				state = State.WAITING_CONNEXIONS;
			case WAITING_CONNEXIONS:
				situation = listConnexionReader.process(buffer);
				if(situation != ProcessStatus.DONE) {
					return situation;
				}
				connexions = listConnexionReader.get();
				state = State.DONE;
				joinResponse = new JoinResponse(publicKey, new ArrayList<>(nodes), new ArrayList<>(connexions));
				return ProcessStatus.DONE;
			default:
				state = State.ERROR;return ProcessStatus.ERROR;
		}
	}

	@Override
	public JoinResponse get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return joinResponse;
	}

	@Override
	public void reset() {
		state = State.WAITING_PUBLIC_KEY;
		PKReader.reset();
		listNodeReader.reset();
		listConnexionReader.reset();
		publicKey = null;
		Utils.safeClear(nodes);
		Utils.safeClear(connexions);
	}
	
	
	//MAIN
    public static void main(String[] args) throws NoSuchAlgorithmException {
        var testPublicKey = UGEncrypt.KeyPairRSA.generate().publicKey();
        var testNodes = List.of(new Node(testPublicKey, new InetSocketAddress("127.0.0.1", 8080)));
        var testConnexions = List.of(new Connexion(testPublicKey, testPublicKey));

        var originalResponse = new JoinResponse(testPublicKey, testNodes, testConnexions);

        var buffer = originalResponse.getWriteModeBuffer();
        System.out.println(buffer.flip().get());
        buffer.compact();

        var reader = new JoinResponseReader();
        if(reader.process(buffer) != ProcessStatus.DONE) {
            System.err.println("Erreur : lecture incomplète !");
            return;
        }

        var reconstructedResponse = reader.get();

        System.out.println("TEST : Comparaison des objets...");
        System.out.println("Original  : " + originalResponse);
        System.out.println("Reconstruit : " + reconstructedResponse);
        
        boolean success = originalResponse.equals(reconstructedResponse);
        System.out.println(success ? "TEST RÉUSSI !" : "TEST ÉCHOUÉ !");
    }

}
