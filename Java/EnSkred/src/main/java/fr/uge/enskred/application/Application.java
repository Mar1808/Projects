package fr.uge.enskred.application;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.paquet.Broadcast;
import fr.uge.enskred.paquet.ChallengeLongResponse;
import fr.uge.enskred.paquet.ChallengeOk;
import fr.uge.enskred.paquet.ChallengePublicKey;
import fr.uge.enskred.paquet.EncodedRSABuffers;
import fr.uge.enskred.paquet.Instruction;
import fr.uge.enskred.paquet.JoinResponse;
import fr.uge.enskred.paquet.LeaveNetworkConfirm;
import fr.uge.enskred.paquet.LeaveNetworkDone;
import fr.uge.enskred.paquet.LeaveNetworkResponse;
import fr.uge.enskred.paquet.ListConnected;
import fr.uge.enskred.paquet.Message;
import fr.uge.enskred.paquet.MessagePublic;
import fr.uge.enskred.paquet.MessageToSecure;
import fr.uge.enskred.paquet.NewConnection;
import fr.uge.enskred.paquet.NewNode;
import fr.uge.enskred.paquet.Node;
import fr.uge.enskred.paquet.Paquet;
import fr.uge.enskred.paquet.PassForward;
import fr.uge.enskred.paquet.Payload;
import fr.uge.enskred.paquet.PreJoin;
import fr.uge.enskred.paquet.RemoveNode;
import fr.uge.enskred.paquet.ResponseChallenge;
import fr.uge.enskred.paquet.SecondJoin;
import fr.uge.enskred.paquet.SecureMessage;
import fr.uge.enskred.readers.PrimaryInstructionReader;
import fr.uge.enskred.readers.PrimaryPayloadReader;
import fr.uge.enskred.readers.PrimaryReader;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.KeyPairRSA;
import fr.uge.enskred.readers.UGEncrypt.PrivateKeyRSA;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;


public final class Application {

	/**
	 * TODO TODO TODO
	 * Context à la fois Client/Server
	 */
	static class Context {
		private static final Logger logger = Logger.getLogger(Application.Context.class.getName());
		private boolean isFirstConnexion;	//diff de SecondConnexion (pas de PreJoin & JoinResponse)
		private final ContextMode mode;
		private ContextProgessStatus progressStatus;
		//Commnu
		private final SelectionKey key;
		private final SocketChannel socketChannel;
		private final ByteBuffer bufferIn;
		private final ByteBuffer bufferOut;
		private final ArrayDeque<ByteBuffer> queue;
		private final Application server;
		private boolean closed = false;
		//--- Partie reader à changer
		private final PrimaryReader primaryReader;
		private final PrimaryPayloadReader primaryPayloadReader;
		private final PrimaryInstructionReader primaryInstructionReader;
		//Propre au client interne (Client) ==>
		private final CommandQueue commandQueue;
		private final PublicKeyRSA publicKeyIntern;
		private final PrivateKeyRSA privateKeyIntern;
		//Propre au client externe (Server) ==>
		private final long longChallengeRSA;
		private PublicKeyRSA publicKeyExtern;
		private SocketAddress addressExtern;

		/**
		 * ContextMode possède 2 modes:
		 *  -> Server: il s'agit d'une demande de connexion vers nous, donc on agis comme un SERVER pour un client externe
		 *  -> Client: il s'agit de notre propre partie client pour initié des connexions, on à les mêmes droit qu'un CLIENT
		 * ProgressStatus va indique dans quel mode on se trouve:
		 *  -> Cette classe s'adresse
		 * ---
		 * @param mode
		 * @param server
		 * @param key
		 * @param commandQueue
		 * @param publicKey
		 * @param privateKey
		 */
		public Context(ContextMode mode, Application server, SelectionKey key, 
				CommandQueue commandQueue, PublicKeyRSA publicKey, PrivateKeyRSA privateKey,
				boolean isFirstConnexion) {
			this.mode = Objects.requireNonNull(mode);
			this.progressStatus = mode == ContextMode.EXTERN_CLIENT ? ContextProgessStatus.UNVERIFIED_PRE_JOIN : ContextProgessStatus.UNCONCERNED;
			this.server = server;
			this.key = key;
			this.socketChannel = (SocketChannel) key.channel();
			this.commandQueue = commandQueue;
			this.publicKeyIntern = publicKey;
			this.privateKeyIntern = privateKey;
			this.isFirstConnexion = isFirstConnexion;
			this.longChallengeRSA = Utils.generateRandomLong();
			this.bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
			this.bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
			this.queue = new ArrayDeque<>();
			this.primaryReader = new PrimaryReader(LEVEL);
			this.primaryPayloadReader = new PrimaryPayloadReader(LEVEL);
			this.primaryInstructionReader = new PrimaryInstructionReader(LEVEL);
			logger.setLevel(LEVEL);
		}

		//ProcessIn

		/**
		 * Add a message to the message queue, tries to fill bufferOut and updateInterestOps
		 *
		 * @param msg
		 */
		public void queuePaquet(Paquet msg) {
			var tmp = queue.add(msg.getWriteModeBuffer().flip());
			logger.info("queue added: " + tmp);
			processOut();
			updateInterestOps();
		}

