package fr.uge.enskred.readers;

import java.nio.ByteBuffer;

/**
 * IntReader est un {@link Reader} permettant de lire un entier Java 32 bits (type {@link Integer})
 * depuis un {@link ByteBuffer}. Il lit exactement 4 octets et retourne un {@code Integer}.
 * 
 * ---
 * 
 * États internes :
 * <ul>
 *   <li>{@code WAITING} : en attente de données</li>
 *   <li>{@code DONE} : entier complètement lu</li>
 *   <li>{@code ERROR} : une erreur est survenue durant le traitement</li>
 * </ul>
 * 
 * ---
 * 
 * @method process(ByteBuffer buffer) : lit jusqu'à 4 octets depuis {@code buffer} pour former
 * un entier. Peut retourner {@link ProcessStatus#REFILL} tant que les données ne sont pas
 * suffisantes. Une fois l'entier lu, retourne {@link ProcessStatus#DONE}.
 * 
 * @method get() : retourne l’entier lu. Doit être appelé uniquement si {@code process()}
 * a retourné {@code DONE}, sinon une exception {@link IllegalStateException} est levée.
 * 
 * @method reset() : réinitialise l’état du lecteur pour permettre une nouvelle lecture.
 * 
 * ---
 * 
 * @implNote Ce lecteur utilise un buffer interne de 4 octets pour stocker les données reçues.
 * Le {@link ByteBuffer} passé à {@code process()} est temporairement retourné en lecture
 * via {@code flip()} puis remis en écriture avec {@code compact()}, ce qui permet un traitement
 * non destructif du buffer.
 * 
 * ---
 * 
 * Exemple d’utilisation :
 * <pre>
 * var buffer = ByteBuffer.allocate(4).putInt(42).flip();
 * var reader = new IntReader();
 * if (reader.process(buffer) == ProcessStatus.DONE) {
 *     int value = reader.get();
 * }
 * </pre>
 * 
 * ---
 * 
 * @throws IllegalStateException si {@code get()} est appelé alors que l’état n’est pas {@code DONE}
 * 
 * ---
 * 
 * Méthodes de test incluses :
 * <ul>
 *   <li>{@code testOneByteAtATime()} : lit un entier en insérant les octets un par un</li>
 *   <li>{@code testSingleIntInLargeBuffer()} : lecture d’un entier avec un buffer large contenant toutes les données</li>
 *   <li>{@code testMultipleInts()} : lecture de plusieurs entiers successifs dans un même buffer</li>
 * </ul>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class IntReader implements Reader<Integer> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(Integer.BYTES); // write-mode
    private int value;

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if (state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException();
        }
        try {
            buffer.flip();
            if (buffer.remaining() <= internalBuffer.remaining()) {
                internalBuffer.put(buffer);
            } else {
                var oldLimit = buffer.limit();
                buffer.limit(internalBuffer.remaining());
                internalBuffer.put(buffer);
                buffer.limit(oldLimit);
            }
        } finally {
            buffer.compact();
        }
        if (internalBuffer.hasRemaining()) {
            return ProcessStatus.REFILL;
        }
        state = State.DONE;
        internalBuffer.flip();
        value = internalBuffer.getInt();
        return ProcessStatus.DONE;
    }

    @Override
    public Integer get() {
        if (state != State.DONE) {
            throw new IllegalStateException();
        }
        return value;
    }

    @Override
    public void reset() {
        state = State.WAITING;
        internalBuffer.clear();
    }
    
    //MAIN
    
    public static void main(String[] args) {
        testOneByteAtATime();
        testSingleIntInLargeBuffer();
        testMultipleInts();
    }

    public static void testOneByteAtATime() {
        var bb = ByteBuffer.allocate(1024);
        bb.putInt(1).flip();

        var intReader = new IntReader();
        var smallBuffer = ByteBuffer.allocate(1);

        while(bb.hasRemaining()) {
            smallBuffer.put(bb.get());
            var status = intReader.process(smallBuffer);
            smallBuffer.clear();  
            if(status == ProcessStatus.DONE) {
                System.out.println("Test One Byte At A Time réussi. Valeur lue: " + intReader.get());
            } else if(status == ProcessStatus.REFILL) {
                System.out.println("TEst en refill");
            }
        }
    }

    public static void testSingleIntInLargeBuffer() {
        var bb = ByteBuffer.allocate(1024);
        bb.putInt(2);

        var intReader = new IntReader();
        var status = intReader.process(bb);

        if(status == ProcessStatus.DONE) {
            System.out.println("Test Single Int In Large Buffer réussi. Valeur lue: " + intReader.get());
        } else {
            System.out.println("Test échoué. Statut: " + status);
        }
    }

    public static void testMultipleInts() {
        var bb = ByteBuffer.allocate(1024);
        bb.putInt(30).putInt(31);

        var intReader = new IntReader();
        System.out.println(bb.remaining() + "   - 1");
        var status = intReader.process(bb);
        if(status == ProcessStatus.DONE) {
            System.out.println("Premier entier lu : " + intReader.get());
        }
        intReader.reset();
        System.out.println(bb.remaining() + "   - 2");
        status = intReader.process(bb);
        if(status == ProcessStatus.DONE) {
            System.out.println("Deuxième entier lu : " + intReader.get());
        }
        System.out.println(bb.remaining() + "   - 3");
    }
    
}