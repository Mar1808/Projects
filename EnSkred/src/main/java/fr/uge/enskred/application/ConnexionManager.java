package fr.uge.enskred.application;

import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.enskred.application.Application.Context;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * Le {@code ConnexionManager} est responsable de la gestion des connexions réseau
 * d'une application membre du réseau Enskred. Il gère une connexion principale
 * unique (appelée {@code uniqueContext}) ainsi qu'un ensemble de connexions
 * secondaires associées à des clients identifiés par leur clé publique RSA.
 *
 * <p>Chaque connexion est représentée par un {@link ContextSlot}, qui regroupe :
 * <ul>
 *   <li>le {@code Context} (canal de communication NIO)</li>
 *   <li>le numéro de port</li>
 *   <li>la clé publique RSA du client distant</li>
 *   <li>son adresse socket</li>
 * </ul>
 *
 * <p>Les connexions sont indexées et peuvent être récupérées via leur port,
 * leur clé publique, ou leur index interne.
 *
 * <p>Le gestionnaire permet également le remplacement automatique de
 * {@code uniqueContext} lorsqu’il est déconnecté.
 */
public final class ConnexionManager {
	private final static Logger logger = Logger.getLogger(ConnexionManager.class.getName());
	private final Map<Integer, ContextSlot> indexToSlot;
	private final Map<Integer, Integer> portToIndex;
	private final Map<PublicKeyRSA, Integer> keyToIndex;
	private final Queue<Integer> freeIndexes;
	private final Lock lock;
	private int nextIndex;

	public ConnexionManager(Level level) {
		lock = new ReentrantLock();
		indexToSlot = new HashMap<>();
		portToIndex = new HashMap<>();
		keyToIndex = new HashMap<>();
		freeIndexes = new ArrayDeque<>();
		logger.setLevel(level == null ? Level.SEVERE : level);
	}
	
	/**
	 * Représente un slot de connexion contenant le contexte réseau,
	 * la clé publique, le port distant, et l'adresse socket du client.
	 * ---
	 * @param context:	Le contexte de la connexion
	 * @param port:	   	Le port distant utilisé
	 * @param key: 		La clé publique RSA du client distant
	 * @param address: 	L'adresse socket du client distant
	 */
	record ContextSlot(Context context, int port, PublicKeyRSA key, InetSocketAddress address) {
		ContextSlot { Utils.requireNonNulls(context, key, address); }
	}

	/**
	 * Enregistre un nouveau contexte de connexion dans la structure de gestion.
	 * ---
	 * @param context:	Le Contexte de la nouvelle connexion
	 * @param port:     Le Port distant associé à cette connexion
	 * @param key:      La Clé publique du client distant
	 * @param address:  L'Adresse socket du client distant
	 */
	public void register(Context context, int port, PublicKeyRSA key, InetSocketAddress address) {
		Utils.requireNonNulls(context, key, address); 
		lock.lock();
		try {
			var index = freeIndexes.isEmpty() ? nextIndex++ : freeIndexes.poll();
			var slot = new ContextSlot(context, port, key, address);
			indexToSlot.put(index, slot);
			portToIndex.put(port, index);
			keyToIndex.put(key, index);
		} finally {
			lock.unlock();
		}
		lock.lock();
	}

	/**
	 * Supprime une connexion à partir d'une clé publique connue.
	 * ---
	 * @param publicKey: La Clé publique du client distant à retirer
	 */
	public int unregisterByKey(PublicKeyRSA publicKey) {
		Objects.requireNonNull(publicKey);
		lock.lock();
		try {
			var index = keyToIndex.remove(publicKey);
			if(index == null) {
				logger.info("Il n'existe aucun context associé à cette publicKey auquel vous êtes connecté.");
				return 0;
			}
			var slot = indexToSlot.remove(index);
			portToIndex.remove(slot.port());
			freeIndexes.offer(index);
			return slot.port();
		}finally {
			lock.unlock();
		}
	}
	
}
