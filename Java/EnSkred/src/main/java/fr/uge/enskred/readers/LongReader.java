package fr.uge.enskred.readers;

import java.nio.ByteBuffer;


/**
 * Reader pour la lecture d'un entier long (64 bits) depuis un {@link ByteBuffer}.
 * 
 * <p>Ce lecteur lit progressivement 8 octets (taille d'un long) dans un buffer interne
 * jusqu'à ce que la valeur complète soit disponible. Il gère les états intermédiaires
 * pour permettre une lecture incrémentale typique des applications réseau non bloquantes.</p>
 * 
 * ---
 * 
 * États possibles :
 * <ul>
 *   <li>WAITING : en attente de plus de données.</li>
 *   <li>DONE : lecture terminée, la valeur peut être récupérée via {@code get()}.</li>
 *   <li>ERROR : une erreur s’est produite (ex. appel invalide après lecture).</li>
 * </ul>
 * 
 * ---
 * 
 * Exemple d'utilisation typique :
 * <pre>{@code
 *   LongReader reader = new LongReader();
 *   while (reader.process(buffer) == ProcessStatus.REFILL) {
 *     // Attendre plus de données
 *   }
 *   long value = reader.get();
 * }</pre>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class LongReader implements Reader<Long> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(Long.BYTES); // write-mode
    private long value;

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
        value = internalBuffer.getLong();
        return ProcessStatus.DONE;
    }

    @Override
    public Long get() {
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
        testSingleLongInLargeBuffer();
        testMultipleLongs();
    }

    public static void testOneByteAtATime() {
        var bb = ByteBuffer.allocate(1024);
        bb.putLong(1).flip();

        var longReader = new LongReader();
        var smallBuffer = ByteBuffer.allocate(1);

        while(bb.hasRemaining()) {
            smallBuffer.put(bb.get());
            var status = longReader.process(smallBuffer);
            smallBuffer.clear();  
            if(status == ProcessStatus.DONE) {
                System.out.println("Test One Byte At A Time réussi. Valeur lue: " + longReader.get());
            } else if(status == ProcessStatus.REFILL) {
                System.out.println("REFILL");
            }
        }
    }

    public static void testSingleLongInLargeBuffer() {
        var bb = ByteBuffer.allocate(1024);
        bb.putLong(2);

        var longReader = new LongReader();
        var status = longReader.process(bb);

        if(status == ProcessStatus.DONE) {
            System.out.println("Test Single Long In Large Buffer réussi. Valeur lue: " + longReader.get());
        } else {
            System.out.println("Test échoué. Statut: " + status);
        }
    }

    public static void testMultipleLongs() {
        var bb = ByteBuffer.allocate(1024);
        bb.putLong(30).putLong(31);

        var longReader = new LongReader();
        System.out.println(bb.remaining() + "   - 1");
        var status = longReader.process(bb);
        if(status == ProcessStatus.DONE) {
            System.out.println("Premier entier lu : " + longReader.get());
        }
        longReader.reset();
        System.out.println(bb.remaining() + "   - 2");
        status = longReader.process(bb);
        if(status == ProcessStatus.DONE) {
            System.out.println("Deuxième entier lu : " + longReader.get());
        }
        System.out.println(bb.remaining() + "   - 3");
    }
    
}