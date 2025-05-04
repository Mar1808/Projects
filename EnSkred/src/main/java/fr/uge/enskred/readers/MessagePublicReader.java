package fr.uge.enskred.readers;

import java.nio.ByteBuffer;

import fr.uge.enskred.paquet.Connexion;
import fr.uge.enskred.paquet.MessagePublic;



/**
 * Reader pour la lecture d'un {@link MessagePublic} depuis un {@link ByteBuffer}.
 * 
 * <p>Ce lecteur décode un message public en deux étapes : d'abord la lecture des informations de connexion
 * (via un {@link ConnexionReader}), puis la lecture du message sous forme de chaîne de caractères (via un {@link StringReader}).</p>
 * 
 * <p>Le processus de lecture s'effectue de manière incrémentale, chaque étape étant gérée séparément.</p>
 * 
 * ---
 * 
 * États possibles :
 * <ul>
 *   <li>WAITING_PK : en attente de données pour la connexion (clé publique du destinataire et de l'expéditeur).</li>
 *   <li>WAITING_MESSAGE : en attente du message sous forme de chaîne de caractères.</li>
 *   <li>DONE : lecture terminée, un {@link MessagePublic} peut être récupéré via {@code get()}.</li>
 *   <li>ERROR : une erreur s’est produite, généralement un appel invalide ou une mauvaise donnée lue.</li>
 * </ul>
 * 
 * ---
 * 
 * Exemple d'utilisation typique :
 * <pre>{@code
 *   MessagePublicReader reader = new MessagePublicReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {//...
 *     // Attendre plus de données
 *   }
 *   MessagePublic message = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class MessagePublicReader implements Reader<MessagePublic> {
	
	private enum State {
		DONE, WAITING_PK, WAITING_MESSAGE, ERROR
	}

	private final ConnexionReader connexionReader = new ConnexionReader();
	private final StringReader stringReader = new StringReader();
	private State state = State.WAITING_PK;
	private MessagePublic messagePublic;
	private Connexion connexion;

    @Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		var localStatus = ProcessStatus.ERROR;
		switch(state) {
			case WAITING_PK:
				localStatus = connexionReader.process(buffer);
				switch(localStatus) {
					case REFILL: return localStatus;
					case DONE:
						connexion = connexionReader.get();
						connexionReader.reset();
						state = State.WAITING_MESSAGE;
						break;
					default:	
						state = State.ERROR; 
						return localStatus;
				}
			case WAITING_MESSAGE:
				localStatus = stringReader.process(buffer);
				switch(localStatus) {
					case REFILL: return localStatus;
					case DONE:
                        var text = stringReader.get();
						stringReader.reset();
						state = State.DONE;
						messagePublic = new MessagePublic(connexion.publicKeySender(), connexion.publicKeyReceiver(), text);
						return ProcessStatus.DONE;
					default:	
						state = State.ERROR; 
						return localStatus;
			}
			default:
				state = State.ERROR;
				return ProcessStatus.ERROR;
		}
	}

	@Override
	public MessagePublic get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		
		return messagePublic;
	}

	@Override
	public void reset() {
		state = State.WAITING_PK;
		connexionReader.reset();
		stringReader.reset();
		messagePublic = null;
	}    

}
