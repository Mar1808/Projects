package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;



/**
 * Reader pour la lecture d'un payload à partir d'un {@link ByteBuffer}.
 * 
 * <p>Ce lecteur lit un payload en deux étapes :</p>
 * <ul>
 *   <li>La première étape consiste à lire la taille du payload (int) depuis le buffer.</li>
 *   <li>La deuxième étape consiste à lire le contenu du payload sous forme de chaîne de caractères (String), en utilisant cette taille.</li>
 * </ul>
 * 
 * <p>Les étapes de lecture sont les suivantes :</p>
 * <ul>
 *   <li>WAITING_SIZE : en attente de la lecture de la taille du payload.</li>
 *   <li>WAITING_STRING : en attente de la lecture du contenu du payload.</li>
 *   <li>DONE : lecture terminée, un {@link ByteBuffer} contenant le payload peut être récupéré via {@code get()}.</li>
 *   <li>ERROR : une erreur s’est produite, souvent liée à une incohérence dans les données lues ou l'état de l'objet.</li>
 * </ul>
 * 
 * <p>Une fois que le payload a été entièrement extrait du buffer, il est disponible via la méthode {@code get()} sous forme de {@link ByteBuffer} contenant le contenu du payload.</p>
 * 
 * ---
 * 
 * Exemple d'utilisation :
 * <pre>{@code
 *   PayloadReader reader = new PayloadReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {ATTENTE ACTIVE
 *     // Attendre plus de données
 *   }
 *   ByteBuffer payload = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * Tests unitaires :
 * Cette classe inclut des tests pour valider la lecture des payloads, notamment avec des buffers de différentes tailles et des payloads hétérogènes (String, int, double, long, short).
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class PayloadReader implements Reader<ByteBuffer> {
	private enum State {
		DONE, WAITING_SIZE, WAITING_STRING, ERROR
	}

	private final IntReader intReader = new IntReader();
	private State state = State.WAITING_SIZE;
	private ByteBuffer payloadBuffer;
	private ByteBuffer value;

    @Override
	public ProcessStatus process(ByteBuffer buffer) {
		if (state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		switch(state) {
			case WAITING_SIZE:
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
				payloadBuffer = ByteBuffer.allocate(size);
			case WAITING_STRING:
				try {
					buffer.flip();
					if(!processForFillString(buffer, payloadBuffer)) {
						return ProcessStatus.REFILL;
					}
					value = ByteBuffer.allocate(payloadBuffer.flip().remaining());
					value.put(payloadBuffer);
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
	public ByteBuffer get() {
		if (state != State.DONE) {
			throw new IllegalStateException();
		}
		return value;
	}

	@Override
	public void reset() {
		state = State.WAITING_SIZE;
		payloadBuffer.clear();
		intReader.reset();
	}

	//////PRIVATE METHODS -----------
	private boolean processForFillString(ByteBuffer bufferIn, ByteBuffer bufferOut) {
		if(!bufferIn.hasRemaining() || !bufferOut.hasRemaining()) {
			return !bufferOut.hasRemaining();
		}
		else if(bufferOut.remaining() >= bufferIn.remaining()) {
			bufferOut.put(bufferIn);
			return !bufferOut.hasRemaining();
		}
		var oldInLimit = bufferIn.limit();
		bufferIn.limit(bufferIn.position() + bufferOut.remaining());
		bufferOut.put(bufferIn);
		bufferIn.limit(oldInLimit);
		return true;
	}
	
	//MAIN----------
	
    public static void main(String[] args) {
        testSinglePayloadInLargeBuffer();
        testSinglePayloadWithSmallBuffer();
        testMultiplePayloads();
        testMultipleHeterogeneousPayloads();
    }

    // Test 1 : Lire un payload avec un buffer suffisant
    private static void testSinglePayloadInLargeBuffer() {
        System.out.println("TEST 1 : Lecture d'un payload avec un buffer suffisant");
        var message = "Hello, World!";
        var buffer = createPayloadBuffer(message);
        var payloadReader = new PayloadReader();
        var status = payloadReader.process(buffer);

        if(status == ProcessStatus.DONE) {
            var result = payloadReader.get();
            var output = StandardCharsets.UTF_8.decode(result.flip()).toString();
            System.out.println("Test 1 réussi. Payload lu : " + output);
        } else {
            System.out.println("Test 1 échoué. Statut : " + status);
        }
    }

    // Test 2 : Lire un payload avec un buffer trop petit (lecture fragmentée)
    private static void testSinglePayloadWithSmallBuffer() {
        System.out.println("TEST 2 : Lecture d'un payload avec un buffer trop petit");
        var message = "Fragmented payload test!";
        var fullBuffer = createPayloadBuffer(message).flip();
        var payloadReader = new PayloadReader();
        var smallBuffer = ByteBuffer.allocate(1);
        var status = ProcessStatus.REFILL;

        while(fullBuffer.hasRemaining() || status == ProcessStatus.REFILL) {
            smallBuffer.clear();
            while(smallBuffer.hasRemaining() && fullBuffer.hasRemaining()) {
                smallBuffer.put(fullBuffer.get());
            }
            status = payloadReader.process(smallBuffer);
        }

        if(status == ProcessStatus.DONE) {
            var result = payloadReader.get();
            var output = StandardCharsets.UTF_8.decode(result.flip()).toString();
            System.out.println("Test 2 réussi. Payload lu : " + output);
        } else {
            System.out.println("Test 2 échoué. Statut : " + status);
        }
    }

    // Test 3 : Lire plusieurs payloads consécutivement
    private static void testMultiplePayloads() {
        System.out.println("TEST 3 : Lecture de plusieurs payloads consécutifs");
        var msg1 = "First message";
        var msg2 = "Second message";
        var buffer1 = createPayloadBuffer(msg1).flip();
        var buffer2 = createPayloadBuffer(msg2).flip();

        var combinedBuffer = ByteBuffer.allocate(buffer1.remaining() + buffer2.remaining());
        combinedBuffer.put(buffer1).put(buffer2);

        var payloadReader = new PayloadReader();
        var status = ProcessStatus.REFILL;

        while(combinedBuffer.position() != 0 || status == ProcessStatus.REFILL) {
            status = payloadReader.process(combinedBuffer);
            if(status == ProcessStatus.DONE) {
                ByteBuffer result = payloadReader.get();
                String output = StandardCharsets.UTF_8.decode(result.flip()).toString();
                System.out.println("Payload lu : " + output);
                payloadReader.reset();
            }
        }
    }

    private static ByteBuffer createPayloadBuffer(String message) {
        var messageBytes = message.getBytes(StandardCharsets.UTF_8);
        var buffer = ByteBuffer.allocate(Integer.BYTES + messageBytes.length);
        buffer.putInt(messageBytes.length);
        buffer.put(messageBytes);
        return buffer;
    }
    

    // Test : Lire plusieurs types de payloads consécutifs (String, int, double, long, short)
    private static void testMultipleHeterogeneousPayloads() {
        System.out.println("TEST : Lecture de plusieurs payloads hétérogènes");

        var combinedBuffer = ByteBuffer.allocate(1024);
        putPayload(combinedBuffer, "Hello, World!");
        putPayload(combinedBuffer, 42);
        putPayload(combinedBuffer, 3.14159);
        putPayload(combinedBuffer, 9876543210L);
        putPayload(combinedBuffer, (short) 12345);
        var payloadReader = new PayloadReader();
        var status = ProcessStatus.REFILL;

        while(combinedBuffer.position() != 0 || status == ProcessStatus.REFILL) {
            status = payloadReader.process(combinedBuffer);
            if(status == ProcessStatus.DONE) {
                var result = payloadReader.get();
                result.flip();
                printPayload(result);
                payloadReader.reset();
            }
        }
    }

 // Méthode générique pour écrire un payload dans un buffer
    private static void putPayload(ByteBuffer buffer, Object value) {
        switch(value) {
            case String str -> {
                var messageBytes = str.getBytes(StandardCharsets.UTF_8);
                buffer.putInt(messageBytes.length).put(messageBytes);
            }
            case Integer i -> 	{buffer.putInt(Integer.BYTES).putInt(i);}
            case Double d -> 	{buffer.putInt(Double.BYTES).putDouble(d);}
            case Long l -> 		{buffer.putInt(Long.BYTES).putLong(l);}
            case Short s -> 	{buffer.putInt(Short.BYTES).putShort(s);}
            default -> throw new IllegalArgumentException("Type non pris en charge : " + value.getClass().getSimpleName());
        }
    }


    // Identification et affichage des payloads
    private static void printPayload(ByteBuffer buffer) {
        switch (buffer.remaining()) {
            case Integer.BYTES -> System.out.println("Payload lu (int) : " + buffer.getInt());
            case Long.BYTES|Double.BYTES -> System.out.println("Payload lu (long) : " + buffer.getLong());
            case Short.BYTES -> System.out.println("Payload lu (short) : " + buffer.getShort());
            default -> System.out.println("Payload lu (String) : " + StandardCharsets.UTF_8.decode(buffer));
        }
    }
	

}
