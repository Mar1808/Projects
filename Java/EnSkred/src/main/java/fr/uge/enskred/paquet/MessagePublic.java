package fr.uge.enskred.paquet;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import fr.uge.enskred.opcode.OpCode;
import fr.uge.enskred.readers.UGEncrypt;
import fr.uge.enskred.readers.UGEncrypt.PublicKeyRSA;
import fr.uge.enskred.utils.Utils;

/**
 * Paquet représentant un message public à envoyer entre deux parties dans l'application.
 * 
 * Cette classe encapsule un message accompagné des clés publiques de l'expéditeur et du destinataire.
 * Le message est sérialisé et peut être transmis de manière sécurisée entre les parties concernées.
 * Elle est utilisée pour l'envoi d'informations sensibles, où l'intégrité et la confidentialité sont
 * essentielles.
 */
public record MessagePublic(PublicKeyRSA sender, PublicKeyRSA receiver, String message) implements Paquet {
	
	private final static Charset UTF8 = StandardCharsets.UTF_8;
	private final static OpCode OP_CODE = OpCode.OPEN_MESSAGE;
	
	public MessagePublic {
		Utils.requireNonNulls(sender, receiver, message);
	}

	@Override
	public ByteBuffer getWriteModeBuffer() {
		//Sender
		var pubBuffer = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
        sender.to(pubBuffer);
        var PKBuffer = ByteBuffer.allocate(Integer.BYTES + pubBuffer.flip().remaining());
        PKBuffer.putInt(pubBuffer.remaining()).put(pubBuffer).flip();
        
        //Receiver
        var pubBuffer2 = ByteBuffer.allocate(UGEncrypt.MAX_PUBLIC_KEY_SIZE);
        receiver.to(pubBuffer2);
        var PKBuffer2 = ByteBuffer.allocate(Integer.BYTES + pubBuffer2.flip().remaining());
        PKBuffer2.putInt(pubBuffer2.remaining()).put(pubBuffer2).flip();
        
        //Message
		var encodedMsg = UTF8.encode(message);
		var buffer = ByteBuffer.allocate(Integer.BYTES + encodedMsg.remaining());
		buffer.putInt(encodedMsg.remaining()).put(encodedMsg);
		buffer.flip();
		
		var finalBuffer = ByteBuffer.allocate(Byte.BYTES + PKBuffer.remaining() + PKBuffer2.remaining() + buffer.remaining());
		
		finalBuffer.put(OP_CODE.getCode()).put(PKBuffer).put(PKBuffer2).put(buffer);
		return finalBuffer;
	}

	@Override
	public OpCode getOpCode() {
		// TODO Auto-generated method stub
		return OP_CODE;
	}

	@Override
	public String toString() {
		return "Exp: " + sender + "\nDest: " + receiver + "\nLe msg: " + message;
	}
	
}
