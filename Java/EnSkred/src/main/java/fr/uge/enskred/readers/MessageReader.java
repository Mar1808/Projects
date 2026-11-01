package fr.uge.enskred.readers;

import java.nio.ByteBuffer;

import fr.uge.enskred.paquet.Message;



/**
 * Reader pour la lecture d'un {@link Message} depuis un {@link ByteBuffer}.
 * 
 * <p>Ce lecteur décode un message en deux étapes : d'abord la lecture du login de l'utilisateur (via un {@link StringReader}),
 * puis la lecture du texte du message (également via un {@link StringReader}).</p>
 * 
 * <p>Le processus de lecture se fait de manière incrémentale, chaque étape étant gérée indépendamment.</p>
 * 
 * <p>Les états possibles pour cette classe sont les suivants :</p>
 * <ul>
 *   <li>WAITING_LOGIN : en attente des données du login.</li>
 *   <li>WAITING_MESSAGE : en attente des données du message.</li>
 *   <li>DONE : lecture terminée, un {@link Message} peut être récupéré via {@code get()}.</li>
 *   <li>ERROR : une erreur s’est produite, généralement en cas d'appel invalide ou de mauvaise donnée lue.</li>
 * </ul>
 * 
 * ---
 * 
 * Exemple d'utilisation typique :
 * <pre>{@code
 *   MessageReader reader = new MessageReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {//ATTENTE ACTIVE !
 *     // Attendre plus de données
 *   }
 *   Message message = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class MessageReader implements Reader<Message> {
	private enum State {
		DONE, WAITING_LOGIN, WAITING_MESSAGE, ERROR
	}

	private final StringReader stringReader = new StringReader();
	private State state = State.WAITING_LOGIN;
	private Message message;
	private String login;

    @Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		var localStatus = ProcessStatus.ERROR;
		switch(state) {
			case WAITING_LOGIN:
				localStatus = stringReader.process(buffer);
				switch(localStatus) {
					case REFILL: return localStatus;
					case DONE:
						login = stringReader.get();
						stringReader.reset();
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
						message = new Message(login, text);
						state = State.DONE;
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
	public Message get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return message;
	}

	@Override
	public void reset() {
		state = State.WAITING_LOGIN;
		stringReader.reset();
		message = null;
	}    
	
	//MAIN
	
	public static void main(String[] args) {
        testSingleMessageInLargeBuffer();
        testSingleMessageOneByteAtATime();
        testMultipleMessagesInSingleBuffer();
    }

    // Test 1 : Lire un seul message dans un buffer assez grand
    public static void testSingleMessageInLargeBuffer() {
        var message = new Message("login1", "Hello, this is a message.");
        var buffer = message.getWriteModeBuffer();
        var messageReader = new MessageReader();
        var status = messageReader.process(buffer);
        if(status == ProcessStatus.DONE) {
            System.out.println("Test 1 réussi. Message lu: " + messageReader.get());
        } else {
            System.out.println("Test échoué. Statut: " + status);
        }
    }

    // Test 2 : Lire un seul message, en envoyant les octets un par un
    public static void testSingleMessageOneByteAtATime() {
        var message = new Message("login1", "Hello, this is a message.");
        var buffer = message.getWriteModeBuffer().flip();
        var messageReader = new MessageReader();
        var smallBuffer = ByteBuffer.allocate(1);
        while(buffer.hasRemaining()) {
            smallBuffer.put(buffer.get());
            var status = messageReader.process(smallBuffer);
            smallBuffer.clear();
            if(status == ProcessStatus.DONE) {
                System.out.println("Test 2 réussi. Message lu: " + messageReader.get());
                break;
            } else if (status == ProcessStatus.REFILL) {
				System.out.println("REFILL");
            }
        }
    }

    // Test 3 : Lire plusieurs messages dans un seul buffer
    public static void testMultipleMessagesInSingleBuffer() {
        var message1 = new Message("login1", "Hello, this is message 1.");
        var message2 = new Message("login2", "Here comes message 2.");
        var buffer = ByteBuffer.allocate(1024);
        buffer.put(message1.getWriteModeBuffer().flip());
        buffer.put(message2.getWriteModeBuffer().flip());
        
        var messageReader = new MessageReader();
        var status = messageReader.process(buffer);
        if(status == ProcessStatus.DONE) {
            var result = messageReader.get();
            System.out.println("Premier message lu: " + result);
        } else {System.err.println("EEF");}
        
        messageReader.reset();
        status = messageReader.process(buffer);
        if(status == ProcessStatus.DONE) {
            var result = messageReader.get();
            System.out.println("Deuxième message lu: " + result);
        } else {System.err.println("ERR2");}
    }
	
}
