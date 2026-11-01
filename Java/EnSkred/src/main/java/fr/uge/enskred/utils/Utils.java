package fr.uge.enskred.utils;

import fr.uge.enskred.paquet.MessageToSecure;
import fr.uge.enskred.paquet.Paquet;
import fr.uge.enskred.paquet.PassForward;
import fr.uge.enskred.paquet.SecureMessage;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PrivateKeyRSA;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.ShortBufferException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * La classe {@code Utils} contient des méthodes utilitaires pour diverses opérations, telles que la génération de nombres aléatoires,
 * la sérialisation de clés publiques RSA, et la gestion de l'encodage et du décodage des messages sécurisés.
 * Elle inclut également des algorithmes de routage basés sur des clés publiques et d'autres outils liés à la sécurité des données.
 *	<li>
 * 		Cette classe est utilisée principalement pour la gestion des clés, des messages sécurisés et la communication entre nœuds dans un réseau.
 *  </li>
 */
public final class Utils {
	private final static Logger logger = java.util.logging.Logger.getLogger(Utils.class.getName());
	private static final String HELP_MESSAGE = """
		    
			-----------------------------------------------------------------
			Liste des commandes disponibles :
			-mp <ID> <message> : Envoi un message public au client avec l'ID spécifié.
			-mc <ID> <message> : Envoi un message privé caché au client avec l'ID spécifié.
			-l               : Demande la liste des utilisateurs connectés.
			-d               : Demande de déconnexion.
			-r               : Demande l'état du réseau.
			-h               : Obtenir les commandes disponible.

			Exemple de commande :
			-mp 2 Bonjour !  : Envoi un message public à l'utilisateur ayant l'ID 2.
			-mc 3 Salut !    : Envoi un message privé caché à l'utilisateur ayant l'ID 3.
			-----------------------------------------------------------------

			""";
	
	
	/**
	 * Génère un long aléatoire qui est garanti d'être différent de zéro.
	 * ---
	 * @return Un long aléatoire, différent de zéro.
	 */
	public static long generateRandomLong() {
		var result = ThreadLocalRandom.current().nextLong();
		return result == 0 ? 1 : result;
	}

	/**
	 * Génère un long aléatoire qui est garanti d'être différent de zéro.
	 * ---
	 * @return Un long aléatoire, différent de zéro.
	 */
	public static <T extends Comparable<T>> T min(T val1, T val2) {
		//requireNonNulls(val1, val2);
		return (val1.compareTo(val2) <= 0) ? val1 : val2;
	}

	/**
	 * Vide une collection générique si elle n'est pas nulle.
	 * ---
	 * @param <T> La collection à vider.
	 * @param collection La collection à vider.
	 */
	public static <T extends Collection<?>> void safeClear(T collection) {
		if(collection != null) {
			collection.clear();
		}
	}


	/**
	 * Sérialise deux clés publiques RSA dans un seul buffer de bytes.
	 * ---
	 * @param sender La clé publique de l'expéditeur.
	 * @param receiver La clé publique du récepteur.
	 * @return Un ByteBuffer contenant les clés publiques sérialisées.
	 * @throws NullPointerException Si l'une des clés est nulle.
	 */
	public static ByteBuffer serializeTwoKPublicKeys(PublicKeyRSA sender, PublicKeyRSA receiver) {
		requireNonNulls(sender, receiver);
		//PKSender
		var pubBufferSender = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		sender.to(pubBufferSender);
		var PKBufferSender = ByteBuffer.allocate(Integer.BYTES + pubBufferSender.flip().remaining());
		PKBufferSender.putInt(pubBufferSender.remaining()).put(pubBufferSender).flip();
		//PKReceiver
		var pubBufferReceiver = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		receiver.to(pubBufferReceiver);
		var PKBufferReceiver = ByteBuffer.allocate(Integer.BYTES + pubBufferReceiver.flip().remaining());
		PKBufferReceiver.putInt(pubBufferReceiver.remaining()).put(pubBufferReceiver).flip();

		var buffer = ByteBuffer.allocate(PKBufferSender.remaining() + PKBufferReceiver.remaining());
		return buffer.put(PKBufferSender).put(PKBufferReceiver);

	}


