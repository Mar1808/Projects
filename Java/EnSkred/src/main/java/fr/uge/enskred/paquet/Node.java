package fr.uge.enskred.paquet;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;


/**
 * Paquet représentant un nœud dans le réseau, avec une clé publique et une adresse de socket.
 * 
 * Cette classe est utilisée en interne pour encapsuler les informations d'un nœud, 
 * comprenant sa clé publique et son adresse de socket. Ces informations sont 
 * sérialisées dans un format binaire afin d'être envoyées à travers le réseau.
 *
 * La clé publique représente l'identité cryptographique du nœud, et l'adresse de socket permet
 * de localiser ce nœud sur le réseau.
 */
public record Node(PublicKeyRSA publicKey, InetSocketAddress socketAddress) implements Paquet {
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	
	public Node {
		Utils.requireNonNulls(publicKey, socketAddress);
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		//PK
		var pubBuffer = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
        publicKey.to(pubBuffer);
        var PKBuffer = ByteBuffer.allocate(Integer.BYTES + pubBuffer.flip().remaining());
        PKBuffer.putInt(pubBuffer.remaining()).put(pubBuffer).flip();
        //SA
        var encodeSocketAddress = UTF8.encode(socketAddress.toString().split("/")[1]);
		var socketAddressLength = encodeSocketAddress.remaining();
		var buffer = ByteBuffer.allocate(PKBuffer.remaining() + Integer.BYTES + encodeSocketAddress.remaining());
		buffer.put(PKBuffer).putInt(socketAddressLength).put(encodeSocketAddress);
		return buffer;
	}
	
	
	@Override
	public String toString() {
		return "PublicKey: " + publicKey + "\nSocketAddress: " + socketAddress;
	}	

	public static void main(String[] args) {
		try {
            var keyPair1 = UGEncrypt.KeyPairRSA.generate();
            var publicKey = keyPair1.publicKey();
            new Node(publicKey, new InetSocketAddress(9)).getWriteModeBuffer();
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur lors de la génération des clés RSA.");
        }
	}

	@Override
	public OpCode getOpCode() {
		return OpCode.NO_STATE;
	}
	
}
