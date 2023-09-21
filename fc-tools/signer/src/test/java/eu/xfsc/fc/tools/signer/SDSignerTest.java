package eu.xfsc.fc.tools.signer;

import org.junit.jupiter.api.Test;
import java.io.File;

public class SDSignerTest {

	@Test
	public void testSigner() throws Exception {
		String original = "src/test/resources/legalPerson_one_VC.jsonld";
		String signed = File.createTempFile("temp", ".json").getAbsolutePath();
		SDSigner.main(new String[] {original, signed});
	}

	@Test
	public void testSigner_StackableSD() throws Exception {
		String original = "src/test/resources/stackable_SD_unsigned.json";
		String signed = File.createTempFile("temp", ".json").getAbsolutePath();
		SDSigner.main(new String[] {original, signed});
	}
}
