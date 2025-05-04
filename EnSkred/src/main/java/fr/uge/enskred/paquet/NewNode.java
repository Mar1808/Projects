package fr.uge.enskred.paquet;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * Paquet représentant un nouveau nœud dans le réseau, avec des informations concernant 
 * l'expéditeur et le récepteur, ainsi que l'adresse du socket de l'expéditeur.
 * 
 * Ce paquet est utilisé lors de l'ajout d'un nouveau nœud dans le réseau, en envoyant les clés
 * publiques de l'expéditeur et du récepteur ainsi que l'adresse du socket de l'expéditeur.
 * 
 * La classe sérialise ces informations dans un format binaire afin qu'elles puissent être
 * transmises à travers le réseau de manière sécurisée.
 */
public record NewNode(PublicKeyRSA publicKeySender, SocketAddress socketAddressSender, PublicKeyRSA publicKeyReceiver) implements Paquet, Payload {
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final OpCode OP_CODE = OpCode.NEW_NODE;

	public NewNode {
		Utils.requireNonNulls(publicKeySender, publicKeyReceiver, socketAddressSender);
	}
	
	@Override
	public ByteBuffer getWriteModeBuffer() {
		//SENDER PK
		var pubBufferSender = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		publicKeySender.to(pubBufferSender);
		var PKBufferSender = ByteBuffer.allocate(Integer.BYTES + pubBufferSender.flip().remaining());
		PKBufferSender.putInt(pubBufferSender.remaining()).put(pubBufferSender).flip();
		//SOCKETADDRESS SENDER
		var encodeSocketAddress = UTF8.encode(socketAddressSender.toString().split("/")[1]);
		var socketAddressLength = encodeSocketAddress.remaining();
		//SENDER PK
		var pubBufferReceiver = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
		publicKeyReceiver.to(pubBufferReceiver);
		var PKBufferReceiver = ByteBuffer.allocate(Integer.BYTES + pubBufferReceiver.flip().remaining());
		PKBufferReceiver.putInt(pubBufferReceiver.remaining()).put(pubBufferReceiver).flip();
		//build buffer
		var buffer = ByteBuffer.allocate(Byte.BYTES + PKBufferReceiver.remaining() + Integer.BYTES + encodeSocketAddress.remaining() + PKBufferReceiver.remaining());
		buffer.put(OP_CODE.getCode()).put(PKBufferSender).putInt(socketAddressLength).put(encodeSocketAddress).put(PKBufferReceiver);
		return buffer;
	}

	@Override
	public String toString() {
		return "Sender: " + publicKeySender + "\nSocket: " + socketAddressSender + "\nReceiver: " + publicKeyReceiver; 
	}

	@Override
	public OpCode getOpCode() {
		return OP_CODE;
	}
}
