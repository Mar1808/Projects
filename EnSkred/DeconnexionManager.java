package fr.uge.enskred.application;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.enskred.application.Application.Context;
import fr.uge.enskred.paquet.Broadcast;
import fr.uge.enskred.paquet.LeaveNetworkAsk;
import fr.uge.enskred.paquet.LeaveNetworkCancel;
import fr.uge.enskred.paquet.LeaveNetworkConfirm;
import fr.uge.enskred.paquet.LeaveNetworkResponse;
import fr.uge.enskred.paquet.Node;
import fr.uge.enskred.paquet.RemoveNode;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

public final class DeconnexionManager {
	final static private Random random = new Random();
	final static private Logger logger = Logger.getLogger(DeconnexionManager.class.getName());
	final static private byte CAN_TREAT_DECONNEXION = 1;
	final static private byte CANNOT_TREAT_DECONNEXION = 0;
	public enum DeconnexionState {
		/********************
		 * CELUI QUI RECOIT *
		 ********************/
		ALREADY_OCCUPED,		//Dans le cas ou l'appli sert à deconnecté qq'un
		/********************
		 **** EN COMMUN  ****
		 ********************/
		INACTIVE,				//Aucune déconnexion en cours
		/********************
		 * CELUI QUI INITIE *
		 ********************/
		WAITING_RESPONSES,      //En attente de réponses des voisins directs
		CANCELLED,              //Déconnexion annulée (au moins un refus)
		WAITING_DONE,           //En attente des DONE des voisins
		DONE                   	//Tout est terminé, prêt pour REMOVE_NODE
	}

	//Application 
	final private Application application;
	//Context de l'application 
	private PublicKeyRSA pubkeyWantDeconnect;
	//context des voisins direct
	private final Map<PublicKeyRSA, Context> appToContext = new HashMap<>();
	private final Map<PublicKeyRSA, Boolean> responseReceived = new HashMap<>();
	private final Lock lock = new ReentrantLock();
	//etat
	private DeconnexionState state = DeconnexionState.INACTIVE;

	//Constructor
	public DeconnexionManager(Application application, Level level) {
		this.application = Objects.requireNonNull(application);
		logger.setLevel(level == null ? Level.SEVERE : level);
	}

	//Public methods

	/************************
	 **** COTE DEMANDEUR **** 
	 ************************/

