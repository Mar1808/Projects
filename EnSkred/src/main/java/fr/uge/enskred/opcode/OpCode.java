package fr.uge.enskred.opcode;

import java.util.Arrays;


/**
 * Représente l'ensemble des codes d'opérations (OpCode) utilisés dans le protocole EnSkred.
 * <p>
 * Chaque {@code OpCode} est associé à une valeur unique de type {@code byte}, utilisée pour identifier
 * les types de messages ou instructions échangés dans le réseau.
 * </p>
 * 
 * <ul>
 *   <li><strong>Opérations de connexion :</strong> {@code PRE_JOIN}, {@code SECOND_JOIN}, {@code JOIN_RESPONSE}, etc.</li>
 *   <li><strong>Déconnexion :</strong> {@code LEAVE_NETWORK_ASK}, {@code LEAVE_NETWORK_CONFIRM}, etc.</li>
 *   <li><strong>Messages :</strong> {@code OPEN_MESSAGE}, {@code SECURE_MESSAGE}, {@code MESSAGE}, {@code STOP}, etc.</li>
 *   <li><strong>Internes et système :</strong> {@code SYSTEME}, {@code NO_STATE}</li>
 * </ul>
 */
public enum OpCode {
    //OPCODE
    NO_STATE((byte) 0),
    BROADCAST((byte) 1),
    PRE_JOIN((byte) 2),
    SECOND_JOIN((byte) 20),
    CHALLENGE_PUBLIC_KEY((byte) 3),
    RESPONSE_CHALLENGE((byte) 4),
    CHALLENGE_OK((byte) 30),
    JOIN_RESPONSE((byte) 5),
    LEAVE_NETWORK_ASK((byte) 6),
    LEAVE_NETWORK_RESPONSE((byte) 7),
    LEAVE_NETWORK_CANCEL((byte) 8),
    LEAVE_NETWORK_CONFIRM((byte) 9),
    LEAVE_NETWORK_DONE((byte) 10),
    OPEN_MESSAGE((byte) 11),
    SECURE_MESSAGE((byte) 12),
    //Payload
    NEW_NODE((byte) 100),
    NEW_CONNECTION((byte) 101),
    REMOVE_NODE((byte) 102),
    //Instruction
    PASS_FORWARD((byte) -56), //(byte)200
    MESSAGE((byte) -55),      //(byte)201
    STOP((byte) -54),         //(byte)202
    //SYSTEME & INTERNE 
    SYSTEME((byte) -127),     //(byte)129_pour les messages d’erreur/annonce
	LIST_CONNECTED((byte) 13);

    private final byte opCode;

    OpCode(byte opCode) {
        this.opCode = opCode;
    }

    /**
     * Retourne le code brut sous forme de {@code byte}.
     *
     * @return Le code d'opération sous forme de byte.
     */
    public byte getCode() {
        return opCode;
    }

    /**
     * Retourne le code d'opération sous forme non signée (entre 0 et 255).
     *
     * @return Le code non signé de l'OpCode.
     */
    public int getUnsignedCode() {
        return Byte.toUnsignedInt(opCode);
    }

    /**
     * Convertit une valeur entière non signée (0–255) en {@link OpCode}.
     * <p>
     * Si la valeur ne correspond à aucun OpCode connu, {@code NO_STATE} est retourné et une erreur est affichée.
     * </p>
     *
     * @param code La valeur entière représentant un code d'opération (0–255).
     * @return L'OpCode correspondant ou {@code NO_STATE} si inconnu ou invalide.
     */
    public static OpCode intToOpCode(int code) {
        if(code < 0 || code > 255) {
            System.err.println("Invalid OpCode (out of unsigned byte range): " + code);
            return NO_STATE;
        }

        return Arrays.stream(values())
            .filter(op -> Byte.toUnsignedInt(op.opCode) == code)
            .findFirst()
            .orElseGet(() -> {
                System.err.println("Unknown OpCode: " + code);
                return NO_STATE;
            });
    }
    
}
