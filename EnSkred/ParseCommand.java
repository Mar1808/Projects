package fr.uge.enskred.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import fr.uge.enskred.paquet.ListConnected;
import fr.uge.enskred.paquet.Message;
import fr.uge.enskred.paquet.MessagePublic;
import fr.uge.enskred.paquet.Node;
import fr.uge.enskred.paquet.Paquet;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;


/**
 * La classe {@code ParseCommand} est responsable de l’analyse et de l’interprétation
 * des commandes utilisateur saisies dans la console.
 * <p>
 * Elle permet de générer des objets {@link Paquet} correspondant à ces commandes,
 * que ce soit pour envoyer des messages publics, privés (sécurisés via onion routing),
 * ou pour effectuer des actions réseau comme la déconnexion ou la récupération
 * des utilisateurs connectés.
 * </p>
 */
public final class ParseCommand {
	private final static Logger logger = Logger.getLogger(ParseCommand.class.getName());
	
	  /**
	   * Analyse une commande utilisateur et construit le {@link Paquet} correspondant.
	   * 
	   * ---
	   * 
	   * @param myPublicKey : Clé publique du client appelant.
	   * @param command     : Chaîne de commande saisie (ex: {@code -mp 2 Bonjour !}).
	   * @param apps        : Map des utilisateurs connectés indexés par ID.
	   * @param graphe      : Graphe de routage entre les utilisateurs.
	   * @param infoUsers   : Informations sur les messages cachés et l'utilisateur local.
	   * @return Le paquet correspondant à la commande ou {@code null} si la commande est invalide ou incomplète.
	   */
	public static Paquet getPaquet(PublicKeyRSA myPublicKey, String command, Map<Integer, Node> apps, Graphe graphe, InfoUsers infoUsers) {
		Utils.requireNonNulls(myPublicKey, command, apps, graphe, infoUsers);
		//On récupérera la commande sans le '-'.
		try {
			switch(command.charAt(1)) {
				case 'm' -> {
					var secondArg = command.charAt(2);
					//message public
					if(secondArg == 'p') {
						var commands = command.split(" ", 3);
						var vals = Integer.parseInt(commands[1]);
						var node = apps.get(vals);
						if(node == null) {
							logger.info("Error, user not found !");
							return null;
						}
						return new MessagePublic(myPublicKey, node.publicKey(), commands[2]);
					}
					//message privée
					else if(secondArg == 'c') {
						var commands = command.split(" ", 3);
						var vals = Integer.parseInt(commands[1]);
						var node = apps.get(vals);
						if(node == null) {
							logger.info("Error, user not found !");
							return null;
						}
						var destPublicKey = node.publicKey();
						var listToDest = graphe.randomPath(myPublicKey, destPublicKey);
						var idMessage = System.currentTimeMillis();
						infoUsers.addNewHiddenMessegeID(idMessage);
						if(myPublicKey.equals(destPublicKey)) { return Utils.onionRoutingForSecureMessage(myPublicKey, new ArrayList<>(List.of(myPublicKey)), commands[2], idMessage); }
						return Utils.onionRoutingForSecureMessage(myPublicKey, listToDest.reversed(), commands[2], idMessage);
					}
				}
				case 'l' -> {
					//demande de liste des publics keys de chacun des utilisateurs
					return new ListConnected();
				}
				case 'd' -> {
					//demande de déconnexion
					return new Message("Système", "d");						
				}
				case 'r' -> {
					//demande réseau
					return new Message("Système", "r");					
				}
				case 'h' -> {
				    Utils.printHelp();
				    return new Message("Système", "h");
				}
				default -> {
					logger.warning("Error, unreconized command !");
				}
			}
		}//catch les erreurs de formation pour d'éventuelle commande incomplete  
		catch(ArrayIndexOutOfBoundsException | StringIndexOutOfBoundsException arioobe) {
			logger.info("Error command !\ncommand do not have valid argument.\nPlease correct and retry.\n");
			return null;
		} catch(NumberFormatException nfe) {
			logger.info("Error command !\ncommand for message need an integer ! like '-mp 2 Hello there !'");
			return null;
		}
		return null;
	}
}
