package eu.xfsc.fc.tools.signer;

import org.junit.jupiter.api.Test;

public class SDSignerTest {
	
	
	@Test
	public void testSigner() throws Exception {
		
		String original = "legalPerson_one_VC.jsonld";
		String signed = "legalPerson_one_VC_signed.jsonld";
		SDSigner.main(new String[] {original, signed});
		
	}

}
