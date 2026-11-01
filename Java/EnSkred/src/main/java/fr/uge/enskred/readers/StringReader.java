package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;


/**
 * StringReader est un Reader générique permettant de lire une chaîne de caractères
 * encodée en UTF-8 depuis un ByteBuffer selon un protocole à deux étapes :
 * <ul>
 *   <li>Lecture d'un entier représentant la taille (en octets) de la chaîne</li>
 *   <li>Lecture des octets correspondant à cette chaîne</li>
 * </ul>
 * 
 * L'état interne permet de suivre la progression du traitement, qui peut nécessiter
 * plusieurs appels à {@code process(ByteBuffer)} si le buffer ne contient pas
 * immédiatement toutes les données nécessaires.
 * 
 * ---
 * 
 * Exemple de format attendu dans le ByteBuffer :
 * <pre>
 * [4 octets (int) = taille] [n octets (string) = chaîne UTF-8]
 * </pre>
 * 
 * ---
 * 
 * États possibles :
 * <ul>
 *   <li>{@code WAITING_SIZE} : en attente de lecture de la taille</li>
 *   <li>{@code WAITING_STRING} : en attente de lecture du contenu UTF-8</li>
 *   <li>{@code DONE} : la chaîne a été entièrement lue et est accessible via {@code get()}</li>
 *   <li>{@code ERROR} : une erreur est survenue (ex : taille négative)</li>
 * </ul>
 * 
 * ---
 * 
 * Méthodes principales :
 * @method process(ByteBuffer) : traite le buffer et retourne un statut de progression.
 * @method get() : retourne la chaîne lue si le traitement est terminé.
 * @method reset() : réinitialise l'état du reader pour réutilisation.
 * 
 * ---
 * 
 * @implNote Utilise {@link IntReader} pour lire la taille, et un ByteBuffer alloué dynamiquement
 * pour stocker les octets de la chaîne. Le buffer source est retourné en mode write à la fin du traitement.
 * 
 * @author (Marwane KAOUANE)
 * @author (Massiouane MAIBECHE)
 */
public final class StringReader implements Reader<String> {
	private enum State {
		DONE, WAITING_SIZE, WAITING_STRING, ERROR
	};

	private final static Charset UTF8 = StandardCharsets.UTF_8;
	private final IntReader intReader = new IntReader();
	private ByteBuffer stringBuffer;// = ByteBuffer.allocateDirect(BUFFER_SIZE); 		// write-mode
	private State state = State.WAITING_SIZE;
	private String value;

    @Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch(state) {
			//on va récuperer la taille de la chaine
			case WAITING_SIZE:
				//Si on try, on fait un compact avant et flip après
				var situation = intReader.process(buffer);
				if(situation != ProcessStatus.DONE) {
					return situation;
				}
                var size = intReader.get();
				if(size < 0) {
					state = State.ERROR;
					return ProcessStatus.ERROR;
				}
				state = State.WAITING_STRING;
				//stringBuffer.limit(size);
				stringBuffer = ByteBuffer.allocate(size);
				//on va récuperer la chaine à partir de la taille
			case WAITING_STRING:
				try {
					buffer.flip();
					if(!processForFillString(buffer, stringBuffer)) {
						return ProcessStatus.REFILL;
					}
					value = UTF8.decode(stringBuffer.flip()).toString();
					state = State.DONE;
					return ProcessStatus.DONE;
				} finally {
					buffer.compact();
				}
			default:
				state = State.ERROR;return ProcessStatus.ERROR;
		}		
	}

	@Override
	public String get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return value;
	}

	@Override
	public void reset() {
		state = State.WAITING_SIZE;
		intReader.reset();
		stringBuffer = null;
	}

	//////PRIVATE METHODS -----------
	private boolean processForFillString(ByteBuffer bufferIn, ByteBuffer bufferOut) {
		//Si les deux buffer sont plein on ft rien
		if(!bufferIn.hasRemaining() || !bufferOut.hasRemaining()) {
			return !bufferOut.hasRemaining();
		}
		//sinon, si le buffer dest autant de place que la source on put tt
		else if(bufferOut.remaining() >= bufferIn.remaining()) {
			bufferOut.put(bufferIn);
			return !bufferOut.hasRemaining();
		} 
		//sinon on joue avec la limit
		var oldInLimit = bufferIn.limit();
		bufferIn.limit(bufferIn.position() + bufferOut.remaining());
		bufferOut.put(bufferIn);
		bufferIn.limit(oldInLimit);
		return true;
	}

}

