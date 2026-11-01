package fr.uge.enskred.application;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import fr.uge.enskred.application.Application.Context;
import fr.uge.enskred.paquet.Connexion;
import fr.uge.enskred.paquet.JoinResponse;
import fr.uge.enskred.paquet.NewConnection;
import fr.uge.enskred.paquet.NewNode;
import fr.uge.enskred.paquet.Node;
import fr.uge.enskred.paquet.Paquet;
import fr.uge.enskred.paquet.RemoveNode;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/*
 * Classe qui représente les info de tout le réseau pour une application donnée
 */
public final class InfoUsers {
	static private final Logger logger = Logger.getLogger(InfoUsers.class.getName());
	private final HashSet<PublicKeyRSA> publicKeys;
	private final Map<Integer, Node> cachedIndexedPublicKeys;
	private int cachedPublicKeyCount;
	
	private final PublicKeyRSA myPublicKeyRSA;
	private final Set<Long> myHiddenMessageID;
	private final HashMap<PublicKeyRSA, Set<Long>> lastMessageIDBroadcast;
	private final HashMap<PublicKeyRSA, SocketAddress> appToAddress;
	private final HashMap<PublicKeyRSA, HashSet<PublicKeyRSA>> routageConnexion;
	private final HashMap<SocketAddress, PublicKeyRSA> addressToApp;
	private final Map<Integer, SocketChannel> socketChannels;//<Port, Socket>
	private final HashMap<PublicKeyRSA, Context> appToContext;
	private final HashMap<Context, PublicKeyRSA> contextToApp;
	private final Lock lock;
	//structure de données pour les connexions direct !

	
	/**
	 * Constructeur de la classe InfoUsers.
	 * Initialise toutes les structures nécessaires au suivi du réseau.
	 * ---
	 */
	public InfoUsers(Level level, PublicKeyRSA publicKeyRSA) {
		myPublicKeyRSA = Objects.requireNonNull(publicKeyRSA);
		publicKeys = new HashSet<>();
		cachedIndexedPublicKeys = new HashMap<>();
		lastMessageIDBroadcast = new HashMap<>();
		appToAddress = new HashMap<>();
		myHiddenMessageID = new HashSet<>();
		routageConnexion = new HashMap<>();
		addressToApp = new HashMap<>();
		socketChannels = new HashMap<>();
		appToContext = new HashMap<>();
		contextToApp = new HashMap<>();
		lock = new ReentrantLock();
		logger.setLevel(level == null ? Level.SEVERE : level);

	}

