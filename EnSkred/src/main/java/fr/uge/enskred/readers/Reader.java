package fr.uge.enskred.readers;

import java.nio.ByteBuffer;


/**
 * L'interface {@code Reader} définit un contrat pour des lecteurs génériques de données à partir d'un {@code ByteBuffer}.
 * Elle permet de traiter un flux de données sous forme de {@code ByteBuffer}, de récupérer les données lues, 
 * et de réinitialiser l'état du lecteur pour un nouvel usage.
 * 
 * Les classes qui implémentent cette interface peuvent gérer différents types de données et processus de lecture,
 * comme le déchiffrement de messages ou la lecture de fichiers. 
 * L'interface fournit des mécanismes de gestion de l'état du processus de lecture via le statut de traitement.
 * 
 * Le processus de lecture est structuré en plusieurs étapes :
 * 1. Le traitement des données d'entrée se fait via la méthode {@code process(ByteBuffer)}.
 * 2. Une fois le traitement terminé, la méthode {@code get()} permet de récupérer les données lues sous forme de l'objet de type {@code T}.
 * 3. La méthode {@code reset()} permet de réinitialiser l'état du lecteur pour une nouvelle session de lecture.
 * 
 * @param <T> Le type des données lues par le lecteur. Ce type doit être compatible avec l'implémentation spécifique.
 *
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public sealed interface Reader<T> permits
	// READER 
	StringReader, SocketAddressReader, RSAReader, PublicKeyReader,
	PayloadReader, PassForwardReader, NodeReader, NewNodeReader,
	MessageToSecureReader, MessageReader, MessagePublicReader,
	LongReader, ListReader, JoinResponseReader, IntReader,
	ConnexionReader, ByteReader, BroadcastReader,
	// LES 3 GRANDS READER PRINCIPALES
	PrimaryReader, PrimaryPayloadReader, PrimaryInstructionReader
{

    /**
     * L'énumération {@code ProcessStatus} définit les différents statuts possibles du processus de lecture.
     */
    enum ProcessStatus {
        DONE,    // Indique que le processus de lecture est terminé.
        REFILL,  // Indique que le buffer doit être à nouveau rempli pour continuer la lecture.
        ERROR    // Indique qu'une erreur est survenue durant la lecture.
    }

    /**
     * Traite les données d'entrée contenues dans le {@code ByteBuffer}. 
     * La méthode peut lire ou analyser les données selon l'implémentation du lecteur.
     * 
     * @param bb Le buffer contenant les données à traiter.
     * @return Le statut du processus de traitement (DONE, REFILL, ERROR).
     */
    ProcessStatus process(ByteBuffer bb);

    /**
     * Récupère l'objet de type {@code T} contenant les données lues après que le processus de lecture soit terminé.
     * 
     * @return Les données lues sous forme d'un objet de type {@code T}.
     * @throws IllegalStateException si le processus de lecture n'est pas encore terminé ou si une erreur est survenue.
     */
    T get();

    /**
     * Réinitialise l'état du lecteur, permettant de le réutiliser pour un nouveau traitement des données.
     * Cette méthode réinitialise toutes les variables internes et prépare le lecteur pour une nouvelle opération.
     */
    void reset();

}
