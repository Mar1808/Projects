package fr.uge.enskred.readers;

import java.nio.ByteBuffer;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.paquet.PassForward;
import fr.uge.enskred.paquet.SecureMessage;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;


/**
 * Reader pour la lecture d'un {@link PassForward} depuis un {@link ByteBuffer}.
 * 
 * <p>Ce lecteur est conçu pour traiter les éléments suivants, dans l'ordre :</p>
 * <ul>
 *   <li>Clé publique RSA via {@link PublicKeyReader}</li>
 *   <li>OpCode du message sécurisé via {@link ByteReader}</li>
 *   <li>Message sécurisé encodé via {@link RSAReader}</li>
 * </ul>
 * 
 * <p>Les étapes de lecture sont les suivantes :</p>
 * <ul>
 *   <li>WAITING_KEY_RSA : en attente de la clé publique RSA.</li>
 *   <li>WAITING_OPCODE_SECURE_MESSAGE : en attente de l'OpCode indiquant un message sécurisé.</li>
 *   <li>WAITING_ENCODED_SECURE_MESSAGE : en attente du message sécurisé encodé.</li>
 *   <li>DONE : lecture terminée, un {@link PassForward} peut être récupéré via {@code get()}.</li>
 *   <li>ERROR : une erreur s’est produite, souvent liée à une incohérence de données lues ou d'états.</li>
 * </ul>
 * 
 * <p>Une fois que toutes les informations nécessaires ont été extraites du buffer, un objet {@link PassForward} est disponible via {@code get()}.</p>
 * 
 * ---
 * 
 * Exemple d'utilisation :
 * <pre>{@code
 *   PassForwardReader reader = new PassForwardReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {//ATTENTE ACTIVE
 *     // Attendre plus de données
 *   }
 *   PassForward passForward = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class PassForwardReader implements Reader<PassForward> {
	private final PublicKeyReader publicKeyReader = new PublicKeyReader();
	private final ByteReader byteReader = new ByteReader();
	private final RSAReader rsaReader = new RSAReader();
	private State state = State.WAITING_KEY_RSA;
	private SecureMessage secureMessage;
	private ByteBuffer encodedBuffer;
	private PassForward passForward;
	private PublicKeyRSA publicKey;
	
	private enum State {
		WAITING_KEY_RSA, WAITING_OPCODE_SECURE_MESSAGE, WAITING_ENCODED_SECURE_MESSAGE, DONE, ERROR 
	}

	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException("State: " + state);
		}
		switch(state) {
			case WAITING_KEY_RSA:
				switch(publicKeyReader.process(buffer)) {
					case REFILL: return ProcessStatus.REFILL;
					case DONE:
						publicKey = publicKeyReader.get();
						state = State.WAITING_OPCODE_SECURE_MESSAGE;
						break;
					default:	
						state = State.ERROR; 
						return ProcessStatus.ERROR;
				}
			case WAITING_OPCODE_SECURE_MESSAGE:
				switch(byteReader.process(buffer)) {
					case REFILL: return ProcessStatus.REFILL;
					case DONE:
						var opCode = byteReader.get();
						if(OpCode.SECURE_MESSAGE.getCode() != opCode) { return ProcessStatus.ERROR; }
						state = State.WAITING_ENCODED_SECURE_MESSAGE;
						break;
					default:	
						state = State.ERROR; 
						return ProcessStatus.ERROR;
				}
			case WAITING_ENCODED_SECURE_MESSAGE:
				switch(rsaReader.process(buffer)) {
					case REFILL: return ProcessStatus.REFILL;
					case DONE:
						encodedBuffer = rsaReader.get();
						secureMessage = new SecureMessage(publicKey, null, encodedBuffer.flip());
						passForward = new PassForward(publicKey, secureMessage);
						state = State.DONE;
						return ProcessStatus.DONE;
					default:	
						state = State.ERROR; 
						return ProcessStatus.ERROR;
				}
			default:
				state = State.ERROR;
				return ProcessStatus.ERROR;
		}
	}

	@Override
	public PassForward get() {
		if(state != State.DONE) {
			throw new IllegalStateException("State is not DONE");
		}
		return passForward;
	}


	@Override
	public void reset() {
		publicKey = null;
		rsaReader.reset();
		passForward = null;
		byteReader.reset();
		encodedBuffer = null;
		secureMessage = null;
		publicKeyReader.reset();
		state = State.WAITING_KEY_RSA;
	}


}