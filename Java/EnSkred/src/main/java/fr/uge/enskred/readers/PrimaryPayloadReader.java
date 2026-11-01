package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.paquet.NewConnection;
import fr.uge.enskred.paquet.Payload;
import fr.uge.enskred.paquet.RemoveNode;



/**
 * Reader pour la lecture du payload d'un paquet à partir d'un {@link ByteBuffer}.
 * 
 * <p>Cette classe permet de décoder le payload d'un paquet en fonction de l'opcode contenu dans celui-ci. Elle est capable de lire différents types de payloads, en fonction des opcodes, tels que :</p>
 * <ul>
 *   <li>NEW_NODE : Lit un payload de type "New Node".</li>
 *   <li>NEW_CONNECTION : Lit un payload de type "New Connection".</li>
 *   <li>REMOVE_NODE : Lit un payload de type "Remove Node".</li>
 * </ul>
 * 
 * <p>Le traitement se fait en plusieurs étapes, et l'état de la lecture suit le cycle suivant :</p>
 * <ul>
 *   <li>WAITING_OPCODE : En attente de la lecture de l'opcode du paquet.</li>
 *   <li>WAITING_PAYLOAD : En attente de la lecture du payload associé à l'opcode.</li>
 *   <li>DONE : Le payload a été entièrement lu et peut être récupéré via {@code get()}.</li>
 *   <li>ERROR : Une erreur s'est produite pendant la lecture du payload.</li>
 * </ul>
 * 
 * <p>Une fois l'opcode et le payload validés, ils peuvent être récupérés via la méthode {@code get()} sous forme d'un objet {@link Payload} correspondant au type de payload décodé.</p>
 * 
 * ---
 * 
 * Exemple d'utilisation :
 * <pre>{@code
 *   PrimaryPayloadReader reader = new PrimaryPayloadReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {
 *     // Attendre plus de données
 *   }
 *   Payload payload = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * Tests unitaires :
 * Cette classe contient des tests permettant de valider la lecture des payloads associés à différents opcodes, comme `NEW_NODE`, `NEW_CONNECTION` et `REMOVE_NODE`.
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class PrimaryPayloadReader implements Reader<Payload> {
	private enum State {
		DONE, WAITING_OPCODE, WAITING_PAYLOAD, ERROR
	};
	
	private static final Logger logger = Logger.getLogger(PrimaryPayloadReader.class.getName());

	private ProcessStatus localStatus = ProcessStatus.REFILL;
	private State state = State.WAITING_OPCODE;
	private Payload payload;
	private OpCode opCode;
	//readers
	private final ByteReader byteReader = new ByteReader();
	private final NewNodeReader newNodeReader = new NewNodeReader();
	private final ConnexionReader connexionReader = new ConnexionReader();
	private final PublicKeyReader publicKeyReader = new PublicKeyReader();
	
	public PrimaryPayloadReader(Level level) {
		logger.setLevel(level == null ? Level.SEVERE : level);
	}
	
	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch(state) {
			//On va récuperer l'opcode du paquet !
			case WAITING_OPCODE:
				localStatus = byteReader.process(buffer);
				if(localStatus != ProcessStatus.DONE) {
					return localStatus;
				}
				opCode = OpCode.intToOpCode(byteReader.get());
				state = State.WAITING_PAYLOAD;
			case WAITING_PAYLOAD:
				switch(opCode) {
					case NEW_NODE -> {
						payload = readNewNode(buffer);
					}
					case NEW_CONNECTION -> {
						payload = readNewConnexion(buffer);
					}
					case REMOVE_NODE -> {
						payload = readRemoveNode(buffer);
					}
					default -> { logger.info("Error with waitingPayload"); }
				}
	
				if(localStatus != ProcessStatus.DONE || null == payload) {
					return localStatus;
				}
				state = State.DONE;
				return ProcessStatus.DONE;
			default:
				state = State.ERROR;return ProcessStatus.ERROR;
		}
	}

	@Override
	public Payload get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return payload;
	}

	@Override
	public void reset() {
		localStatus = ProcessStatus.REFILL;
		state = State.WAITING_OPCODE;
		opCode = OpCode.NO_STATE;
		payload = null;
		byteReader.reset();
		newNodeReader.reset();
		publicKeyReader.reset();
		connexionReader.reset();
	}

	
	//PRIVATE METHODS
	/**
	 * Méthode pour les payload NewNode
	 * ---
	 * @param buffer: buffer
	 * @return Renvoie une payload de type NewNode
	 */
	private Payload readNewNode(ByteBuffer buffer) {
		localStatus = newNodeReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return newNodeReader.get(); }
			case ERROR -> 	{ logger.info("Error with readNewNode"); }
		}
		//tmp
		return null;
	}	
	
	/**
	 * Lis une payload de type newConnexion
	 * ---
	 * @param buffer: bufferPayload
	 * @return Payload de type NewConnexion
	 */
	private Payload readNewConnexion(ByteBuffer buffer) {
		localStatus = connexionReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ 
				var connexion = connexionReader.get();
				return new NewConnection(connexion.publicKeySender(), connexion.publicKeyReceiver());
			}
			case ERROR -> 	{ logger.info("Error with readNewConnexion"); }
		}
		//tmp
		return null;
	}


	/**
	 * Lis une payload de type RemoveNode
	 * ---
	 * @param buffer: bufferPayload
	 * @return Payload de type RemoveNode
	 */
	private Payload readRemoveNode(ByteBuffer buffer) {
		localStatus = publicKeyReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return new RemoveNode(publicKeyReader.get()); }
			case ERROR -> 	{ logger.info("Error with readNewConnexion"); }
		}
		//tmp
		return null;
	}
	
}
