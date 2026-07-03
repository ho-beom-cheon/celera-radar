package com.sellerradar.wholesale.service;

import com.sellerradar.wholesale.domain.CsvEncoding;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

@Component
public class CsvEncodingDetector {
	private static final Charset CP949 = Charset.forName("MS949");

	public CsvEncoding detect(byte[] bytes, CsvEncoding requestedEncoding) {
		if (requestedEncoding == CsvEncoding.UTF_8 || requestedEncoding == CsvEncoding.CP949) {
			return requestedEncoding;
		}
		return isValidUtf8(bytes) ? CsvEncoding.UTF_8 : CsvEncoding.CP949;
	}

	public String decode(byte[] bytes, CsvEncoding encoding) {
		Charset charset = encoding == CsvEncoding.CP949 ? CP949 : StandardCharsets.UTF_8;
		return charset.decode(ByteBuffer.wrap(bytes)).toString();
	}

	private boolean isValidUtf8(byte[] bytes) {
		CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
				.onMalformedInput(CodingErrorAction.REPORT)
				.onUnmappableCharacter(CodingErrorAction.REPORT);
		try {
			decoder.decode(ByteBuffer.wrap(bytes));
			return true;
		} catch (CharacterCodingException exception) {
			return false;
		}
	}
}
