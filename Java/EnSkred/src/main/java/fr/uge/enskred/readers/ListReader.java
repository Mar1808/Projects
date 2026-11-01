package fr.uge.enskred.readers;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import fr.uge.enskred.utils.Utils;



/**
 * Reader générique permettant de lire une liste d’éléments de type {@code T} depuis un ByteBuffer.
 * ---
 * Cette classe utilise un {@link Reader<T>} pour lire chaque élément,
 * et un {@link IntReader} pour déterminer la taille initiale de la liste.
 * Elle fonctionne par étapes : lecture de la taille, puis lecture des éléments.
 *
 * @param <T> : Le type des éléments à lire
 */
public final class ListReader<T> implements Reader<List<T>> {
    private enum State {
        WAITING_LIST_SIZE, WAITING_ELEMENT, DONE, ERROR
    }
    
    private final Reader<T> elementReader;
    private final IntReader intReader = new IntReader();
    private final List<T> list = new ArrayList<>();
    private int size;
    private State state = State.WAITING_LIST_SIZE;
    private ProcessStatus localStatus;

    public ListReader(Reader<T> elementReader) {
        this.elementReader = Objects.requireNonNull(elementReader);
    }

    @Override
    public ProcessStatus process(ByteBuffer buffer) {
        if(state == State.DONE || state == State.ERROR) {
            throw new IllegalStateException("State: " + state);
        }
        
        switch(state) {
            case WAITING_LIST_SIZE:
                localStatus = intReader.process(buffer);
                if(localStatus == ProcessStatus.REFILL) {
                    return ProcessStatus.REFILL;
                }
                size = intReader.get();
                intReader.reset();
                state = State.WAITING_ELEMENT;
            case WAITING_ELEMENT:
                for(var i = list.size(); i < size; i++) {
                    localStatus = elementReader.process(buffer);
                    if(localStatus == ProcessStatus.REFILL) {
                        return ProcessStatus.REFILL;
                    }
                    list.add(elementReader.get());
                    elementReader.reset();
                }
                
                state = State.DONE;
                return ProcessStatus.DONE;
            
            default:
                state = State.ERROR;
                return ProcessStatus.ERROR;
        }
    }

    @Override
    public List<T> get() {
        if (state != State.DONE) {
            throw new IllegalStateException("State is not DONE");
        }
        return new ArrayList<>(list);
    }

    @Override
    public void reset() {
        state = State.WAITING_LIST_SIZE;
        intReader.reset();
        elementReader.reset();
        Utils.safeClear(list);
    }
    
    
    //MAIN -----------
    public static void main(String[] args) {
        testListOfIntsInLargeBuffer();
        testListOfIntsWithSmallBuffer();
        testMultipleListsOfInts();
    }

    // Test 1 : Lire une liste d'entiers avec un buffer suffisant
    private static void testListOfIntsInLargeBuffer() {
        System.out.println("TEST 1 : Lecture d'une liste d'entiers avec un buffer suffisant");

        var ints = List.of(10, 20, 30, 40);
        var buffer = ByteBuffer.allocate(1024);
        
        buffer.putInt(ints.size());
        ints.forEach(buffer::putInt);

        var listReader = new ListReader<>(new IntReader());
        var status = listReader.process(buffer);

        if(status == ProcessStatus.DONE) {
            var result = listReader.get();
            System.out.println("Test 1 réussi. Liste lue : " + result);
        } else {
            System.out.println("Test 1 échoué. Statut : " + status);
        }
    }

    // Test 2 : Lire une liste d'entiers avec un buffer trop petit (lecture en plusieurs étapes)
    private static void testListOfIntsWithSmallBuffer() {
        System.out.println("TEST 2 : Lecture d'une liste d'entiers avec un buffer trop petit");

        var ints = List.of(5, 15, 25, 35);
        var fullBuffer = ByteBuffer.allocate(1024);
        fullBuffer.putInt(ints.size());
        ints.forEach(fullBuffer::putInt);
        fullBuffer.flip();

        var listReader = new ListReader<>(new IntReader());
        var smallBuffer = ByteBuffer.allocate(1);
        var status = ProcessStatus.REFILL;

        while(fullBuffer.hasRemaining() || status == ProcessStatus.REFILL) {
            smallBuffer.clear();
            while(smallBuffer.hasRemaining() && fullBuffer.hasRemaining()) {
                smallBuffer.put(fullBuffer.get());
            }
            status = listReader.process(smallBuffer);
        }

        if(status == ProcessStatus.DONE) {
            var result = listReader.get();
            System.out.println("Test 2 réussi. Liste lue : " + result);
        } else {
            System.out.println("Test 2 échoué. Statut : " + status);
        }
    }

