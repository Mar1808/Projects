package fr.uge.enskred.readers;

import java.nio.ByteBuffer;


/**
 * ByteReader est un {@link Reader} spécialisé dans la lecture d’un unique octet (byte) à partir
 * d’un {@link ByteBuffer}. Il fonctionne par étapes et gère les cas où le buffer ne contient pas
 * immédiatement assez de données.
 * ---
 * États internes :
 * <ul>
 *   <li>{@code WAITING} : en attente d’un octet à lire</li>
 *   <li>{@code DONE} : un octet a été lu avec succès</li>
 *   <li>{@code ERROR} : une erreur est survenue (par exemple un appel incorrect de {@code get()})</li>
 * </ul>
 * 
 * ---
 * Méthodes principales :
 * @method process(ByteBuffer buffer) : lit jusqu'à un octet depuis le buffer source. Retourne
 * {@link ProcessStatus#REFILL} si l’octet n’est pas encore totalement disponible,
 * {@link ProcessStatus#DONE} si l’octet a été lu avec succès.
 * 
 * @method get() : retourne l’octet lu. Doit être appelé uniquement lorsque le {@code process()}
 * a retourné {@code DONE}, sinon une exception {@link IllegalStateException} est levée.
 * 
 * @method reset() : remet le lecteur dans l’état initial pour permettre une nouvelle lecture.
 * ---
 * 
 * @implNote Le {@code ByteBuffer} passé en paramètre est manipulé via les méthodes {@code flip()} et
 * {@code compact()} pour permettre un traitement non destructif. L’octet est stocké dans un
 * {@code internalBuffer} de taille 1.
 * ---
 * Exemple d’utilisation :
 * <pre>
 * ByteBuffer buffer = ByteBuffer.allocate(1);
 * buffer.put((byte) 42).flip();
 * var reader = new ByteReader();
 * if (reader.process(buffer) == ProcessStatus.DONE) {
 *     byte result = reader.get();
 * }
 * </pre>
 * 
 * ---
 * 
 * @throws IllegalStateException si {@code get()} est appelé avant la fin de la lecture (état non DONE)
 * ---
 * Méthodes de test incluses :
 * <ul>
 *   <li>{@code testSingleByte()} : lecture d’un seul octet depuis un buffer contenant plus de données</li>
 *   <li>{@code testByteByByte()} : lecture successive de 3 octets, chacun via un buffer d’un seul octet</li>
 * </ul>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */

public final class ByteReader implements Reader<Byte> {

    private enum State {
        DONE, WAITING, ERROR
    }

    private State state = State.WAITING;
    private final ByteBuffer internalBuffer = ByteBuffer.allocate(Byte.BYTES); // 1 octet
    private byte value;

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
        value = internalBuffer.get();
        return ProcessStatus.DONE;
    }

    @Override
    public Byte get() {
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

    //MAIN POUR TESTS
    public static void main(String[] args) {
        testSingleByte();
        testByteByByte();
    }

    public static void testSingleByte() {
        var bb = ByteBuffer.allocate(1 + 8);
        bb.put((byte) 42).putLong(0);

        var byteReader = new ByteReader();
        var status = byteReader.process(bb);
        if (status == ProcessStatus.DONE) {
            System.out.println("Byte lu : " + byteReader.get());
        } else {
            System.out.println("Statut : " + status);
        }
    }

    public static void testByteByByte() {
        var bb = ByteBuffer.allocate(3);
        bb.put((byte) 13).put((byte) 37).put((byte) 77).flip();

        var byteReader = new ByteReader();
        for (int i = 0; i < 3; i++) {
            var oneByte = ByteBuffer.allocate(1);
            oneByte.put(bb.get());
            var status = byteReader.process(oneByte);
            if (status == ProcessStatus.DONE) {
                System.out.println("Byte " + (i + 1) + " lu : " + byteReader.get());
            } else {
                System.out.println("Byte " + (i + 1) + " en attente !");
            }
            byteReader.reset();
        }
    }
}
