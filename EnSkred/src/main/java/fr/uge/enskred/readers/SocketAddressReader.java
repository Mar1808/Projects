package fr.uge.enskred.readers;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


/**
 * SocketAddressReader est un Reader permettant de lire une {@link java.net.SocketAddress}
 * depuis un {@link ByteBuffer}, selon le format suivant :
 * 
 * <pre>
 * [4 octets = taille de la chaîne "ip:port"] [n octets = chaîne UTF-8 "ip:port"]
 * </pre>
 * 
 * ---
 * 
 * Ce reader décode une chaîne de type "192.168.0.1:8080" pour en extraire l'adresse IP et le port,
 * et construit un objet {@link InetSocketAddress}.
 * 
 * ---
 * 
 * États internes :
 * <ul>
 *   <li>{@code WAITING_IP} : en attente de la lecture de la chaîne contenant l'adresse</li>
 *   <li>{@code DONE} : la SocketAddress est prête et accessible via {@code get()}</li>
 *   <li>{@code ERROR} : une erreur de parsing est survenue</li>
 * </ul>
 * 
 * ---
 * 
 * Méthodes principales :
 * @method process(ByteBuffer) : lit et traite la chaîne "ip:port" encodée UTF-8.
 * @method get() : retourne l’objet {@code InetSocketAddress} si le traitement est terminé.
 * @method reset() : réinitialise le reader pour permettre une nouvelle lecture.
 * 
 * ---
 * 
 * @implNote Utilise un {@link StringReader} interne pour la lecture de la chaîne, et
 * fait un parsing de la chaîne via {@code String.split(":")} pour extraire l'IP et le port.
 * La chaîne peut contenir un préfixe "/" (ex : "/127.0.0.1:8080") qui sera automatiquement supprimé.
 * 
 * ---
 * 
 * Exemple d’utilisation :
 * <pre>
 * ByteBuffer buffer = ...; // contient une IP+port
 * var reader = new SocketAddressReader();
 * if (reader.process(buffer) == ProcessStatus.DONE) {
 *     SocketAddress addr = reader.get();
 * }
 * </pre>
 * 
 * ---
 * 
 * @author Marwane KAOUANE
 * @author Massiouane MAIBECHE
 */
public final class SocketAddressReader implements Reader<SocketAddress> {
	private final StringReader stringReader = new StringReader();
	private final IntReader intReader = new IntReader();
	private InetSocketAddress socketAddress = null;
	private State state = State.WAITING_IP;
	private String ip;
	
	private enum State {
		WAITING_IP, DONE, ERROR 
	}

	@Override
	public ProcessStatus process(ByteBuffer buffer) {
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException("State: " + state);
		}
        if(Objects.requireNonNull(state) != State.WAITING_IP) {
			state = State.ERROR;
			return ProcessStatus.ERROR;
        }
		switch (stringReader.process(buffer)) {
			case REFILL:
				return ProcessStatus.REFILL;
			case DONE:
				ip = stringReader.get();
				state = State.DONE;
				return ProcessStatus.DONE;
			default:
				state = State.ERROR;
				return ProcessStatus.ERROR;
		}
    }

	@Override
	public InetSocketAddress get() {
		if(state != State.DONE) {
			throw new IllegalStateException("State is not DONE");
		}
		socketAddress = (socketAddress != null) ? socketAddress : createSocketAddress(ip);
		return socketAddress;
	}


	@Override
	public void reset() {
		state = State.WAITING_IP;
		stringReader.reset();
		intReader.reset();
		ip = null;
		socketAddress = null;
	}

	private InetSocketAddress createSocketAddress(String ip) {
		var parts = ip.split(":", 2);
	    if(parts.length != 2) {
	        throw new IllegalStateException("Invalid IP format, missing port");
	    }
	    var ipAddress = parts[0];
	    int portNumber;
	    try {
	        portNumber = Integer.parseInt(parts[1]);
	    } catch (NumberFormatException e) {
	        throw new IllegalStateException("Invalid port format", e);
	    }
	    if(ipAddress.startsWith("/")) {
	        ipAddress = ipAddress.substring(1);
	    }
		return new InetSocketAddress(ipAddress, portNumber);
	}

	//MAIN

	public static void main(String[] args) {
		testSingleSocketAddressInLargeBuffer();
	}

	// Test 1 : Lire une adresse IP et un port dans un buffer assez grand
	public static void testSingleSocketAddressInLargeBuffer() {
		var ipAddress = "192.168.1.48:667";
		var buffer = getSocketAddressBuffer(ipAddress);
		var socketAddressReader = new SocketAddressReader();
		var status = socketAddressReader.process(buffer);

		if(status == ProcessStatus.DONE) {
			var result = socketAddressReader.get();
			System.out.println("Test 1 réussi. Adresse lue: " + result);
		} else {
			System.out.println("Test échoué. Statut: " + status);
		}
	}
	
	private static ByteBuffer getSocketAddressBuffer(String ipAddress) {
		var encodedIp = StandardCharsets.UTF_8.encode(ipAddress);
		var buffer = ByteBuffer.allocate(Integer.BYTES + encodedIp.remaining());
		buffer.putInt(encodedIp.remaining()).put(encodedIp);  
		return buffer;
	}
}

