	/**
	 * Initialise une demande de déconnexion pour une application.
	 * ---
	 * @param publicKey : Clé publique de l'application souhaitant se déconnecter
	 * @param appToContext : Map des voisins directs avec leur contexte associé
	 */
	public void initDeconnexion(PublicKeyRSA publicKey, Map<PublicKeyRSA, Context> appToContext) {
		Utils.requireNonNulls(publicKey, appToContext);
		lock.lock();
		try {
			if(state != DeconnexionState.INACTIVE) { 
				logger.info("You can't initiate deconnexion ! because you help already application to disconnect him");
				//cancelDeconnexion();
				state = DeconnexionState.INACTIVE;
				return; 
			}
			reset();
			pubkeyWantDeconnect = publicKey;
            this.appToContext.putAll(appToContext);
			//Envoyer les paquets ACK
			this.appToContext.forEach((_, context) -> context.queuePaquet(new LeaveNetworkAsk()));
			state = DeconnexionState.WAITING_RESPONSES;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Traite la réponse d’un voisin à une demande de déconnexion.
	 * ---
	 * @param publicKey : Clé publique du voisin ayant répondu
	 * @param response : Réponse du voisin (true si acceptée, false sinon)
	 * @param list : Liste des nœuds que le voisin devra connecter s’il accepte
	 */
	public void receiveDeconnexion(PublicKeyRSA publicKey, boolean response, List<Node> list) {
		Utils.requireNonNulls(publicKey, list);
		lock.lock();
		try {
			if(state != DeconnexionState.WAITING_RESPONSES) {logger.info("receiveConnexionERROR !");return;}
			responseReceived.compute(publicKey, (_, resp) -> response);
			if(!response) {
				cancelDeconnexion();
				reset();
				return;
			}
	        logger.info("receiveDeconnexion\nsR="+responseReceived.size()+"\naTC="+appToContext.size());
			if(responseReceived.size() == appToContext.size() &&
					responseReceived.values().stream().allMatch(Boolean::booleanValue)){
				sendLeaveNetworkConfirm(list);
			}
		} finally {
			lock.unlock();
		}
	}
	
	private void cancelDeconnexion() {
	    for (var context : appToContext.values()) {
	        context.queuePaquet(new LeaveNetworkCancel());
	    }
	    state = DeconnexionState.CANCELLED;
	}


    private void sendLeaveNetworkConfirm(List<Node> list) {
        logger.info("------------------>>> LA sendLeaveNetworkConfirm");
        list.forEach(v -> logger.info(v.publicKey()+"___"+v.socketAddress()));
        var appli = new ArrayList<>(appToContext.entrySet());
        if(!appli.isEmpty()) {
            var hazardNumber = random.nextInt(appli.size());
            var hazardAppli = appli.remove(hazardNumber);
            var listForHazard = new ArrayList<>(list);
            listForHazard.removeIf(t -> t.publicKey().equals(hazardAppli.getKey()));
            logger.info("\n\nLe receveur sera: " + hazardAppli.getKey()+"\n\n");
            hazardAppli.getValue().queuePaquet(new LeaveNetworkConfirm(listForHazard));
        }
        for(var entry : appli) {
            var context = entry.getValue();
            var pk = entry.getKey();
            var listForHazard = new ArrayList<>(list);
            listForHazard.removeIf(t -> t.publicKey().equals(pk));
            context.queuePaquet(new LeaveNetworkConfirm(listForHazard));
        }
        responseReceived.clear();
        state = DeconnexionState.WAITING_DONE;
    }


	/**
	 * Traite la réception d’un paquet DONE indiquant qu’un voisin a terminé les connexions.
	 * ---
	 * @param sender : Clé publique du voisin ayant envoyé le DONE
	 * @return true si toutes les réponses DONE ont été reçues, false sinon
	 */
	public boolean receiveDone(PublicKeyRSA sender) {
	    lock.lock();
	    try {
	        if(state != DeconnexionState.WAITING_DONE) {
	            logger.info("receiveDone ERROR: wrong state.");
	            return false;
	        }
	        responseReceived.compute(sender, (pk, val) -> true);
	        logger.info("receiveDone\nsR="+responseReceived.size()+"\naTC="+appToContext.size());
	        if(responseReceived.size() == appToContext.size() &&
	            responseReceived.values().stream().allMatch(Boolean::booleanValue)) {
	            finishDeconnexion();
	            return true;
	        }
	        return false;
	    } finally {
	        lock.unlock();
	    }
	}

	private void finishDeconnexion() {
	    logger.info("Déconnexion complétée pour : " + pubkeyWantDeconnect);
	    state = DeconnexionState.DONE;
	    var removeNode = new RemoveNode(pubkeyWantDeconnect);
		var payload = removeNode.getWriteModeBuffer().flip();
		var size = payload.remaining();
	    var broadcast = new Broadcast(pubkeyWantDeconnect, Utils.generateRandomLong(), size, payload.compact());
	    //PAS DE MAJ du long de broadcast car on se déconnecte
	    application.broadcast(broadcast, appToContext.get(pubkeyWantDeconnect));
	    //appToContext.forEach((pubKey, context) -> application.disconnectApp(context));
	    //application.terminate();
	}



	/***********************
	 **** COTE RECEVEUR **** 
	 ***********************/
	
	/**
	 * Traite une demande de déconnexion reçue d’un autre nœud.
	 * ---
	 * @param context : Contexte du demandeur
	 * @param appToContext : Map des voisins directs avec leur contexte associé
	 */
	public void treatAsk(Context context, Map<PublicKeyRSA, Context> appToContext) {
		Utils.requireNonNulls(context, appToContext);
		lock.lock();
		try {
			if(state != DeconnexionState.INACTIVE) {
				//Si notre clé est plus petite que celle qui vient, on arrete notre déco
			    if(pubkeyWantDeconnect.equals(Utils.min(pubkeyWantDeconnect, context.publicKeyExtern()))) {
			    	context.queuePaquet(new LeaveNetworkResponse(CANNOT_TREAT_DECONNEXION));
			        return;
			    }
			    //Sinon on renonce à notre déconnexion et on accepte celle qui vient
			    cancelDeconnexion();
			    acceptDeconnexion(context, appToContext);
			}
			acceptDeconnexion(context, appToContext);
		} finally {
			lock.unlock();
		}
	}
	private void acceptDeconnexion(Context context, Map<PublicKeyRSA, Context> appToContext) {
		reset();
		//remplir notre propre appToContext
        this.appToContext.putAll(appToContext);
		pubkeyWantDeconnect = context.publicKeyExtern();
		context.queuePaquet(new LeaveNetworkResponse(CAN_TREAT_DECONNEXION));
		state = DeconnexionState.ALREADY_OCCUPED;
	}
	
	/**
	 * Traite l’annulation d’une demande de déconnexion par un autre nœud.
	 */
	public void treatCancel() {
		lock.lock();
		try {
			reset();
		} finally {
			lock.unlock();
		}
	}
	
	/**
	 * Traite la confirmation de déconnexion reçue et renvoie les clés publiques des applications concernées.
	 * ---
	 * @return Set des clés publiques des voisins concernés par l’établissement de connexions
	 */
	public Set<PublicKeyRSA> treatConfirm() {
		lock.lock();
		try {
			return Set.copyOf(appToContext.keySet());
		}finally {
			lock.unlock();
		}
	}

	/**
	 * Renvoie l’état courant de la déconnexion.
	 * ---
	 * @return L’état actuel du DeconnexionManager
	 */
	public DeconnexionState state() {
		lock.lock();
		try {
			return state;
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Réinitialise manuellement le gestionnaire de déconnexion.
	 * ---
	 */
	public void resetDeconnexionManager() {
		lock.lock();
		try {
			reset();
		}finally {
			lock.unlock();
		}
	}

	//Private methods
	private void reset() {
		pubkeyWantDeconnect = null;
		appToContext.clear();
		state = DeconnexionState.INACTIVE;
	}
	
	/**
	 * Affiche sous forme de chaîne l’état actuel du gestionnaire de déconnexion.
	 * ---
	 * @return Une chaîne représentant l’état, la clé en déconnexion et les voisins concernés
	 */
	public String toString() {
	    lock.lock();
	    try {
	    	return "=== Etat de déconnexion ===\n" + 
	    		    " - Etat : " + state + "\n" +
	    		    " - Clé en déconnexion : " + pubkeyWantDeconnect + "\n" + 
	    		    " - Voisins concernés : " + appToContext.keySet();
	    } finally {
	    	lock.unlock();
	    }
	}

	
}

/**
 * I 	-> Leaver envoie à ses voisins proches (1e degré) un ASK
 * 
 * II 	-> ses voisins du 1er degré: un RESPONSE
 * II.1		-> Renvoie 1 si le voisin n'accepte pas la déco d'un autre de ses voisins ou 0
 * II.2		-> Renvoie 0 si le voisin à initier sa dmd de déco en même temps qu'un ask, et
 * si sa PK est plus petite que celle du leaver, sinon renvoie 1.
 * Si elle renvoie 1, elle annule sa demande de déco auprès de ses voisins.
 * 
 * III	-> Leaver attends de recevoir la réponse de ses voisins directs
 * III.1	-> Si leaver recoit au moins une rep négative, elle annule sa dmd de déco.
 * et renvoie un CANCEL.
 * 			-> Si elle recoit une rep positive de tt ses voisins directs,
 * et renvoie un CONFIRM à ses voisins directs.
 * (A PARTIR DE LA, ON POURRA RENVOYER UNE VRAI LISTE PLEINE À UNE SEULE PERSONNE
 * ET ON RENVERRA DES LISTES VIDES AUX AUTRES. PB: SI DÉCONNEXIONS BRUTALE SINON O.K.)
 * 
 * IV	-> Les voisins directs qui recoivent CONFIRM vont établir des connexions avec les NODES
 * Une fois que tte les connexions sont établie, le voisin direct renvoie DONE à Leaver.
 * 
 * V	-> Leaver attends de recevoir tt les DONE.
 * V.1		-> Ensuite, elle renvoie un BROADCAST de type REMOVE_NODE à tte les appli du réseau
 * V.1.1	-> Quand une appli recoit REMOVE_NODE, elle maj sa vue du réseau en supprimant le noeuds
 * et les connexions correspondantes au payload.
 * 
 * VI	-> Leaver ferme toutes les connexions TCP avec ses voisins directs.
 * 
 */
