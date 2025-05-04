package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import fr.uge.enskred.opcode.OpCode;


/**
 * Paquet représentant la liste des utilisateurs actuellement connectés au réseau.
 * 
 * Cette classe encapsule une {@link Map} associant un identifiant d'application (clé) à un {@link Node}
 * représentant un utilisateur connecté. Elle est utilisée pour envoyer la liste des utilisateurs actuellement
 * connectés, permettant de mettre à jour et de consulter cette liste.
 */
public record ListConnected(Map<Integer, Node> apps) implements Paquet {
	
    public ListConnected {
        Objects.requireNonNull(apps, "La map 'apps' ne doit pas être null.");
    }
    public ListConnected() {
        this(new HashMap<>());
    }
	
	public void updateInfos(Map<Integer, Node> copy) {
		Objects.requireNonNull(copy);
		copy.forEach((key, value) -> {
			if(key == null && value == null) {
				return;
			}
			apps.put(key, value);
		});
	}
	
	@Override
	public String toString() {
	    if (apps.isEmpty()) {
	        return "Aucun utilisateur connecté.";
	    }
	    var builder = new StringBuilder("Voici les utilisateurs connectés :\n");
	    apps.forEach((id, node) -> builder
	        .append("ID: ").append(id)
	        .append(" -> PublicKey: ").append(node.publicKey())
	        .append("\n"));
	    return builder.toString();
	}
 
	@Override
	public OpCode getOpCode() {
		return OpCode.LIST_CONNECTED;
	}
	
	//Non utile---------------------------------
	@Override
	public ByteBuffer getWriteModeBuffer() {
		return ByteBuffer.allocate(0);
	}
	//------------------------------------------
	
}
