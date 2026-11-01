package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.paquet.Instruction;

/**
 * Reader pour la lecture d'instructions primaires à partir d'un {@link ByteBuffer}.
 * 
 * <p>Cette classe permet de lire et décoder des instructions primaires envoyées dans un paquet, en fonction de leur opcode. Elle identifie d'abord l'opcode de l'instruction, puis lit le payload associé selon le type d'opcode.</p>
 * 
 * <p>Le traitement se déroule en plusieurs étapes, avec les états suivants :</p>
 * <ul>
 *   <li>WAITING_OPCODE : en attente de la lecture de l'opcode (un byte) indiquant le type de l'instruction.</li>
 *   <li>WAITING_INSTRUCTION : en attente de la lecture du payload associé à l'instruction en fonction de l'opcode.</li>
 *   <li>DONE : l'instruction a été complètement lue et est disponible via {@code get()}.</li>
 *   <li>ERROR : une erreur s’est produite pendant la lecture de l'instruction.</li>
 * </ul>
 * 
 * <p>Lorsque l'instruction est lue et validée, elle peut être récupérée via la méthode {@code get()} sous forme d'un objet {@link Instruction} correspondant au type d'instruction décodé.</p>
 * 
 * ---
 * 
 * Exemple d'utilisation :
 * <pre>{@code
 *   PrimaryInstructionReader reader = new PrimaryInstructionReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {
 *     // Attendre plus de données
 *   }
 *   Instruction instruction = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * Tests unitaires :
 * Cette classe contient des tests permettant de valider la lecture des instructions avec différents opcodes (par exemple, `MESSAGE`, `PASS_FORWARD`).
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class PrimaryInstructionReader implements Reader<Instruction> {
	private enum State {
		DONE, WAITING_OPCODE, WAITING_INSTRUCTION, ERROR
	}
	private static final Logger logger = Logger.getLogger(PrimaryInstructionReader.class.getName());
	private final MessageToSecureReader messageToSecureReader = new MessageToSecureReader();
	private final PassForwardReader passForwardReader = new PassForwardReader();
	private final ByteReader byteReader = new ByteReader();
	private State state = State.WAITING_OPCODE;
	private Instruction instruction;
	private OpCode opCode;
	private ProcessStatus localStatus = ProcessStatus.REFILL;
	
	public PrimaryInstructionReader(Level level) {
		logger.setLevel(level == null ? Level.SEVERE : level);
	}
	
	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch(state) {
			//On va récuperer l'opcode du paquet !
			case WAITING_OPCODE:
				localStatus = byteReader.process(buffer);
				if(localStatus != ProcessStatus.DONE) {
					return localStatus;
				}
				opCode = OpCode.intToOpCode(Byte.toUnsignedInt(byteReader.get()));
				state = State.WAITING_INSTRUCTION;
			case WAITING_INSTRUCTION:
				switch(opCode) {
					case MESSAGE -> {
						instruction = readMessage(buffer);
					}
					case PASS_FORWARD -> {
						instruction = readPassForward(buffer);
					}
					default -> { logger.info("Error with waitingPayload"); }
				}
	
				if(localStatus != ProcessStatus.DONE || null == instruction) {
					return localStatus;
				}
				state = State.DONE;
				return ProcessStatus.DONE;
			default:
				state = State.ERROR;return ProcessStatus.ERROR;
		}
	}

	@Override
	public Instruction get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return instruction;
	}

	@Override
	public void reset() {
		localStatus = ProcessStatus.REFILL;
		state = State.WAITING_OPCODE;
		opCode = OpCode.NO_STATE;
		instruction = null;
		byteReader.reset();
		passForwardReader.reset();
		messageToSecureReader.reset();
	}

	
	//PRIVATE METHODS
	/**
	 * Méthode pour les payload NewNode
	 * ---
	 * @param buffer
	 * @return Renvoie un PreJoin
	 */
	private Instruction readMessage(ByteBuffer buffer) {
		localStatus = messageToSecureReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return messageToSecureReader.get(); }
			case ERROR -> 	{ logger.info("Error with readNewNode"); }
		}
		//tmp
		return null;
	}	
	
	/**
	 * Méthode qui réalise la lecture d'un passForward dans le buffer
	 * ---
	 * @param buffer: bufferIn
	 * @return Instruction de type PassForward
	 */
	private Instruction readPassForward(ByteBuffer buffer) {
		localStatus = passForwardReader.process(buffer);
		switch(localStatus) {
			case REFILL -> 	{/*REFILL*/}
			case DONE -> 	{ return passForwardReader.get(); }
			case ERROR -> 	{ logger.info("Error with readNewConnexion"); }
		}
		//tmp
		return null;
	}
	
}