	/**
	 * Encrypte un long avec une clé publique RSA et renvoie un ByteBuffer contenant le message chiffré.
	 * Si une erreur se produit pendant l'encryptage, retourne un buffer avec 0.
	 * ---
	 * @param publicKey La clé publique RSA utilisée pour l'encryptage.
	 * @param value La valeur à chiffrer.
	 * @return Un ByteBuffer contenant la valeur chiffrée, ou un buffer avec 0 en cas d'erreur.
	 * @throws NullPointerException Si la clé publique est nulle.
	 */
	public static ByteBuffer encodeLongWithPublicKeyInWriteMode(PublicKeyRSA publicKey, long value) {
		Objects.requireNonNull(publicKey);
		try {
			var longBuffer = ByteBuffer.allocate(Long.BYTES).putLong(value).flip();
			return UGEncrypt.encryptRSA(longBuffer, publicKey);
		} catch (InvalidKeyException | ShortBufferException | IllegalBlockSizeException e) {
			logger.info("Error with encrypt");
			return ByteBuffer.allocate(Integer.BYTES).putInt(0);
		}
	}

	/**
	 * Décrypte un long avec une clé privée RSA à partir d'un ByteBuffer.
	 * Si une erreur se produit pendant le décryptage, retourne 0.
	 * ---
	 * @param privateKey La clé privée RSA utilisée pour le décryptage.
	 * @param buffer Le ByteBuffer contenant le message chiffré.
	 * @return La valeur décryptée ou 0 en cas d'erreur.
	 * @throws NullPointerException Si la clé privée ou le buffer est nul.
	 */
	public static long decodeLongWithPrivateKey(PrivateKeyRSA privateKey, ByteBuffer buffer) {
		requireNonNulls(privateKey, buffer);
		try {
			var decryptedBlock = UGEncrypt.decryptRSA(buffer, privateKey);
            return decryptedBlock.flip().getLong();
		} catch (ShortBufferException | IllegalBlockSizeException | BadPaddingException e) {
			logger.info("Error with decrypt");
			//Une valeur d'erreur !
			return 0L;
		}
	}


	/**
	 * Effectue un chiffrement RSA sécurisé d'un message et renvoie le ByteBuffer contenant le message chiffré.
	 * Si une erreur se produit pendant le chiffrement, retourne un buffer avec 0.
	 * ---
	 * @param payload Le message à chiffrer.
	 * @param recipient La clé publique du destinataire.
	 * @return Un ByteBuffer contenant le message chiffré, ou un buffer avec 0 en cas d'erreur.
	 * @throws NullPointerException Si le message ou la clé publique est nulle.
	 */
	public static ByteBuffer safeEncryptRSA(ByteBuffer payload, PublicKeyRSA recipient) {
		requireNonNulls(payload, recipient);
		try {
			return UGEncrypt.encryptRSA(payload, recipient);
		} catch(InvalidKeyException | ShortBufferException | IllegalBlockSizeException error) {
			return ByteBuffer.allocate(Integer.BYTES).putInt(0);
		}
	}

	/**
	 * Décrypte un message RSA sécurisé et renvoie le ByteBuffer contenant le message décrypté.
	 * Si une erreur se produit pendant le décryptage, retourne un buffer vide.
	 * ---
	 * @param payload Le message chiffré à décrypter.
	 * @param recipient La clé privée du destinataire.
	 * @return Un ByteBuffer contenant le message décrypté, ou un buffer vide en cas d'erreur.
	 * @throws NullPointerException Si le message ou la clé privée est nulle.
	 */
	public static ByteBuffer safeDecryptRSA(ByteBuffer payload, PrivateKeyRSA recipient) {
		requireNonNulls(payload, recipient);
		try {
			return UGEncrypt.decryptRSA(payload, recipient);
		} catch(BadPaddingException | ShortBufferException | IllegalBlockSizeException error) {
			return ByteBuffer.allocate(0);
		}
	}