    // Test 3 : Lire plusieurs listes d'entiers dans un même buffer
    private static void testMultipleListsOfInts() {
        System.out.println("TEST 3 : Lecture de plusieurs listes d'entiers consécutives");

        var list1 = List.of(1, 2, 3);
        var list2 = List.of(4, 5, 6, 7);

        var buffer1 = ByteBuffer.allocate(1024);
        buffer1.putInt(list1.size());
        list1.forEach(buffer1::putInt);
        buffer1.flip();

        var buffer2 = ByteBuffer.allocate(1024);
        buffer2.putInt(list2.size());
        list2.forEach(buffer2::putInt);
        buffer2.flip();

        var buffer = ByteBuffer.allocate(buffer1.remaining() + buffer2.remaining());
        buffer.put(buffer1).put(buffer2);

        var listReader = new ListReader<>(new IntReader());
        var status = ProcessStatus.REFILL;

        while(buffer.position() != 0 || status == ProcessStatus.REFILL) {
            status = listReader.process(buffer);
            if(status == ProcessStatus.DONE) {
                var result = listReader.get();
                System.out.println("Liste lue : " + result);
                listReader.reset();
            }
        }
    }
    
    
//    public static void main(String[] args) {
//        try {
//            var keyPair1 = UGEncrypt.KeyPairRSA.generate();
//            var publicKey1 = keyPair1.publicKey();
//            var keyPair2 = UGEncrypt.KeyPairRSA.generate();
//            var publicKey2 = keyPair2.publicKey();
//            var keyPair3 = UGEncrypt.KeyPairRSA.generate();
//            var publicKey3 = keyPair3.publicKey();
//            var keyPair4 = UGEncrypt.KeyPairRSA.generate();
//            var publicKey4 = keyPair4.publicKey();
//
//            testListOfConnexionsInLargeBuffer(publicKey1, publicKey2, publicKey3, publicKey4);
//            testListOfConnexionsWithSmallBuffer(publicKey1, publicKey2, publicKey3, publicKey4);
//            testMultipleListsOfConnexions(publicKey1, publicKey2, publicKey3, publicKey4);
//        } catch (NoSuchAlgorithmException e) {
//            System.err.println("Erreur lors de la génération des clés RSA.");
//        }
//    }
//
//    // Test 1 : Lire une liste de Connexions avec un buffer suffisant
//    private static void testListOfConnexionsInLargeBuffer(PublicKeyRSA... publicKeys) {
//        System.out.println("TEST 1 : Lecture d'une liste de Connexions avec un buffer suffisant");
//
//        var connexions = List.of(
//            new Connexion(publicKeys[0], publicKeys[1]),
//            new Connexion(publicKeys[2], publicKeys[3])
//        );
//
//        var buffer = ByteBuffer.allocate(4096);
//        buffer.putInt(connexions.size()); // Ajout de la taille de la liste
//        connexions.forEach(c -> buffer.put(c.getWriteWriteBuffer().flip()));
//
//        var listReader = new ListReader<>(new ConnexionReader());
//        var status = listReader.process(buffer);
//
//        if(status == ProcessStatus.DONE) {
//            var result = listReader.get();
//            System.out.println("Test 1 réussi. Liste lue : " + result);
//        } else {
//            System.out.println("Test 1 échoué. Statut : " + status);
//        }
//    }
//
//    // Test 2 : Lire une liste de Connexions avec un buffer trop petit (lecture en plusieurs étapes)
//    private static void testListOfConnexionsWithSmallBuffer(PublicKeyRSA... publicKeys) {
//        System.out.println("TEST 2 : Lecture d'une liste de Connexions avec un buffer trop petit");
//
//        var connexions = List.of(
//            new Connexion(publicKeys[0], publicKeys[1]),
//            new Connexion(publicKeys[2], publicKeys[3])
//        );
//
//        var fullBuffer = ByteBuffer.allocate(4096);
//        fullBuffer.putInt(connexions.size());
//        connexions.forEach(c -> fullBuffer.put(c.getWriteWriteBuffer().flip()));
//        fullBuffer.flip();
//
//        var listReader = new ListReader<>(new ConnexionReader());
//        var smallBuffer = ByteBuffer.allocate(1);
//        var status = ProcessStatus.REFILL;
//
//        while(fullBuffer.hasRemaining() || status == ProcessStatus.REFILL) {
//            smallBuffer.clear().put(fullBuffer.get());
//            status = listReader.process(smallBuffer);
//            smallBuffer.clear();
//        }
//
//        if(status == ProcessStatus.DONE) {
//            var result = listReader.get();
//            System.out.println("Test 2 réussi. Liste lue : " + result);
//        } else {
//            System.out.println("Test 2 échoué. Statut : " + status);
//        }
//    }
//
//    // Test 3 : Lire plusieurs listes de Connexions dans un même buffer
//    private static void testMultipleListsOfConnexions(PublicKeyRSA... publicKeys) {
//        System.out.println("TEST 3 : Lecture de plusieurs listes de Connexions consécutives");
//
//        var connexions1 = List.of(new Connexion(publicKeys[0], publicKeys[1]));
//        var connexions2 = List.of(new Connexion(publicKeys[2], publicKeys[3]));
//
//        var buffer1 = ByteBuffer.allocate(4096);
//        buffer1.putInt(connexions1.size());
//        connexions1.forEach(c -> buffer1.put(c.getWriteWriteBuffer().flip()));
//        buffer1.flip();
//
//        var buffer2 = ByteBuffer.allocate(4096);
//        buffer2.putInt(connexions2.size());
//        connexions2.forEach(c -> buffer2.put(c.getWriteWriteBuffer().flip()));
//        buffer2.flip();
//
//        var buffer = ByteBuffer.allocate(buffer1.remaining() + buffer2.remaining());
//        buffer.put(buffer1).put(buffer2);
//
//        var listReader = new ListReader<>(new ConnexionReader());
//        var status = ProcessStatus.REFILL;
//
//        while(status == ProcessStatus.REFILL || buffer.hasRemaining()) {
//            status = listReader.process(buffer);
//            if(status == ProcessStatus.DONE) {
//                var result = listReader.get();
//                System.out.println("Liste lue : " + result);
//                listReader.reset();
//            }
//        }
//    }
    
}
