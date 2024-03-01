package eu.xfsc.fc.tools.signer;

import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.danubetech.keyformats.crypto.PrivateKeySigner;
import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.crypto.impl.RSA_PS256_PrivateKeySigner;
import com.danubetech.keyformats.crypto.impl.RSA_PS256_PublicKeyVerifier;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
import com.danubetech.verifiablecredentials.jsonld.VerifiableCredentialContexts;

import foundation.identity.jsonld.ConfigurableDocumentLoader;
import foundation.identity.jsonld.JsonLDException;
import foundation.identity.jsonld.JsonLDObject;
import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.jsonld.LDSecurityKeywords;
import info.weboftrust.ldsignatures.signer.JsonWebSignature2020LdSigner;
import info.weboftrust.ldsignatures.signer.LdSigner;
import info.weboftrust.ldsignatures.verifier.JsonWebSignature2020LdVerifier;
import info.weboftrust.ldsignatures.verifier.LdVerifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

public class SDSigner {
    //openssl req -x509 -newkey rsa:4096 -keyout prk.ss.pem -out cert.ss.pem -sha256 -days 365 -nodes
    private static String PATH_TO_PRIVATE_KEY = "src/main/resources/prk.ss.pem";
    private static String PATH_TO_PUBLIC_KEY = "src/main/resources/cert.ss.pem";
    private static String PATH_TO_SELF_DESCRIPTION = "src/main/resources/vc.json";
    private static String PATH_TO_SIGNED_SELF_DESCRIPTION = "src/main/resources/sd.signed.json";
    private static String ISSUER = "did:web:compliance.lab.gaia-x.eu";
    private static String SD_TYPE = "VC";
    private static String CHECK = "false";
    

    static String readFile(String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        System.out.println("args: " + java.util.Arrays.toString(args));
        
    	for (String arg: args) {
    		String[] parts = arg.split("=");
    		if ("t".equals(parts[0]) || "type".equals(parts[0])) {
    			SD_TYPE = parts[1];
    			continue;
    		}
    		if ("i".equals(parts[0]) || "issuer".equals(parts[0])) {
    			ISSUER = parts[1];
    			continue;
    		}
    		if ("c".equals(parts[0]) || "check".equals(parts[0])) {
    			CHECK = parts[1];
    			continue;
    		}
    		if ("puk".equals(parts[0]) || "public-key".equals(parts[0])) {
    			PATH_TO_PUBLIC_KEY = parts[1];
    			continue;
    		}
    		if ("prk".equals(parts[0]) || "private-key".equals(parts[0])) {
    			PATH_TO_PRIVATE_KEY = parts[1];
    			continue;
    		}
    		if ("sd".equals(parts[0]) || "self-description".equals(parts[0])) {
    			PATH_TO_SELF_DESCRIPTION = parts[1];
    			continue;
    		}
    		if ("ssd".equals(parts[0]) || "signed-description".equals(parts[0])) {
    			PATH_TO_SIGNED_SELF_DESCRIPTION = parts[1];
    			continue;
    		}
			System.out.println("unknown parameter: " + arg);
    	}
        
    	boolean check = Boolean.parseBoolean(CHECK);
        String json = readFile(PATH_TO_SELF_DESCRIPTION);
    	JsonLDObject ld = JsonLDObject.fromJson(json);
    	VerifiableCredential vc;
        if (SD_TYPE.equalsIgnoreCase("VP")) {
            VerifiablePresentation vp = VerifiablePresentation.fromJsonLDObject(ld);
            System.out.println("Signing VP");
            LdProof vp_proof = sign(vp);
            System.out.println("VP Signed");
            if (check) {
                check(vp, vp_proof);
                System.out.println("VP Signature verified");
            }
            vc = vp.getVerifiableCredential();
        } else if (SD_TYPE.equalsIgnoreCase("VC")) {
        	vc = VerifiableCredential.fromJsonLDObject(ld);
        } else {
            System.out.println("Unknown SD type: " + SD_TYPE);
            return;
        }
        System.out.println("Signing VC");
        LdProof vc_proof = sign(vc);
        System.out.println("VC Signed");
        if (check) {
            check(vc, vc_proof);
            System.out.println("VC Signature verified");
        }
        Files.writeString(Path.of(PATH_TO_SIGNED_SELF_DESCRIPTION), ld.toJson(true));
    }

    public static LdProof sign(JsonLDObject credential) throws IOException, GeneralSecurityException, JsonLDException, URISyntaxException, JsonLdError {
        FileReader fileReader = new FileReader(PATH_TO_PRIVATE_KEY);

        PEMParser pemParser = new PEMParser(fileReader);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        Object inst = pemParser.readObject();
        PrivateKeyInfo privateKeyInfo;
        if (inst instanceof PEMKeyPair) {
        	privateKeyInfo = ((PEMKeyPair) inst).getPrivateKeyInfo();
        } else {
            privateKeyInfo = PrivateKeyInfo.getInstance(inst);
        }

        PrivateKey prk = converter.getPrivateKey(privateKeyInfo);

        KeyPair kp = new KeyPair(null, prk);
        PrivateKeySigner<?> privateKeySigner = new RSA_PS256_PrivateKeySigner(kp);

		ConfigurableDocumentLoader loader = (ConfigurableDocumentLoader) VerifiableCredentialContexts.DOCUMENT_LOADER;
		loader.getLocalCache().put(new URI("https://schema.org"), JsonDocument.of(new StringReader("{\"@context\": {}}")));
        
        LdSigner signer = new JsonWebSignature2020LdSigner(privateKeySigner);

        signer.setCreated(new Date());
        signer.setProofPurpose(LDSecurityKeywords.JSONLD_TERM_ASSERTIONMETHOD);
        signer.setVerificationMethod(URI.create(ISSUER));

        LdProof ldProof = signer.sign(credential); 
        return ldProof;
    }

    public static void check(JsonLDObject credential, LdProof proof) throws IOException, GeneralSecurityException, JsonLDException {
        //---extract Expiration Date--- https://stackoverflow.com/a/11621488
        String certString = readFile(PATH_TO_PUBLIC_KEY);
        ByteArrayInputStream certStream  =  new ByteArrayInputStream(certString.getBytes(StandardCharsets.UTF_8));
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certs = (List<X509Certificate>) certFactory.generateCertificates(certStream);

        for(X509Certificate cert : certs) {
            PublicKey puk = cert.getPublicKey();
            PublicKeyVerifier<?> pkVerifier = new RSA_PS256_PublicKeyVerifier((RSAPublicKey) puk);
            LdVerifier verifier = new JsonWebSignature2020LdVerifier(pkVerifier);
            boolean verified = verifier.verify(credential, proof);
            System.out.println("issuer: " + cert.getIssuerX500Principal() + "; verified: " + verified);
        }
    }
}
