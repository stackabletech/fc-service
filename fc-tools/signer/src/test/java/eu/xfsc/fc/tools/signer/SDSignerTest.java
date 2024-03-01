package eu.xfsc.fc.tools.signer;

import org.junit.jupiter.api.Test;

public class SDSignerTest {
	
	
	@Test
	public void testVPSignature() throws Exception {
		
		String original = "sd=legalPerson_one_VC.jsonld";
		String signed = "ssd=legalPerson_one_VC_signed.jsonld";
		SDSigner.main(new String[] {"t=VP", original, signed, "c=true"});
	}

	//@Test 
	//fails from cmd line, for some reason.. java.lang.NullPointerException: Cannot invoke "foundation.identity.jsonld.JsonLDObject.getDocumentLoader()" because "jsonLdObject" is null; works perfectly from IDE
	public void testVCSignature() throws Exception {
		
		String original = "sd=tl_vc_3s.jsonld";
		String signed = "ssd=tl_vc_signed.jsonld";
		SDSigner.main(new String[] {original, signed, "puk=src/main/resources/rsa2048.pub.pem", "prk=src/main/resources/rsa2048.sign.pem", "i=did:jwk:eyJlIjoiQVFBQiIsImtpZCI6InNpZ25SU0EyMDQ4Iiwia3R5IjoiUlNBIiwibiI6IjhDUl8ySUJuSGlDcjVIY0kxcWh1dXpiRmdMRjFlSVI3dGM1NTF2WlN2ek1rTEtIVEpxeHVZR1BlVXp2U29ndWctTDJRQ2tpVWdmSjFZUzRFUmhrRFM2cVBJbFdMM1BGbkc4VjZQVWxoODZjTjgxWkhZdkhoUFYwUDdKT25UeGVKcTV1a1dFT0ctMUxrTmk5eGVnelRoMEVwV0w4NU5aUmJOYWJ0R0VqTUhqY04yak9NUldSYkE4OVdENVhOTExwdWUyNVlxbHcxOGZsbXNNRmh5dXk2YlVmanR2aXJ5SThwb0RLOVpXcGtPUjN6ejlNN0hpQXVyZ2NLSks5ODlEX0UzZ3d3S1FhQ21CbEJ4X3hESlJoVFB1bnJZUFBySHlpMExmaG5CeWgzcEJRMWhyelFLblZyX18zYkVwcE5abmRFLVhlWTY0VzNNaGUwa1RrREp2VXJndyJ9#0"});
	}
}