		/**
		 * Try to fill bufferOut from the message queue
		 */
		private void processOut() {
			while(!queue.isEmpty() && bufferOut.hasRemaining()) {
				var bufferMessage = queue.getFirst();
				//cas ou l'on aurait beaucoup de place dans bufferOut
				if(bufferOut.remaining() >= bufferMessage.remaining()) {
					bufferOut.put(bufferMessage);
					queue.pollFirst();
				} else {
					var oldLimit = bufferMessage.limit();
					bufferMessage.limit(bufferMessage.position() + bufferOut.remaining());
					bufferOut.put(bufferMessage);
					bufferMessage.limit(oldLimit);
				}
			}
		}

		/**
		 * Update the interestOps of the key looking only at values of the boolean
		 * closed and of both ByteBuffers.
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * updateInterestOps and after the call. Also it is assumed that process has
		 * been be called just before updateInterestOps.
		 */
		private void updateInterestOps() {
			var newInterest = 0;
			if(!closed && bufferIn.hasRemaining()) {
				newInterest |= SelectionKey.OP_READ;
			}
			if(bufferOut.position() > 0) {
				newInterest |= SelectionKey.OP_WRITE;
			}
			if(newInterest == 0 || !key.isValid()) {
				logger.info("Error, interestOps is 0 !");
				silentlyClose();
				server.deletionAfterBrutaleDeconnexion(this);
				return;
			}
			key.interestOps(newInterest);
		}

		private void silentlyClose() {
			try {
				key.cancel();
				socketChannel.close();
			} catch (IOException e) {
				// ignore exception
			}
		}


		/**
		 * Performs the read action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * doRead and after the call
		 *
		 * @throws IOException
		 */
		private void doRead() throws IOException {
			if(socketChannel.read(bufferIn) == -1) {
				/*logger.info*/System.out.println((mode == ContextMode.EXTERN_CLIENT ? "Client" : "Server") + " closing connexion !\nIS: " + publicKeyExtern);
				closed = true;
			}
			processIn();
			updateInterestOps();
		}

		/**
		 * Performs the write action on sc
		 *
		 * The convention is that both buffers are in write-mode before the call to
		 * doWrite and after the call
		 *
		 * @throws IOException
		 */

		private void doWrite() throws IOException {
			socketChannel.write(bufferOut.flip());
			bufferOut.compact();
			processOut();
			updateInterestOps();
		}


		//MÉTHODE PROPRE AU CLIENT INTERNE (CLIENT) =>
		public void connectFirstJoin(PublicKeyRSA publicKey, InetSocketAddress socketAddress) {
			if(mode == ContextMode.EXTERN_CLIENT) {logger.severe("Fatal Context Error for PreJoin !!!");return;}
			Objects.requireNonNull(publicKey);
			Objects.requireNonNull(socketAddress);
			var node = new Node(publicKey, socketAddress);
			queuePaquet(isFirstConnexion ? new PreJoin(node) : new SecondJoin(node));
			logger.info("\n\nOK, GO !\n\n");
		}

		public void doConnect() throws IOException {
			if(mode == ContextMode.EXTERN_CLIENT) {logger.severe("Fatal Context Error for DoConnect !!!");return;}
			try {
				if(!socketChannel.finishConnect()) {
					logger.info("Connexion terminé avec l'application distante : " + socketChannel.getRemoteAddress());
					return;
				}
				key.interestOps(SelectionKey./*OP_READ*/OP_WRITE);
			} catch (IOException e) {
				logger.severe("Impossible de se connecter à l'application distante : " + e.getMessage());
			}
		}

		//MÉTHODE PROPRE AU CLIENT EXTERNE (SERVER) =>
		private void manageConnexion(Node node) {
			Objects.requireNonNull(node);
			var paquet = new ChallengePublicKey(node.publicKey(), longChallengeRSA);
			queuePaquet(paquet);
			publicKeyExtern = node.publicKey();
			addressExtern = node.socketAddress();
			logger.info("mC-" + (isFirstConnexion ? "fC: " : "sC: ") + node.publicKey() + "\n" + node.socketAddress());
			progressStatus = ContextProgessStatus.UNVERIFIED_CHALLENGE;
		}

		private void manageVerifyChallenge(long decodedLongChallenge) throws IOException {
			var status = decodedLongChallenge == longChallengeRSA;
			if(!server.manageVerifyChallenge(this, status)) {
				closed = true;
				logger.info("On deconnecte " + this.socketChannel);
				return;
			}
			logger.info("Good !");
			progressStatus = isFirstConnexion ? ContextProgessStatus.VERIFIED_1 : ContextProgessStatus.VERIFIED_2;
		}

		private Payload analysePayload(ByteBuffer buffer) {
			Objects.requireNonNull(buffer);
			switch(primaryPayloadReader.process(buffer)) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{
				var payload = primaryPayloadReader.get();
				primaryPayloadReader.reset();
				return payload;
			}
			case ERROR -> 	{ logger.info("Error with analysePayload 1"); }
			}
			return null;
		}

