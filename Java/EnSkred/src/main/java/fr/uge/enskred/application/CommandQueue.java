package fr.uge.enskred.application;

import java.util.ArrayDeque;
import java.util.concurrent.locks.ReentrantLock;

import fr.uge.enskred.paquet.Paquet;


/**
 * Représente une file d’attente threadSafe permettant de séparer les paquets
 * de type commande et ceux de type message.
 * ---
 * Cette classe utilise un verrou explicite pour garantir la sécurité des accès
 * concurrents. Elle permet d’ajouter ou retirer des paquets selon leur nature
 * (commande ou message) et de vérifier l’état de chaque file.
 */
public final class CommandQueue {
	private final ReentrantLock lock = new ReentrantLock();
	//for the command
	private final ArrayDeque<Paquet> commandQueue = new ArrayDeque<>();
	//for the message
	private final ArrayDeque<Paquet> messageQueue = new ArrayDeque<>();
	
	
	//nous supposerons que chaque commande et message ne sont pas null
	public void offerCommand(Paquet command) {
		lock.lock();
		try {
			commandQueue.offer(command);
		} finally {
			lock.unlock();
		}
	}
	
	public void offerMessage(Paquet message) {
		lock.lock();
		try {
			messageQueue.offer(message);
		} finally {
			lock.unlock();
		}
	}
	
	public Paquet pollCommand() {
		lock.lock();
		try {
			return commandQueue.poll();
		} finally {
			lock.unlock();
		}
	}
	
	public Paquet pollMessage() {
		lock.lock();
		try {
			return messageQueue.poll();
		} finally {
			lock.unlock();
		}
	}
	
	public boolean isEmptyCommandQueue() {
		lock.lock();
		try {
			return commandQueue.isEmpty();
		} finally {
			lock.unlock();
		}
	}
	
	public boolean isEmptyMessageQueue() {
		lock.lock();
		try {
			return messageQueue.isEmpty();
		} finally {
			lock.unlock();
		}
	}
	
	
}
