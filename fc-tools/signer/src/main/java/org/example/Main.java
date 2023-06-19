package org.example;

import com.danubetech.keyformats.crypto.PrivateKeySigner;
import com.danubetech.keyformats.crypto.PublicKeyVerifier;
import com.danubetech.keyformats.crypto.impl.RSA_PS256_PrivateKeySigner;
import com.danubetech.keyformats.crypto.impl.RSA_PS256_PublicKeyVerifier;
import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.VerifiablePresentation;
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
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

public class Main {
    //openssl req -x509 -newkey rsa:4096 -keyout prk.ss.pem -out cert.ss.pem -sha256 -days 365 -nodes
    private static final String PATH_TO_PRIVATE_KEY = "src/main/resources/prk.ss.pem";
    private static final String PATH_TO_PUBLIC_KEY = "src/main/resources/cert.ss.pem";
    private static String PATH_TO_SELF_DESCRIPTION = "src/main/resources/vc.json";
    private static String PATH_TO_SIGNED_SELF_DESCRIPTION = "src/main/resources/sd.signed.json";

    static String readFile (String path) throws IOException {
        return Files.readString(Path.of(path));
    }

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        System.out.println("args: " + java.util.Arrays.toString(args));
        if (args.length > 0) {
          PATH_TO_SELF_DESCRIPTION = args[0];
          if (args.length > 1) {
            PATH_TO_SIGNED_SELF_DESCRIPTION = args[1];
          }
        }

        VerifiablePresentation vp = VerifiablePresentation.fromJson(readFile(PATH_TO_SELF_DESCRIPTION));
        VerifiableCredential vc = vp.getVerifiableCredential();

        System.out.println("Signing VC");
        LdProof vc_proof = sign(vc);
        check(vc, vc_proof);
        System.out.println("Signed");

        vc.setJsonObjectKeyValue("proof", vc.getLdProof().getJsonObject());
        vp.setJsonObjectKeyValue("verifiableCredential", vc.getJsonObject());

        System.out.println("Signing VC");
        LdProof vp_proof = sign(vp);
        check(vp, vp_proof);
        System.out.println("Signed");

        vp.setJsonObjectKeyValue("proof", vp.getLdProof().getJsonObject());

        JSONObject json = new JSONObject(vp.toString()); 
        //System.out.println(json.toString(4));

        Files.writeString(Path.of(PATH_TO_SIGNED_SELF_DESCRIPTION), json.toString(4));
    }

    public static LdProof sign (JsonLDObject credential) throws IOException, GeneralSecurityException, JsonLDException {
        FileReader fileReader = new FileReader(PATH_TO_PRIVATE_KEY);

        PEMParser pemParser = new PEMParser(fileReader);
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        PrivateKeyInfo privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());

        PrivateKey prk = converter.getPrivateKey(privateKeyInfo);

        KeyPair kp = new KeyPair(null, prk);
        PrivateKeySigner<?> privateKeySigner = new RSA_PS256_PrivateKeySigner(kp);

        LdSigner signer = new JsonWebSignature2020LdSigner(privateKeySigner);

        signer.setCreated(new Date());
        signer.setProofPurpose(LDSecurityKeywords.JSONLD_TERM_ASSERTIONMETHOD);
        signer.setVerificationMethod(URI.create("did:web:compliance.lab.gaia-x.eu"));

        LdProof ldProof = signer.sign(credential);

        return ldProof;
    }

    public static void check (JsonLDObject credential, LdProof proof) throws IOException, GeneralSecurityException, JsonLDException {
        //---extract Expiration Date--- https://stackoverflow.com/a/11621488
        String certString = readFile(PATH_TO_PUBLIC_KEY);
        ByteArrayInputStream certStream  =  new ByteArrayInputStream(certString.getBytes(StandardCharsets.UTF_8));
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        List<X509Certificate> certs = (List<X509Certificate>) certFactory.generateCertificates(certStream);

        for(X509Certificate cert : certs) {

            PublicKey puk = cert.getPublicKey();
            PublicKeyVerifier<?> pkVerifier = new RSA_PS256_PublicKeyVerifier((RSAPublicKey) puk);

            LdVerifier verifier = new JsonWebSignature2020LdVerifier(pkVerifier);

            System.out.println(verifier.verify(credential, proof));
        }
    }
}