	/**
	 * Prépare l'ajout d'un utilisateur avec sa clé publique et son adresse.
	 * ---
	 * @param publicKeyIntern : Clé publique de l'utilisateur local
	 * @param socketAddressIntern : Adresse socket de l'utilisateur local
	 */
	public void prepare(PublicKeyRSA publicKeyIntern, SocketAddress socketAddressIntern) {
		Utils.requireNonNulls(publicKeyIntern, socketAddressIntern);
		lock.lock();
		try {
			publicKeys.add(publicKeyIntern);
			appToAddress.compute(publicKeyIntern, (k, v) -> socketAddressIntern);
			addressToApp.compute(socketAddressIntern, (k, v) -> publicKeyIntern);
			routageConnexion.compute(publicKeyIntern, (k, v) -> new HashSet<>());
			logger.info("\n\nprepare\n" 
					+"pkI: " + publicKeyIntern + "\n saI: " + socketAddressIntern 
					+ "\n\n\n");
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Réinitialise l'état du réseau lors d'une déconnexion.
	 * ---
	 * @param publicKey : Clé publique de l'utilisateur à déconnecter
	 */
	public void resetAtDeconnexion(PublicKeyRSA publicKey) {
		Objects.requireNonNull(publicKey);
		lock.lock();
		try {
	        publicKeys.retainAll(Set.of(publicKey));
	        cachedIndexedPublicKeys.clear();
	        cachedPublicKeyCount = 0;
	        
	        var mySocketAddress = appToAddress.get(publicKey);
	        appToAddress.clear();
	        if(mySocketAddress != null) {
	            appToAddress.put(publicKey, mySocketAddress);
	        }
	        addressToApp.clear();
	        if(mySocketAddress != null) {
	            addressToApp.put(mySocketAddress, publicKey);
	        }
	        
	        lastMessageIDBroadcast.clear();
	        
	        routageConnexion.clear();
	        routageConnexion.put(publicKey, new HashSet<>());
	        
	        appToContext.clear();
	        contextToApp.clear();
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Ajoute une première connexion pour une clé publique donnée.
	 * ---
	 * @param publicKey : Clé publique de l'utilisateur
	 * @param socketAddress : Adresse associée à la clé publique
	 */
	public void makeFirstConnexion(PublicKeyRSA publicKey, SocketAddress socketAddress) {
		Utils.requireNonNulls(publicKey, socketAddress);
		lock.lock();
		try {
			logger.info("\n\nmakeFirstConnexion\n");
			lastMessageIDBroadcast.computeIfAbsent(publicKey, longValSet -> new HashSet<>());
			if(!publicKeys.add(publicKey)) {
				logger.warning("\n\n\nError sur makeFirst:" + "pk: " + publicKey + "\n sa: " + socketAddress +"\n\n\n");
				return ;
			}
			invalidateCache();
			//associé la clé publique de chaque utilisateur à ses infos
			addressToApp.put(socketAddress, publicKey);
			appToAddress.put(publicKey, socketAddress);
			logger.info("pk: " + publicKey + "\n sa: " + socketAddress + "\n\n\n");
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Met à jour le graphe du réseau lors d'une nouvelle connexion entre deux utilisateurs.
	 * ---
	 * @param pkReceiver : Clé publique du receveur
	 * @param pkSender : Clé publique de l'expéditeur
	 */
	public void JoinRoot(PublicKeyRSA pkReceiver, PublicKeyRSA pkSender) {
		Utils.requireNonNulls(pkReceiver, pkSender);
		lock.lock();
		try {
			publicKeys.add(pkSender);publicKeys.add(pkReceiver);
			invalidateCache();
			routageConnexion.computeIfAbsent(pkSender, c -> new HashSet<>()).add(pkReceiver);
			routageConnexion.computeIfAbsent(pkReceiver, c -> new HashSet<>()).add(pkSender);
			logger.info("\n\nJoinRoot\n"
					+ "pkSender: " + pkSender + "\n pkReceiver: " + pkReceiver 
					+ "\n\n\n");
		} finally {
			lock.unlock();
		}
	}


	/******************************
	 ******************************
	 ****** méthode de getter *****TODO
	 ****** + 1 GET/UPDATE    *****
	 ******************************
	 ******************************/


	/**
	 * Récupère une copie de la table des connexions entre utilisateurs.
	 * ---
	 * @return Une HashMap représentant les connexions
	 */
	public HashMap<PublicKeyRSA, HashSet<PublicKeyRSA>> getRoutageConnexion(){
		lock.lock();
		try {
			return new HashMap<>(routageConnexion);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Récupère une map indexée de tous les nœuds connus dans le réseau.
	 * ---
	 * @return Map d'entiers vers objets Node
	 */
	public Map<Integer, Node> getIndexedNodes() {
		lock.lock();
		try {
			if(cachedIndexedPublicKeys != null && publicKeys.size() == cachedPublicKeyCount) {
				return Collections.unmodifiableMap(cachedIndexedPublicKeys);
			}
			cachedIndexedPublicKeys.clear();
			var index = 0;
			for(var pk : publicKeys) {
				var address = (InetSocketAddress) appToAddress.get(pk);
				cachedIndexedPublicKeys.compute(index, (_, __) -> new Node(pk, address));
				index++;
			}
			cachedPublicKeyCount = publicKeys.size();
			return Collections.unmodifiableMap(cachedIndexedPublicKeys);
		} finally {
			lock.unlock();
		}
	}


	/**
	 * Sélectionne un utilisateur inconnu de la clé publique donnée pour établir une nouvelle connexion.
	 * ---
	 * @param publicKey : Clé publique de l'utilisateur demandeur
	 * @return Un nœud aléatoire disponible pour connexion, ou null s'il n'y en a pas
	 */
	public Node getNewConnexion(PublicKeyRSA publicKey) {
		Objects.requireNonNull(publicKey);
		lock.lock();
		try {
			var pubKeys = appToContext.keySet();
			//V2
			logger.info("On a en param: ");
			var cpPubKeys = new HashSet<>(pubKeys);
			cpPubKeys.add(publicKey);
			cpPubKeys.forEach(i -> logger.info(""+i));
			var availableKeys = new HashSet<>(appToAddress.keySet());
			logger.info("On a en interne: ");
			availableKeys.forEach(i -> logger.info(""+i));
			availableKeys.removeAll(cpPubKeys);
			logger.info("Il reste:");
			availableKeys.forEach(i -> logger.info(""+i));
			if(availableKeys.isEmpty()) { return null; }

			var shuffledKeys = new ArrayList<>(availableKeys);
			Collections.shuffle(shuffledKeys);
			var randomKey = shuffledKeys.getFirst();

			var address = (InetSocketAddress) appToAddress.get(randomKey);
			logger.info("On a " + randomKey + " pour " + address);
			return new Node(randomKey, address);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Construit une réponse JoinResponse avec tous les nœuds et connexions connus.
	 * ---
	 * @param idReceiver : Clé publique du demandeur
	 * @return Un objet JoinResponse contenant l'état actuel du réseau
	 */
	public JoinResponse getJoinResponse(PublicKeyRSA idReceiver) {
		lock.lock();
		try {
			var nodes = appToAddress.entrySet().stream()
					.map(entry -> new Node(entry.getKey(), (InetSocketAddress) entry.getValue()))
					.collect(Collectors.toCollection(ArrayList::new));

			var connexions = routageConnexion.entrySet().stream()
					.flatMap(entry -> entry.getValue().stream().map(receiver -> new Connexion(entry.getKey(), receiver)))
					.collect(Collectors.toCollection(ArrayList::new));
			return new JoinResponse(idReceiver, nodes, connexions);
		} finally {
			lock.unlock();
		}
	}
	
	
	/**
	 * Récupère la liste des nœuds connectés localement (voisins directs).
	 * ---
	 * @return Liste de nœuds accessibles directement
	 */
	public List<Node> getNeighborsNodes(){
		lock.lock();
		try {
			var publicKeys = Set.copyOf(appToContext.keySet());
			var list = new ArrayList<Node>();
			logger.info("\n\ngetNeighoborsNodes\n");
			for(var publicKey: publicKeys) {
				if(publicKey == null) { logger.info("Error 1 on gNN"); continue;}
				var address = appToAddress.get(publicKey);
				if(address == null) { logger.info("Error 2 on gNN"); continue;}
				logger.info("pk: " + publicKey + "\n sa: " + address);
				list.add(new Node(publicKey, (InetSocketAddress) address));
			}
			logger.info("\n\n\n");
			return list;
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Fournit une vue non modifiable de la map appToContext.
	 * ---
	 * @return Map immuable de appToContext.
	 */
	public Map<PublicKeyRSA, Context> getViewAppToContext(){
		lock.lock();
		try {
			return Map.copyOf(appToContext);
		} finally {
			lock.unlock();
		}
	}
	

	/**
	 * Retourne le nombre d’éléments dans la map appToContext.
	 * ---
	 * @return Nombre d’associations dans appToContext.
	 */
	public int getSizeAppToContext() {
		lock.lock();
		try {
			return appToContext.size();
		} finally {
			lock.unlock();
		}
	}


	
	/**
	 * Vérifie si le messageID est déjà connu pour cette clé.
	 * Si ce n’est pas le cas, l’ajoute et retourne false.
	 * Si c’est déjà présent, retourne true (déjà vu).
	 * ---
	 * @param publicKey La clé publique de l'expéditeur
	 * @param messageID L'identifiant du message
	 * @return true si le messageID a déjà été vu, false sinon (et il est alors ajouté)
	 */
	public boolean getAndUpdateMessageIDBroadcast(PublicKeyRSA publicKey, long messageID) {
		Objects.requireNonNull(publicKey);
		lock.lock();
		try {
			var set = lastMessageIDBroadcast.computeIfAbsent(publicKey, _ -> new HashSet<>());
	        if(set.contains(messageID)) {
	            return true; //déjà vu
	        }
	        set.add(messageID);
	        return false; //c'est nouveau
		} finally {
			lock.unlock();
		}
	}

	/*********************************
	 *********************************
	 ****** méthode UPDATE/AJOUT *****TODO
	 *********************************
	 *********************************/

	/**
	 * Met à jour les informations d'adresse et de clé publique d'un nœud.
	 * ---
	 * @param node : Nœud à mettre à jour
	 */
	public void updateAppAndAddress(Node node) {
		Objects.requireNonNull(node);
		lock.lock();
		try {
			publicKeys.add(node.publicKey());
			invalidateCache();
			appToAddress.compute(node.publicKey(), (k, v) -> node.socketAddress());
			addressToApp.compute(node.socketAddress(), (k, v) -> node.publicKey());
			logger.info("\n\nUpdateAppAndAddress\n"
					+ "pk: " + node.publicKey() + "\n sa: " + node.socketAddress()
					+ "\n\n\n");
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Met à jour la table de routage en ajoutant une connexion entre deux clés publiques.
	 * Cette méthode assure la symétrie des liens dans les deux sens.
	 * ---
	 * @param connexion : Connexion à ajouter, contenant l'émetteur et le récepteur.
	 */
	public void updateRoutageConnexion(Connexion connexion) {
		Objects.requireNonNull(connexion);
		lock.lock();
		try {
			var setAB = routageConnexion.computeIfAbsent(connexion.publicKeySender(), v -> new HashSet<>());
			var setBA = routageConnexion.computeIfAbsent(connexion.publicKeyReceiver(), v -> new HashSet<>());
            setAB.add(connexion.publicKeyReceiver());
			setBA.add(connexion.publicKeySender());
			publicKeys.add(connexion.publicKeySender());publicKeys.add(connexion.publicKeyReceiver());
			invalidateCache();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Met à jour la table de routage pour une paire clé publique récepteur/émetteur.
	 * ---
	 * @param publicKeyReceiver : Clé publique du récepteur
	 * @param publicKeySender   : Clé publique de l'émetteur
	 */
	public void updatePKReceiver(PublicKeyRSA publicKeyReceiver, PublicKeyRSA publicKeySender) {
		Utils.requireNonNulls(publicKeyReceiver, publicKeySender);
		lock.lock();
		try {
			routageConnexion.computeIfAbsent(publicKeySender, i -> new HashSet<>()).add(publicKeyReceiver);
			routageConnexion.computeIfAbsent(publicKeyReceiver, i -> new HashSet<>()).add(publicKeySender);
			publicKeys.add(publicKeyReceiver);publicKeys.add(publicKeySender);
			logger.info("\n\nupdatePLReceiver\n"
					+ "pkR: " + publicKeyReceiver + "\n pkS: " + publicKeySender
					+ "\n\n\n");
			invalidateCache();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Met à jour les structures de routage et d'adresses lors de l'arrivée d'un nouveau nœud.
	 * ---
	 * @param newNode : Informations du nouveau nœud.
	 */
	public void updateNewNode(NewNode newNode) {
		Objects.requireNonNull(newNode);
		lock.lock();
		try {
			logger.info("\n\nupdateNewNode\n");
			updatePKReceiver(newNode.publicKeySender(), newNode.publicKeyReceiver());
			appToAddress.compute(newNode.publicKeySender(), (k, v) -> newNode.socketAddressSender());
			addressToApp.compute(newNode.socketAddressSender(), (k, v) -> newNode.publicKeySender());
			logger.info("pkS: " + newNode.publicKeySender() + "\n pkR: " + newNode.publicKeySender() + "\n\n\n");
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Met à jour les structures internes à l'arrivée d'une nouvelle connexion.
	 * ---
	 * @param newConnection : Informations de la connexion établie.
	 */
	public void updateNewConnection(NewConnection newConnection) {
		Objects.requireNonNull(newConnection);
		lock.lock();
		try {
			logger.info("\n\nupdateNewConnexion\n");
			updatePKReceiver(newConnection.publicKeySender(), newConnection.publicKeyReceiver());
			logger.info("pkS: " + newConnection.publicKeySender() + "\n pkR: " + newConnection.publicKeyReceiver() + "\n\n\n");
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Supprime un nœud du réseau selon les données contenues dans une trame de type RemoveNode.
	 * ---
	 * @param removeNode : Trame contenant la clé publique à retirer.
	 */
	public void updateRemoveNode(RemoveNode removeNode) {
		Objects.requireNonNull(removeNode);
		lock.lock();
		try {
			//On ne supprimer pas nos infos si on est concerné par un removeNode
			if(myPublicKeyRSA.equals(removeNode.publicKeyLeaver())) { return; }
			deleteUser(removeNode.publicKeyLeaver());
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Ajoute un identifiant de message caché à la mémoire locale.
	 * ---
	 * @param messageID : Identifiant du message caché
	 */
	public void addNewHiddenMessegeID(long messageID) {
		lock.lock();
		try {
			myHiddenMessageID.add(messageID);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Ajoute un SocketChannel à la table s’il n’existe pas déjà pour un port donné.
	 * ---
	 * @param socketChannel : Canal socket à insérer
	 * @param optionalPort  : Port associé
	 * @return true si le canal a été inséré, false s’il existait déjà
	 */
	public boolean putPortAndSocketChannelIfAbsent(SocketChannel socketChannel, int optionalPort) {
		Objects.requireNonNull(socketChannel);
		lock.lock();
		try {
			if(socketChannels.putIfAbsent(optionalPort, socketChannel) != null) {
				logger.info("\n\n++++\n++++Connexion déjà existante !\n++++\n++++\n");
				return false;
			}
			return true;
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Associe une clé publique à un contexte dans la map appToContext.
	 * ---
	 * @param publicKeyRSA : Clé publique de l'application
	 * @param context      : Contexte réseau associé
	 */
	public void putOnAppToContext(PublicKeyRSA publicKeyRSA, Context context) {
		Utils.requireNonNulls(publicKeyRSA, context);
		lock.lock();
		try {
			appToContext.put(publicKeyRSA, context);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Associe un contexte à une clé publique dans la map contextToApp.
	 * ---
	 * @param context      : Contexte à enregistrer
	 * @param publicKeyRSA : Clé publique associée
	 */
	public void putOnContextToApp(Context context, PublicKeyRSA publicKeyRSA) {
		Utils.requireNonNulls(context, publicKeyRSA);
		lock.lock();
		try {
			contextToApp.put(context, publicKeyRSA);
		} finally {
			lock.unlock();
		}
	}
	
	/*********************************
	 *********************************
	 ****** méthode SUPPRESSION ******TODO
	 *********************************
	 *********************************/
	
	/**
	 * Supprime un SocketChannel à partir de son port.
	 * ---
	 * @param port : Port associé au canal à supprimer.
	 */
	public void removeSocketChannelByPort(int port) throws IOException {
		lock.lock();
		try {
			var vals = socketChannels.remove(port);
			vals.close();
		} finally {
			lock.unlock();
		}
	}

	
	/**
	 * Supprime un contexte de la map appToContext à partir de sa clé publique.
	 * ---
	 * @param publicKey : Clé publique dont le contexte doit être retiré
	 * @return Le contexte supprimé ou null si non trouvé
	 */
	public Context removeContextWithPublicKey(PublicKeyRSA publicKey) {
		Objects.requireNonNull(publicKey);
		lock.lock();
		try {
			return appToContext.remove(publicKey);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Supprime une clé publique à partir de son contexte associé.
	 * ---
	 * @param context : Contexte à dissocier
	 * @return La clé publique supprimée ou null si absente
	 */
	public PublicKeyRSA removePublicKeyWithContext(Context context) {
		Objects.requireNonNull(context);
		lock.lock();
		try {
			return contextToApp.remove(context);
		} finally {
			lock.unlock();
		}
	}
	
	/**********************************
	 **********************************
	 ****** méthode VÉRIFICATION ******TODO
	 **********************************
	 **********************************/
	
	/**
	 * Vérifie si un message caché identifié est reconnu localement.
	 * ---
	 * @param messageID : Identifiant du message
	 * @return true si ce message est bien connu, false sinon
	 */
	public boolean verifyAcknowlegdmentHiddenMessegeID(long messageID) {
		lock.lock();
		try {
			return myHiddenMessageID.contains(messageID);
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Vérifie si une clé publique est présente dans le réseau actuel.
	 * ---
	 * @param publicKey : Clé publique à vérifier
	 * @return true si présente, false sinon
	 */
	public boolean publicKeyIsOnNetwork(PublicKeyRSA publicKey) {
		Objects.requireNonNull(publicKey);
		lock.lock();
		try {
			return publicKeys.contains(publicKey);
		} finally {
			lock.unlock();
		}
	}
	
	/*********************************
	 *********************************
	 ****** méthode DÉCONNEXION ******TODO
	 *********************************
	 *********************************/
	
	/**
	 * Supprime une application du réseau via son contexte et met à jour toutes les structures associées.
	 * ---
	 * @param context          : Contexte à supprimer
	 * @return La clé publique de l'application supprimée, ou null si non trouvée
	 */
	public PublicKeyRSA disconnectAppWithContextToApp(Context context) {
		Objects.requireNonNull(context);
		lock.lock();
		try {
			var publicKey = contextToApp.getOrDefault(context, null);
			if(null == publicKey) {
				logger.info("application not found to disconnect");
				return null;
			}
			deleteUser(publicKey);
			return publicKey;
		} finally {
			lock.unlock();
		}
	}

	
	/********************************
	 ********************************
	 ****** méthode ENVOIE MSG ******TODO
	 ********************************
	 ********************************/
	
	/**
	 * Tente d'envoyer un paquet via le contexte associé à une application intermédiaire.
	 * ---
	 * @param appIntermediaire : Clé publique de l'application relais
	 * @param paquet           : Paquet à envoyer
	 */
	public boolean sendMessageWithAppToContext(PublicKeyRSA appIntermediaire, Paquet paquet) {
		Utils.requireNonNulls(appIntermediaire, paquet);
		lock.lock();
		try {
			var contextIntermediaire = appToContext.get(appIntermediaire);
			if(contextIntermediaire != null) {
				logger.info("==7.1==");
				logger.info("Message en transit via : " + appIntermediaire);
				contextIntermediaire.queuePaquet(paquet);
				return true;
			}
			logger.info("==8==");
			logger.info("Pas de contexte pour " + appIntermediaire + ", impossible de router.");
			return false;
		} finally {
			lock.unlock();
		}
	}
	
	
	/*******************************
	 *******************************
	 ****** méthode AFFICHAGE ******TODO
	 *******************************
	 *******************************/
	
	/**
	 * Affiche un état lisible des connexions réseau et des adresses associées.
	 * ---
	 * @return Chaîne descriptive des connexions réseau.
	 */
	public String toStringRootConnexions() {
		lock.lock();
		try {
            return routageConnexion.entrySet().stream()
                    .map(entry -> {
                        var senderPK = entry.getKey();
                        var senderAddr = appToAddress.get(senderPK);
                        var connected = entry.getValue().stream()
                                .map(pk -> {
                                    var socketAddr = appToAddress.get(pk);
                                    return "\t-> " + pk + ": " + socketAddr;
                                })
                                .collect(Collectors.joining("\n"));
                        return senderPK + ": " + senderAddr + "\n" + connected + "\n";
                    })
                    .collect(Collectors.joining("\n", "\n--- Affichage des connexions du réseau ---\n", "\n------------------------------------------\n"));
		} finally {
			lock.unlock();
		}
	}
	
	/****************************
	 ****************************
	 ****** méthode PRIVÉE ******TODO
	 ****************************
	 ****************************/
	
	private void deleteUser(PublicKeyRSA publicKey) {
		invalidateCache();
		if(null == publicKey) {
			logger.info("null on deleteUser => data user aren't registered or is already deleted !");
			return;
		}
		//suppression du set des clés connecté <PKRSA>
		if(!publicKeys.remove(publicKey)) { logger.info("Error on deleteUser: key unknow !"); return; }
		//suppression au niveau de la table de reconnaissance <PKRSA, ISA>
		var socketAddress = appToAddress.remove(publicKey);
		if(null != socketAddress) {
			addressToApp.remove(socketAddress);
		}
		logger.info("En supprimant " + publicKey + " on a " + (lastMessageIDBroadcast.containsKey(publicKey) ? lastMessageIDBroadcast.get(publicKey) : "Rien"));
		//suppression du dernier long des messages par broadcast
		lastMessageIDBroadcast.remove(publicKey);
		//suppression sur la table de routage !
		var connexionNodes = routageConnexion.remove(publicKey);
		if(null == connexionNodes) {
			logger.info("Error, publicKey is not in routage table! dU-IU");
			return;
		}
		connexionNodes.forEach(otherPublicKey -> removePublicKeyFromRoutingTable(otherPublicKey, publicKey));
	}

	
	private void removePublicKeyFromRoutingTable(PublicKeyRSA otherPublicKey, PublicKeyRSA publicKey) {
		Utils.requireNonNulls(publicKey, otherPublicKey);
		routageConnexion.computeIfPresent(otherPublicKey, (key, otherSet) -> {
			otherSet.remove(publicKey);
			return otherSet.isEmpty() ? new HashSet<PublicKeyRSA>() : otherSet;
		});
	}
	
	
	private void invalidateCache() {
		cachedIndexedPublicKeys.clear();
		cachedPublicKeyCount = -1;
	}

	//END
}