		private Instruction analyseInstruction(ByteBuffer buffer) {
			Objects.requireNonNull(buffer);
			primaryInstructionReader.reset();
			switch(primaryInstructionReader.process(buffer)) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{
				var instruction = primaryInstructionReader.get();
				return instruction;
			}
			case ERROR -> 	{ logger.info("Error with analysePayload 1"); }
			}
			return null;
		}
		
		/**
		 * Process the content of bufferIn
		 *
		 * The convention is that bufferIn is in write-mode before the call to process and
		 * after the call
		 * @throws IOException 
		 *
		 */
		private void processIn() throws IOException {
		    switch(primaryReader.process(bufferIn)) {
		        case REFILL -> {} // rien à faire
		        case ERROR 	-> logger.info("Error with ProcessIn 1°");
		        case DONE	-> handlePaquet(primaryReader.get());
		    }
		}
		
		private void handlePaquet(Paquet paquet) throws IOException {
		    logger.info(paquet.getOpCode() + " IS LA ");
		    switch(paquet.getOpCode()) {
				/*****************************************
				 ******** GESTION DE LA CONNEXION ********
				 *****************************************/
		        case PRE_JOIN, SECOND_JOIN 	-> handleJoin(paquet);
		        case CHALLENGE_PUBLIC_KEY 	-> handleChallengePublicKey((EncodedRSABuffers) paquet);
		        case RESPONSE_CHALLENGE 	-> handleResponseChallenge((ChallengeLongResponse) paquet);
		        case CHALLENGE_OK 			-> handleChallengeOk((ChallengeOk) paquet);
		        case JOIN_RESPONSE 			-> handleJoinResponse((JoinResponse) paquet);
				/*****************************************
				 ********* GESTION DES BROADCAST *********
				 *****************************************/
		        case BROADCAST				-> handleBroadcast((Broadcast) paquet);
				/*****************************************
				 ********* GESTION DES BROADCAST *********
				 *****************************************/
		        case OPEN_MESSAGE 			-> handlePublicMessage((MessagePublic) paquet);
		        case SECURE_MESSAGE 		-> handleSecureMessage((EncodedRSABuffers) paquet);
				/*****************************************
				 ******* GESTION DE LA DÉCONNEXION *******
				 *****************************************/
		        case LEAVE_NETWORK_ASK 		-> server.treatAsk(this);
		        case LEAVE_NETWORK_RESPONSE -> server.treatResponse(this, ((LeaveNetworkResponse) paquet).response());
		        case LEAVE_NETWORK_CANCEL 	-> server.treatCancel();
		        case LEAVE_NETWORK_CONFIRM 	-> server.treatConfirm(this, (LeaveNetworkConfirm) paquet);
		        case LEAVE_NETWORK_DONE 	-> server.treatDone(this);
		        
		        default 					-> logger.info("ERROR DE PROCESSIN CONTEXT 2°");
		    }
		    primaryReader.reset();
		}
		
		private void handleJoin(Paquet paquet) {
		    if(progressStatus != ContextProgessStatus.UNVERIFIED_PRE_JOIN) {
		        logger.info("Error, impossible to access prejoin");
		        return;
		    }
		    logger.info(paquet.getOpCode() == OpCode.PRE_JOIN ? "PREJOIN LA" : "SECONDJOIN LA");
		    switch(paquet) {
			    case PreJoin prejoin 		-> { manageConnexion(prejoin.node()); }
			    case SecondJoin secondJoin 	-> { isFirstConnexion = false; manageConnexion(secondJoin.node()); }
				default -> throw new IllegalArgumentException("Unexpected value: " + paquet);
		    }
		}
		
		private void handleChallengePublicKey(EncodedRSABuffers paquet) {
		    queuePaquet(new ResponseChallenge(privateKeyIntern, paquet.buffer()));
		    logger.info("GOOD CHALLENGE !");
		}
		
		private void handleResponseChallenge(ChallengeLongResponse paquet) throws IOException {
		    if(progressStatus != ContextProgessStatus.UNVERIFIED_CHALLENGE) {
		        logger.info("Error, impossible to access response challenge");
		        return;
		    }
		    logger.info("Response challenge => Logique coté server");
		    manageVerifyChallenge(paquet.challengeResponse());
		}

		private void handleChallengeOk(ChallengeOk paquet) throws IOException {
		    logger.info("Challenge OK => sur 2nd connexions");
		    publicKeyExtern = paquet.publicKeyReceiver();
		    server.registerSecondConnexion(this);
		}

		private void handleJoinResponse(JoinResponse paquet) throws IOException {
		    logger.info("JoinResponse côté client dans le bien !");
		    publicKeyExtern = paquet.publicKeyReceiver();
		    server.updateWithJoinResponse(this, paquet);
		    commandQueue.offerMessage(new Message("Système", "Challenge O.K."));
		}

		private void handleBroadcast(Broadcast paquet) throws IOException {
			if(!isConnexionVerified()) { return; }
		    logger.info("On va prendre le broadcast en main II_ProcessIn_II");
		    server.updateBroadCast(this, paquet);
		}

		private void handlePublicMessage(MessagePublic paquet) {
			if(!isConnexionVerified()) { return; }
		    logger.info("Message Publique de processIn !");
		    server.sendMessage(publicKeyIntern, paquet);
		}

		private void handleSecureMessage(EncodedRSABuffers paquet) {
			if(!isConnexionVerified()) { return; }
		    logger.info("Message privée de processIn !");
		    var encodedBuffer = paquet.buffer();
		    if(encodedBuffer == null) {
		        logger.warning("Buffer encodé null, paquet invalide.");
		        return;
		    }
		    var decoded = Utils.safeDecryptRSA(encodedBuffer.flip(), privateKeyIntern);
		    var instruction = analyseInstruction(decoded);
		    server.sendHiddenMessage(instruction);
		}
		
		private boolean isConnexionVerified() {
		    if(progressStatus == ContextProgessStatus.UNVERIFIED_CHALLENGE
		            || progressStatus == ContextProgessStatus.UNVERIFIED_PRE_JOIN) {
		        logger.info("Error, you can't access to this protocole");
		        return false;
		    }
		    return true;
		}

		public void mustBeDisconnected() {
			closed = true;
		}

		//TODO TODO TODO TODO LA FIN
		public PublicKeyRSA publicKeyExtern() {
			return publicKeyExtern;
		}
		public SocketAddress socketAddressExtern() {
			return addressExtern;
		}
		public boolean isFirstConnexion() {
			return isFirstConnexion;
		}
	}

	//TODO En commun
	private static final int BUFFER_SIZE = 2 << 9;
	private static final Logger logger = Logger.getLogger(Application.class.getName());
	private static final Level LEVEL = Level.SEVERE;	//CHAMPS À MODIFIER SI NECESSAIRE !

	private final ServerSocketChannel serverSocketChannel;
	private final InetSocketAddress serverAddress;
	private final Selector selector;
	private final CommandQueue commandQueue;
	private final Thread console;
	//private InetSocketAddress remoteServerAddress;
	private final PublicKeyRSA uniquePublicKeyRSA;
	private final PrivateKeyRSA uniquePrivateKeyRSA;
	private Integer optionalPort;
	//Les connexions initier par des externes
	private final ConnexionManager connexionManager;
	private final DeconnexionManager deconnexionManager;
	//informations générales sur les applications
	private final InfoUsers infoUsers;
	//Graphe
	private final Graphe graphe;
	
	public Application(InetSocketAddress serverAddress, KeyPairRSA keyPairRSA, Integer optionalPort) throws IOException {
		selector = Selector.open();

		serverSocketChannel = ServerSocketChannel.open();
		this.serverAddress = Objects.requireNonNull(serverAddress);
		logger.setLevel(LEVEL);
		this.console = Thread.ofPlatform().unstarted(this::consoleRun);
		this.commandQueue = new CommandQueue();
		Objects.requireNonNull(keyPairRSA);
		this.uniquePublicKeyRSA = keyPairRSA.publicKey();
		this.uniquePrivateKeyRSA = keyPairRSA.privateKey();
		this.infoUsers = new InfoUsers(LEVEL, uniquePublicKeyRSA);
		this.graphe = new Graphe();
		this.connexionManager = new ConnexionManager(LEVEL);
		this.deconnexionManager = new DeconnexionManager(this, LEVEL);
		this.optionalPort = optionalPort;
	}

	//TODO debut client
	private void consoleRun() {
		try {
			try (var scanner = new Scanner(System.in)) {
				while (scanner.hasNextLine()) {
					var msg = scanner.nextLine();
					sendCommand(msg);
				}
			}
			logger.info("Console thread stopping");
		} catch (InterruptedException e) {
			logger.info("Console thread has been interrupted");
		}
	}

	/**
	 * Send instructions to the selector via a BlockingQueue and wake it up
	 *
	 * @param command: commande reçu en paramètre
	 * @throws InterruptedException: en cas d'erreur avec la commandQueue
	 */
	private void sendCommand(String command) throws InterruptedException {
		var paquet = ParseCommand.getPaquet(uniquePublicKeyRSA, command, Map.copyOf(infoUsers.getIndexedNodes()), graphe, infoUsers);
		if(null == paquet) {
			Utils.printHelp();
			return;
		}
		commandQueue.offerCommand(paquet);
		selector.wakeup();
	}

	/**
	 * Processes the command from the BlockingQueue 
	 */
	private void processCommands() {
		while(!commandQueue.isEmptyCommandQueue()) {
			var msg = commandQueue.pollCommand();
			if(null == msg) {
				break;
			}
			switch(msg) {
			case ListConnected listConnected -> {
				listConnected.updateInfos(infoUsers.getIndexedNodes()); 
				//logger.info(listConnected.toString()); 
				System.out.println(listConnected);
			}
			case MessagePublic messagePublic -> {
				logger.info("On vient de crée un openMessage: " + messagePublic);
				logger.info("Sender: " +messagePublic.sender()+"\nReceiver: "+ messagePublic.receiver());
				sendMessage(uniquePublicKeyRSA, messagePublic);
			}
			case SecureMessage secureMessage -> {
				logger.info("On vient de créer un secureMessage: " + secureMessage);
				sendHiddenMessage(secureMessage.instruction());
			}
			case Message message -> {
				logger.info("On traite un message en interne");
				switch(message.message()) {
				case "r" 	-> { System.out.println(infoUsers.toStringRootConnexions()); }
				case "d"	-> { logger.info("Avant:");
				logger.info("On va se déconnecter");deconnexionManager.initDeconnexion(uniquePublicKeyRSA, infoUsers.getViewAppToContext());}
				default 	-> { logger.info("Rien à faire ici !"); }
				}
			}
			default -> {logger.warning("Cas imprevu sur processCommand !");}
			}

		}
		while(!commandQueue.isEmptyMessageQueue()) {
			var messageReceived = commandQueue.pollMessage();
			if(null != messageReceived) {
				System.out.println(messageReceived + "\n\n");
				//logger.info("" + messageReceived);
			}
		}
	}
	//TODO fin client

	private void startInternClient(SocketChannel socketChannel, Integer optionalPort) throws IOException {
		if(null == optionalPort) { return; }
		if(!infoUsers.putPortAndSocketChannelIfAbsent(socketChannel, optionalPort)) { return; }
		var remoteServerAddress = new InetSocketAddress(optionalPort);
		logger.info("RemoteX: " + remoteServerAddress);
		//Client config
		socketChannel.configureBlocking(false);
		var clientKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
		var context = new Context(ContextMode.INTERN_CLIENT, this, clientKey, commandQueue, uniquePublicKeyRSA, uniquePrivateKeyRSA, infoUsers.getSizeAppToContext() < 1);
		clientKey.attach(context);
		connexionManager.register(context, optionalPort, uniquePublicKeyRSA, remoteServerAddress);
		socketChannel.connect(remoteServerAddress);
		logger.info("O.K. pour le contextIntern");
	}

	private SocketChannel createAndReturnSocketChannel() throws IOException {
		return SocketChannel.open();
	}

	public void launch() throws IOException {
		//Server config
		serverSocketChannel.bind(serverAddress);
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

		startInternClient(createAndReturnSocketChannel(), optionalPort);

		infoUsers.prepare(uniquePublicKeyRSA, serverAddress);
		//UI
		console.start();
		Utils.printHelp();
		while (!Thread.interrupted()) {
			//Helpers.printKeys(selector); // for debug
			logger.info("Starting select");
			try {
				selector.select(this::treatKey);
				processCommands();
			} catch (UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			logger.info("Select finished");
		}
	}

	private void treatKey(SelectionKey key) {
		//Helpers.printSelectedKey(key); // for debug
		try {
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
		var context = (Context) key.attachment();
		if(context == null) {
			logger.info("context doesn't have attachemet !");
			return;
		}
		try {
			if (key.isValid() && key.isConnectable()) {
				context.doConnect();
				context.connectFirstJoin(uniquePublicKeyRSA, serverAddress);
			}
			if (key.isValid() && key.isWritable()) {
				context.doWrite();
			}
			if (key.isValid() && key.isReadable()) {
				context.doRead();
			}
		} catch (IOException ioe) {
			// lambda call in select requires to tunnel IOException
			throw new UncheckedIOException(ioe);
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		//NN = nonNullable et N = Nullable
		var client = serverSocketChannel.accept();
		if(null == client) {
			logger.severe("SocketChannel client is null !");
			return;
		}
		client.configureBlocking(false);
		var clientKey = client.register(selector, SelectionKey.OP_READ);
		var context = new Context(ContextMode.EXTERN_CLIENT, this, clientKey, null, uniquePublicKeyRSA, uniquePrivateKeyRSA, true);
		clientKey.attach(context);
		//(state(NN), application(NN), SelectKey(NN), CommQueue(N), PubKey(N), Privkey(N))
	}

//	private void silentlyClose(SelectionKey key) {
//		Channel sc = (Channel) key.channel();
//		try {
//			sc.close();
//		} catch (IOException e) {
//			// ignore exception
//		}
//	}


	/**
	 * Add a message to all connected clients queue
	 *
	 * @param broadcast: broadcast à transmettre à tous le monde sauf à celui qui nous l'a envoyé.
	 * @param sContext: context de la personne qui nous l'a envoyé
	 */
	public void broadcast(Broadcast broadcast, Context sContext) {
		Objects.requireNonNull(broadcast);
		for(var key: selector.keys()) {
			var context = (Context) key.attachment();
			if(context == null || (sContext != null && sContext.equals(context))) {
				continue;
			}
			logger.info("On passe " + context.publicKeyExtern());
			context.queuePaquet(broadcast);
		}
	}


	private void startSecondConnexion() throws IOException {
		if(infoUsers.getSizeAppToContext() >= 2) {
			return;
		}
		logger.info("On tente une 2e connexion");
		var node = infoUsers.getNewConnexion(uniquePublicKeyRSA);
		if(null == node) {
			logger.info("2e connexion impossible pour le moment !");
			return;
		}
		logger.info("\n2e connexion en cours ...\n");
		startInternClient(createAndReturnSocketChannel(), node.socketAddress().getPort());
		logger.info("\n2e connexion O.K.");
	}


	private boolean manageVerifyChallenge(Context context, boolean status) throws IOException {
		if(!status) {
			logger.info("Error, long is not equals in verifyChallenge");
			disconnectApp(context);
			return false;
		}
		//mise à jour des infos si challenge O.K
		infoUsers.makeFirstConnexion(context.publicKeyExtern(), context.socketAddressExtern());
		infoUsers.JoinRoot(uniquePublicKeyRSA, context.publicKeyExtern());
		infoUsers.putOnAppToContext(context.publicKeyExtern(), context);
		infoUsers.putOnContextToApp(context, context.publicKeyExtern());
		logger.info("MAJ ATC CTA, PK =>" + context.publicKeyExtern() + " _ " + uniquePublicKeyRSA);
		//On remet à jour notre graphe interne
		graphe.updateNetWork(infoUsers.getRoutageConnexion());
		//préparation du paquet
		var paquet = context.isFirstConnexion()
						? infoUsers.getJoinResponse(uniquePublicKeyRSA)
						: new ChallengeOk(uniquePublicKeyRSA);
		context.queuePaquet(paquet);
		//transmission du broadcast ici:
		if(context.isFirstConnexion()) { broadcastFirstConnexion(context);/*NewNode*/ }
		return true;
	}

	private void registerSecondConnexion(Context context) {
		var publicKeyUser = context.publicKeyExtern();
		if(publicKeyUser == null) {
			logger.info("No registration 2ndCo possible since " + context.socketChannel + "\n");
			return;
		}
		infoUsers.putOnAppToContext(publicKeyUser, context);
		infoUsers.putOnContextToApp(context, publicKeyUser);
		infoUsers.updatePKReceiver(publicKeyUser, uniquePublicKeyRSA);
		//Mise à jour du réseau de graphe !
		graphe.updateNetWork(infoUsers.getRoutageConnexion());
		broadcastSecondConnexion(context);/*NewConection*/ 
	}

	private void broadcastFirstConnexion(Context context) {
		var generateMessageID = Utils.generateRandomLong();
		logger.info(uniquePublicKeyRSA + " genere pour la 1re co: " + generateMessageID);
		infoUsers.getAndUpdateMessageIDBroadcast(uniquePublicKeyRSA, generateMessageID);
		var publicKeyUser = context.publicKeyExtern();
		var socketAddressUser = context.socketAddressExtern();
		if(publicKeyUser == null || socketAddressUser == null) {
			logger.info("No broadcast possible since " + context.socketChannel + "\n" +publicKeyUser+"  _&&_ "+socketAddressUser);
			return;
		}
		var newNode = new NewNode(publicKeyUser, socketAddressUser, uniquePublicKeyRSA);
		var payload = newNode.getWriteModeBuffer().flip();
		var size = payload.remaining();
		broadcast(new Broadcast(uniquePublicKeyRSA, generateMessageID, size, payload.compact()), context);
		logger.info("Broadcast info for: " + context.publicKeyExtern());
	}

	private void broadcastSecondConnexion(Context context) {
		var generateMessageID = Utils.generateRandomLong();
		var publicKeyUser = context.publicKeyExtern();
		if(publicKeyUser == null) {
			logger.info("No broadcast possible since " + context.socketChannel + "\n");
			return;
		}
		logger.info(uniquePublicKeyRSA + " genere pour la 2re co: " + generateMessageID);
		infoUsers.getAndUpdateMessageIDBroadcast(uniquePublicKeyRSA, generateMessageID);
		var newConnexion = new NewConnection(publicKeyUser, uniquePublicKeyRSA);
		var payload = newConnexion.getWriteModeBuffer().flip();
		var size = payload.remaining();
		broadcast(new Broadcast(uniquePublicKeyRSA, generateMessageID, size, payload.compact()), null);
		logger.info("Broadcast info for: " + context.publicKeyExtern());
	}

	private void disconnectApp(Context context) throws IOException {
		Objects.requireNonNull(context);
		var publicKey = infoUsers.disconnectAppWithContextToApp(context);
		if(publicKey == null) { return; } 
		var port = connexionManager.unregisterByKey(publicKey);
		infoUsers.removeSocketChannelByPort(port);
		infoUsers.removePublicKeyWithContext(context);
		infoUsers.removeContextWithPublicKey(publicKey);
		context.silentlyClose();
		logger.info("User " + publicKey + "Disconnected !");
	}


	/**
	 * Mise à jour des infos à la suite d'un joinResponse sur le réseau
	 * + info et maj du graphe
	 * @param context: context de la personne qui envoie le joinResponse
	 * @param paquet: Paquet de type JoinResponse envoyé par context
	 * @throws IOException 
	 */
	private void updateWithJoinResponse(Context context, JoinResponse paquet) throws IOException {
		Utils.requireNonNulls(context, paquet);
		var pubKeyReceiver = paquet.publicKeyReceiver();
		var nodes = paquet.nodes();
		var connexions = paquet.connexions();
		infoUsers.putOnContextToApp(context, context.publicKeyExtern());
		infoUsers.putOnAppToContext(context.publicKeyExtern(), context);
		//MAJ les listes
		for(var node: nodes) {
			infoUsers.updateAppAndAddress(node);
		}
		for(var connexion: connexions) {
			infoUsers.updateRoutageConnexion(connexion);
		}
		//MAJ la pkReceiver
		infoUsers.updatePKReceiver(pubKeyReceiver, uniquePublicKeyRSA);
		//Mise à jour du réseau de graphe !
		graphe.updateNetWork(infoUsers.getRoutageConnexion());
		startSecondConnexion();
	}

	/**
	 * Mise à jour, puis redifusion à la suite d'un broadcast.
	 * 
	 * -- Mise à jour du graphe
	 * @param context: context de la personne qui envoie un broadcast
	 * @param broadcast: paquet de broadcast reçu
	 * @throws IOException 
	 */
	private void updateBroadCast(Context context, Broadcast broadcast) throws IOException {
		Utils.requireNonNulls(context, broadcast);
		//on ajoute la payload à nos infos personnelle
		var responseMessageID = infoUsers.getAndUpdateMessageIDBroadcast(broadcast.publicKeySender(), broadcast.messageID());
		logger.info(uniquePublicKeyRSA + " recoit de " + broadcast.publicKeySender() + " msg "+ (responseMessageID ? "déjà" : "non") + " reçu" + broadcast.messageID());
		if(responseMessageID) { logger.info("pas d'update pr broascast"); return; }
		//On appel analysePayload
		var publicKeyWantDisconnect = analysePayload(context.analysePayload(broadcast.payload()));
		if(publicKeyWantDisconnect != null) {
			var contextToRemove = infoUsers.removeContextWithPublicKey(publicKeyWantDisconnect);
			if(null != contextToRemove) {
				infoUsers.removePublicKeyWithContext(contextToRemove); 
				contextToRemove.mustBeDisconnected();
			}
			//startSecondConnexion();//pas nécesaire
		}
		broadcast(broadcast, context);
		//Mise à jour du réseau de graphe !
		graphe.updateNetWork(infoUsers.getRoutageConnexion());
		logger.info("on doit renvoyé && maj nos info sur le broadcast");
	}

	/**
	 * Méthode pour analyser les payload
	 * 
	 * --> Sert pour l'instant uniquement au payload NN
	 * 
	 * Logique à changer à l'avenir
	 * @param payload: payload reçu à traiter après un broadcast.
	 * @throws IOException 
	 */
	private PublicKeyRSA analysePayload(Payload payload) throws IOException {
		if(null == payload) {
			logger.info("No analyse/update possible for payload");
			return null;
		}
		switch(payload) {
		case NewNode newNode -> {
			infoUsers.updateNewNode(newNode);
			logger.info("newNode O.K.");
		}
		case NewConnection newConnection -> {
			infoUsers.updateNewConnection(newConnection);
			logger.info("NewConnection O.K.\n\n");
		}
		case RemoveNode removeNode -> {
			infoUsers.updateRemoveNode(removeNode);
			logger.info("RemoveNode O.K.\n\n");
			return removeNode.publicKeyLeaver();
		}
		}
		return null;
	}

	public void sendMessage(PublicKeyRSA key, MessagePublic paquet) {
		Utils.requireNonNulls(key, paquet);
		logger.info("=1&2=| notre key: " + key);
		if(key.equals(paquet.receiver())) {
			logger.info("==3==");
			receiveMessage(paquet);
			return;
		}
		logger.info("==4==");
		if(!infoUsers.publicKeyIsOnNetwork(key)) {
			logger.info("==4.1==");
			System.err.println("Error, key isn't on network (disconnected app or else) !");
			return;
		}
		logger.info("==5==");
		var appIntermediaire = graphe.nextHop(key, paquet.receiver());
		logger.info("==6==> Inter = " + appIntermediaire);
		if(appIntermediaire == null) {
			logger.info("==6.1==");
			System.err.println("Aucun chemin trouvé de " + key + " à " + paquet.receiver());
		}
		logger.info("==7==");
		infoUsers.sendMessageWithAppToContext(appIntermediaire, paquet);
	}
	
	public void sendHiddenMessage(Instruction paquet) {
		Objects.requireNonNull(paquet);
		switch(paquet) {
			case MessageToSecure message -> { receiveMessage(message); }
			case PassForward passForward -> { infoUsers.sendMessageWithAppToContext(passForward.receiver(), passForward.secureMessage()); }
		}
	}


	/**
	 * Methode pour afficher un message.
	 * On sait qu'il s'adresse à nous, car ce sera vérifier avant.
	 * @param messageReceived: Message reçu.
	 */
	private void receiveMessage(Paquet messageReceived) {
		Objects.requireNonNull(messageReceived);
		var sb = new StringBuilder();
		switch(messageReceived) {
			case MessagePublic messagePublic -> {
				var annonce = " Nouveau message public reçu ";
				sb.append("\n")
				  .append("*".repeat(5)).append(annonce).append("*".repeat(5)).append("\n")
				  .append("Expéditeur: ")
				  .append(messagePublic.sender().equals(uniquePublicKeyRSA) ? "Nous même" : messagePublic.sender()).append("\n")
				  .append("Message: ").append(messagePublic.message()).append("\n")
				  .append("*".repeat(5 + annonce.length())).append("*".repeat(5)).append("\n");
				System.out.println(sb);
			}
			case MessageToSecure message -> {
				var isSelf = message.sender().equals(uniquePublicKeyRSA);
				var isAck = infoUsers.verifyAcknowlegdmentHiddenMessegeID(message.idMessage());
				var annonce = (isAck && !isSelf ? " Accusé de reception suite au" : " Nouveau") + " message caché " + (isAck ? "" : "reçu ");
				sb.append("\n")
				  .append("*".repeat(5)).append(annonce).append("*".repeat(5)).append("\n")
				  .append("Expéditeur: ")
				  .append(isSelf ? "Nous même" : message.sender()).append("\n");
				if(isAck && !isSelf) {
					sb.append("ID de l'accusé de reception ").append(message.idMessage()).append("\n");
				}else {
					sb.append("Message: ").append(message.message()).append("\n");
				}
				sb.append((isAck && !isSelf ? "Retransmis en " : "Reçu en "))
				  .append(System.currentTimeMillis() - message.idMessage())
				  .append(" ms\n");
				if(isSelf) {
					sb.append("Pas d'acknowlegement à nous même !\n");
				}
				sb.append("*".repeat(5 + annonce.length())).append("*".repeat(5)).append("\n");
				System.out.println(sb);
				
				if(isAck) { return; }
				var listToSender = graphe.randomPath(uniquePublicKeyRSA, message.sender()).reversed();
				var ackToSender = Utils.acknowledgmentAfterSecureMessage(uniquePublicKeyRSA, listToSender, message.idMessage());
				sendHiddenMessage(ackToSender.instruction());
			}
			default -> throw new IllegalArgumentException("Unexpected value: " + messageReceived);	
		}
	}

	private void treatAsk(Context context) {
		Objects.requireNonNull(context);
		deconnexionManager.treatAsk(context, infoUsers.getViewAppToContext());
	}

	private void treatResponse(Context context, byte response) {
		Objects.requireNonNull(context);
		logger.info("\n\n\ntreatResponse\n");
		logger.info("\n\n\n");
		var neightbors = infoUsers.getNeighborsNodes();
		deconnexionManager.receiveDeconnexion(context.publicKeyExtern(), response == 1, neightbors);
	}

	private void treatCancel() {
		deconnexionManager.treatCancel();
	}

	private void treatConfirm(Context context, LeaveNetworkConfirm paquet) throws IOException {
		Objects.requireNonNull(context);
		var neighbors = deconnexionManager.treatConfirm();
		for(var node: paquet.nodes()) {
			var publicKey = node.publicKey();
			var socketAddress = node.socketAddress();
			if(neighbors.contains(publicKey)) { logger.info("Déjà connecté");continue; }
			logger.info("J'me co à " + socketAddress);
			startInternClient(createAndReturnSocketChannel(), socketAddress.getPort());
		}
		context.queuePaquet(new LeaveNetworkDone());
		deconnexionManager.resetDeconnexionManager();
	}

	private void treatDone(Context context) {
		Objects.requireNonNull(context);
		if(deconnexionManager.receiveDone(context.publicKeyExtern())) {
			//CAN'T RECEIVE
			infoUsers.resetAtDeconnexion(uniquePublicKeyRSA);
			graphe.updateNetWork(infoUsers.getRoutageConnexion());
			System.out.println("L'application n'est plus dans le réseau");
		}
	}
	
	private void deletionAfterBrutaleDeconnexion(Context context) {
		infoUsers.disconnectAppWithContextToApp(context);
		graphe.updateNetWork(infoUsers.getRoutageConnexion());
		//DECONNEXION PAR REMOVE NODE
		var pubkeyWantDeconnect = context.publicKeyExtern();
		if(pubkeyWantDeconnect == null) { return; }//sécurité en cas de défaillance
		var removeNode = new RemoveNode(pubkeyWantDeconnect);
		var payload = removeNode.getWriteModeBuffer().flip();
		var size = payload.remaining();
	    var broadcast = new Broadcast(pubkeyWantDeconnect, Utils.generateRandomLong(), size, payload.compact());
	    broadcast(broadcast, context);
	}
	
	public static void main(String[] args) {
		try {
			if (args.length < 1) {
				usage();
				return;
			}
			var requiredPort = Integer.parseInt(args[0]);
			var optionalPort = args.length > 1 ? Integer.parseInt(args[1]) : null;
			new Application(new InetSocketAddress(requiredPort), UGEncrypt.KeyPairRSA.generate(), optionalPort).launch();
		} catch(NumberFormatException nfe) {
			usage();
			return;
		} catch(NoSuchAlgorithmException nsae) {
			logger.severe("Fatal error with operation for [Public|Private]KeyRSA.");
			return;
		} catch(IllegalArgumentException iae) {
			logger.severe("Fatal error, port out of range for port1 or port2");
			return;
		} catch(IOException ioe) {
			logger.severe("Fatal I/O ERROR\n[Port already used / Error with receiving or sending / ...]\nSysteme brutal closing !");
			return;
		}
	}


	private static void usage() {
		logger.info("Usage : Application portServerSource[required] portServerDest[Optional]");
	}
}