	/**
	 * Vérifie que tous les objets fournis ne sont pas nuls.
	 * ---
	 * @param objects Les objets à vérifier.
	 * @throws NullPointerException Si l'un des objets est nul.
	 */
	public static void requireNonNulls(Object... objects) {
		Objects.requireNonNull(objects);
		for(var obj : objects){
			Objects.requireNonNull(obj);
		}
	}

	/**
	 * Effectue un routage "Onion" pour sécuriser un message à travers une liste de nœuds intermédiaires.
	 * ---
	 * @param publicKey La clé publique de l'expéditeur.
	 * @param wayToDestination La liste des nœuds intermédiaires jusqu'à la destination.
	 * @param message Le message à envoyer.
	 * @param idMessage L'ID unique du message.
	 * @return Le paquet sécurisé prêt à être envoyé.
	 * @throws NullPointerException Si l'une des entrées est nulle.
	 */
	public static Paquet onionRoutingForSecureMessage(PublicKeyRSA publicKey, List<PublicKeyRSA> wayToDestination, String message, long idMessage) {
		requireNonNulls(publicKey, wayToDestination, message);
		if(wayToDestination.isEmpty()) { return null; }

		var messageToDest = new SecureMessage(wayToDestination.getFirst(), new MessageToSecure(publicKey, idMessage, message), null);

		if(wayToDestination.size() == 1) { return messageToDest; }

		var finalMsg = messageToDest;

		for(var hopIndex = 1; hopIndex < wayToDestination.size(); ++hopIndex) {
			var instruction = new PassForward(wayToDestination.get(hopIndex - 1), finalMsg);
			finalMsg = new SecureMessage(wayToDestination.get(hopIndex), instruction, null);
		}

		return finalMsg;
	}

	/**
	 * Crée un message d'accusé de réception sécurisé après l'envoi d'un message sécurisé, en utilisant le routage "Onion" pour sécuriser l'acheminement.
	 * <r>&nbsp;</r>
	 * Cette méthode utilise la méthode {@link #onionRoutingForSecureMessage(PublicKeyRSA, List, String, long)} pour générer un message sécurisé de type accusé de réception, 
	 * en envoyant un message vide à travers la chaîne de nœuds intermédiaires, ce qui permet de signaler la réception du message sécurisé initial.
	 * 
	 * @param publicKey La clé publique de l'expéditeur, utilisée pour l'authentification du message.
	 * @param wayToDestination La liste des nœuds intermédiaires à travers lesquels le message devra passer avant d'atteindre sa destination finale.
	 * @param idMessage L'ID unique du message pour lequel un accusé de réception est généré.
	 * @return Un objet {@link SecureMessage} représentant l'accusé de réception sécurisé, prêt à être envoyé à travers le réseau.
	 * @throws NullPointerException Si la clé publique ou la liste des nœuds intermédiaires est nulle.
	 */
	public static SecureMessage acknowledgmentAfterSecureMessage(PublicKeyRSA publicKey, List<PublicKeyRSA> wayToDestination, long idMessage) {
		requireNonNulls(publicKey, wayToDestination);
		return (SecureMessage) onionRoutingForSecureMessage(publicKey, wayToDestination, "", idMessage);
	}
	
	/**
	 * Affiche dans la console les commandes disponibles de l'application.
	 * <p>
	 * Commandes :
	 * <ul>
	 *   <li><b>-mp &lt;ID&gt; &lt;message&gt;</b> : message public</li>
	 *   <li><b>-mc &lt;ID&gt; &lt;message&gt;</b> : message privé (caché)</li>
	 *   <li><b>-l</b> : liste des utilisateurs connectés</li>
	 *   <li><b>-d</b> : déconnexion</li>
	 *   <li><b>-r</b> : état du réseau</li>
	 * </ul>
	 * Ex : {@code -mp 2 Bonjour !}
	 */
	public static void printHelp() {
		System.out.println(HELP_MESSAGE);
	}

}





















