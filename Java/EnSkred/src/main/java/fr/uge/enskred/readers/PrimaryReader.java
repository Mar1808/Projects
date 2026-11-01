package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.paquet.ChallengeLongResponse;
import fr.uge.enskred.paquet.ChallengeOk;
import fr.uge.enskred.paquet.EncodedRSABuffers;
import fr.uge.enskred.paquet.LeaveNetworkAsk;
import fr.uge.enskred.paquet.LeaveNetworkCancel;
import fr.uge.enskred.paquet.LeaveNetworkConfirm;
import fr.uge.enskred.paquet.LeaveNetworkDone;
import fr.uge.enskred.paquet.LeaveNetworkResponse;
import fr.uge.enskred.paquet.Node;
import fr.uge.enskred.paquet.Paquet;
import fr.uge.enskred.paquet.PreJoin;
import fr.uge.enskred.paquet.SecondJoin;


/**
 * La classe `PrimaryReader` implémente l'interface `Reader<Paquet>` et est responsable de la gestion de la lecture de paquets dans un protocole réseau spécifique. 
 * Elle traite les paquets reçus dans un buffer et les convertit en objets de type `Paquet`, en fonction de l'opcode identifié. 
 * La classe gère différents types de paquets, tels que ceux relatifs à la connexion, la messagerie sécurisée, les broadcasts, et la déconnexion. 
 * Chaque type de paquet est décodé par un lecteur dédié et la classe maintient un état interne pour suivre le processus de lecture.
 * 
 * ---
 * 
 * 
 * Cette classe gère les étapes suivantes :
 * - Lecture et décodage des paquets en fonction de l'opcode
 * - Gestion des erreurs et de l'état de la lecture
 * - Traitement des paquets de type `PreJoin`, `SecondJoin`, `Challenge`, `JoinResponse`, `Broadcast`, `Message`, `LeaveNetwork`, etc.
 * 
 * Les paquets sont décodés par des objets de type `Reader` spécifiques à chaque type de paquet, par exemple, `RSAReader`, `NodeReader`, `LongReader`, etc.
 * Chaque type de paquet est traité de manière asynchrone, en utilisant une machine d'état pour suivre l'avancement de la lecture.
 * 
 * 
 * @see Paquet
 * @see Reader
 * 
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class PrimaryReader implements Reader<Paquet> {
	private enum State {
		DONE, WAITING_OPCODE, WAITING_PAQUET, ERROR
	}
	
	private static final Logger logger = Logger.getLogger(PrimaryReader.class.getName());

	private ProcessStatus localStatus = ProcessStatus.REFILL;
	private State state = State.WAITING_OPCODE;
	private OpCode opCode;
	private Paquet paquet;
	//readers
	private final RSAReader rsaReader = new RSAReader();
	private final ByteReader byteReader = new ByteReader();
	private final LongReader longReader = new LongReader();
	private final NodeReader nodeReader = new NodeReader();
	private final BroadcastReader broadcastReader = new BroadcastReader();
	private final PublicKeyReader publicKeyReader = new PublicKeyReader();
	private final JoinResponseReader joinResponseReader = new JoinResponseReader();
	private final ListReader<Node> listReader = new ListReader<>(new NodeReader());
	private final MessagePublicReader messagePublicReader = new MessagePublicReader();

	public PrimaryReader(Level level) {
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
			byteReader.reset();
			state = State.WAITING_PAQUET;
		case WAITING_PAQUET:
			switch(opCode) {
				/*****************************************
				 ******** GESTION DE LA CONNEXION ********
				 *****************************************/
				case PRE_JOIN 				-> { paquet = readPreJoin(buffer); }
				case SECOND_JOIN 			-> { paquet = readSecondJoin(buffer); }
				case CHALLENGE_PUBLIC_KEY 	-> { paquet = readBufferChallengePublicKey(buffer); }
				case RESPONSE_CHALLENGE 	-> { paquet = readLongChallengeResponse(buffer); }
				case CHALLENGE_OK 			-> { paquet = readChallengeOk(buffer); }
				case JOIN_RESPONSE 			-> { paquet = readBufferJoinResponse(buffer); }
				/*****************************************
				 ********* GESTION DES BROADCAST *********
				 *****************************************/
				case BROADCAST -> { paquet = readBufferBroadcast(buffer); }
				/*****************************************
				 ******** GESTION DE LA MESSAGERIE ******* 
				 *****************************************/
				case OPEN_MESSAGE 	-> { paquet = readBufferMessagePublic(buffer); }
				case SECURE_MESSAGE -> { paquet = readBufferEncodedBySecureMessage(buffer); }
				/*****************************************
				 ******* GESTION DE LA DÉCONNEXION *******
				 *****************************************/
				case LEAVE_NETWORK_ASK 		-> { localStatus = ProcessStatus.DONE; paquet = new LeaveNetworkAsk(); }
				case LEAVE_NETWORK_RESPONSE -> { paquet = readBufferLeaveNetworkResponse(buffer); }
				case LEAVE_NETWORK_CANCEL 	-> { localStatus = ProcessStatus.DONE; paquet = new LeaveNetworkCancel(); }
				case LEAVE_NETWORK_CONFIRM 	-> { paquet = readBufferLeaveNetworkConfirm(buffer); }
				case LEAVE_NETWORK_DONE 	-> { localStatus = ProcessStatus.DONE; paquet = new LeaveNetworkDone(); }
				default -> { logger.warning("Error with WAITING_PAQUET"); }
			}

			if(localStatus != ProcessStatus.DONE || null == paquet) {
				return localStatus;
			}
			state = State.DONE;
			return ProcessStatus.DONE;
		default:
			state = State.ERROR;return ProcessStatus.ERROR;
		}
	}

	@Override
	public Paquet get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return paquet;
	}

	@Override
	public void reset() {
		localStatus = ProcessStatus.REFILL;
		state = State.WAITING_OPCODE;
		opCode = OpCode.NO_STATE;
		paquet = null;
		rsaReader.reset();
		nodeReader.reset();
		listReader.reset();
		byteReader.reset();
		longReader.reset();
		broadcastReader.reset();
		publicKeyReader.reset();
		joinResponseReader.reset();
		messagePublicReader.reset();
	}

	//PRIVATE METHODS
	/**
	 * Méthode pour les paquets préjoin
	 * ---
	 * @param buffer
	 * @return Renvoie un PreJoin
	 */
	private Paquet readPreJoin(ByteBuffer buffer) {
		localStatus = nodeReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return new PreJoin(nodeReader.get());}
			case ERROR -> 	{ logger.info("Error with manageConnexion"); }
		}
		return null;
	}
	
	/**
	 * Méthode pour la lecture d'un paquet SecondJoin.
	 * ---
	 * Cette méthode lit les données nécessaires pour un paquet SecondJoin et le retourne sous forme d'un objet `SecondJoin`.
	 * 
	 * @param buffer Le buffer contenant les données à lire
	 * @return Un objet `SecondJoin` contenant les informations lues
	 */
	private Paquet readSecondJoin(ByteBuffer buffer) {
		localStatus = nodeReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return new SecondJoin(nodeReader.get());}
			case ERROR -> 	{ logger.info("Error with manageConnexion"); }
		}
		return null;
	}
	/**
	 * Méthode pour la lecture du challenge de connexion !
	 * ---
	 * @param buffer: bufferIn
	 * @return Renvoie un EncodedRSABuffers
	 */
	private Paquet readBufferChallengePublicKey(ByteBuffer buffer) {
		localStatus = rsaReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return new EncodedRSABuffers(rsaReader.get(), OpCode.CHALLENGE_PUBLIC_KEY); }
			case ERROR -> 	{ logger.info("Error with challengePublicKey"); }
		}
		return null;
	}

	/**
	 * Méthode pour la lecture du challenge de long
	 * --> coté récepteur
	 * ---
	 * @param buffer
	 * @return Renvoie un ChalengeLongResponse
	 */
	private Paquet readLongChallengeResponse(ByteBuffer buffer) {
		localStatus = longReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return new ChallengeLongResponse(longReader.get(), OpCode.RESPONSE_CHALLENGE); }
			case ERROR -> 	{ logger.info("Error with challengePublicKey"); }
		}
		return null;
	}
	
	
	/**
	 * Méthode pour la lecture d'un challenge OK.
	 * ---
	 * Cette méthode décode un challenge OK et retourne un objet `ChallengeOk`.
	 * 
	 * @param buffer Le buffer contenant les données à lire
	 * @return Un objet `ChallengeOk` contenant les informations du challenge
	 */
	private Paquet readChallengeOk(ByteBuffer buffer) {
		localStatus = publicKeyReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return new ChallengeOk(publicKeyReader.get()); }
			case ERROR -> 	{ logger.info("Error with challengePublicKey"); }
		}
		return null;
	}
	
	
	/**
	 * Méthode pour la lecture d'un joinResponse
	 * ---
	 * @param buffer
	 * @return Renvoie un JoinResponse
	 */
	private Paquet readBufferJoinResponse(ByteBuffer buffer) {
		localStatus = joinResponseReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return joinResponseReader.get(); }
			case ERROR -> 	{ logger.info("Error with JoinResponse"); }
		}
		return null;
	}
	
	/**
	 * Méthode pour la lecture d'un broadcast
	 * ---
	 * @param buffer
	 * @return
	 */
	private Paquet readBufferBroadcast(ByteBuffer buffer) {
		localStatus = broadcastReader.process(buffer);
		switch (localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return broadcastReader.get(); }
			case ERROR -> 	{ logger.info("Error with JoinResponse"); }
		}
		return null;
	}
	
	/**
	 * Méthode pour la lecture d'un message poublique
	 * ---
	 * @param buffer
	 * @return
	 */
	private Paquet readBufferMessagePublic(ByteBuffer buffer) {
		localStatus = messagePublicReader.process(buffer);
		switch (localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return messagePublicReader.get(); }
			case ERROR -> 	{ logger.info("Error with JoinResponse"); }
		}
		return null;
	}
	
	/**
	 * Méthode pour la lecture d'un message sécurisé encodé.
	 * ---
	 * Cette méthode décode un message sécurisé encodé et retourne un objet `EncodedRSABuffers`.
	 * 
	 * @param buffer Le buffer contenant les données à lire
	 * @return Un objet `EncodedRSABuffers` contenant les informations du message sécurisé
	 */
	private Paquet readBufferEncodedBySecureMessage(ByteBuffer buffer) {
		localStatus = rsaReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return new EncodedRSABuffers(rsaReader.get(), OpCode.SECURE_MESSAGE); }
			case ERROR -> 	{ logger.info("Error with challengePublicKey"); }
		}
		return null;
	}
	
	
	/**
	 * Méthode pour la lecture d'une réponse LeaveNetwork.
	 * ---
	 * Cette méthode décode une réponse LeaveNetwork et retourne un objet `LeaveNetworkResponse`.
	 * 
	 * @param buffer Le buffer contenant les données à lire
	 * @return Un objet `LeaveNetworkResponse` contenant les informations du paquet
	 */
	private Paquet readBufferLeaveNetworkResponse(ByteBuffer buffer) {
		localStatus = byteReader.process(buffer);
		switch (localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return new LeaveNetworkResponse(byteReader.get()); }
			case ERROR -> 	{ logger.info("Error with JoinResponse"); }
		}
		return null;
	}
	
	/**
	 * Méthode pour la lecture d'une confirmation LeaveNetwork.
	 * ---
	 * Cette méthode décode une confirmation LeaveNetwork et retourne un objet `LeaveNetworkConfirm`.
	 * 
	 * @param buffer Le buffer contenant les données à lire
	 * @return Un objet `LeaveNetworkConfirm` contenant les informations du paquet
	 */
	private Paquet readBufferLeaveNetworkConfirm(ByteBuffer buffer) {
		localStatus = listReader.process(buffer);
		switch (localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return new LeaveNetworkConfirm(new ArrayList<>(listReader.get())); }
			case ERROR -> 	{ logger.info("Error with JoinResponse"); }
		}
		return null;
	}
	
}
