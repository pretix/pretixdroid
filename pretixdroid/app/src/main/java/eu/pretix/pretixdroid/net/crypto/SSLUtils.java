package eu.pretix.pretixdroid.net.crypto;

import android.annotation.SuppressLint;
import android.content.Context;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jce.provider.X509CertificateObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Calendar;
import java.util.Date;

@SuppressWarnings("deprecation")
public class SSLUtils {

    public static final String FILE_NAME = "keystore.bks";
    public static final String KEY_ALIAS = "ssl";
    public static final String CNAME = "node.pretixdroid.pretix.eu";

    /**
     * Creates a new SSL key and certificate and stores them in the app's
     * internal data directory.
     *
     * @param ctx              An Android application context
     * @param keystorePassword The password to be used for the keystore
     * @return boolean indicating success or failure
     */
    @SuppressLint("TrulyRandom")
    public static boolean genSSLKey(Context ctx, String keystorePassword) {
        try {
            // Create a new pair of RSA keys using BouncyCastle classes
            RSAKeyPairGenerator gen = new RSAKeyPairGenerator();
            gen.init(new RSAKeyGenerationParameters(BigInteger.valueOf(3),
                    new SecureRandom(), 1024, 80));
            AsymmetricCipherKeyPair keypair = gen.generateKeyPair();
            RSAKeyParameters publicKey = (RSAKeyParameters) keypair.getPublic();
            RSAPrivateCrtKeyParameters privateKey = (RSAPrivateCrtKeyParameters) keypair
                    .getPrivate();

            // We also need our pair of keys in another format, so we'll convert
            // them using java.security classes
            PublicKey pubKey = KeyFactory.getInstance("RSA").generatePublic(
                    new RSAPublicKeySpec(publicKey.getModulus(), publicKey
                            .getExponent()));
            PrivateKey privKey = KeyFactory.getInstance("RSA").generatePrivate(
                    new RSAPrivateCrtKeySpec(publicKey.getModulus(), publicKey
                            .getExponent(), privateKey.getExponent(),
                            privateKey.getP(), privateKey.getQ(), privateKey
                            .getDP(), privateKey.getDQ(), privateKey
                            .getQInv()));

            // CName or other certificate details do not really matter here
            X509Name x509Name = new X509Name("CN=" + CNAME);

            // We have to sign our public key now. As we do not need or have
            // some kind of CA infrastructure, we are using our new keys
            // to sign themselves

            // Set certificate meta information
            V3TBSCertificateGenerator certGen = new V3TBSCertificateGenerator();
            certGen.setSerialNumber(new DERInteger(BigInteger.valueOf(System
                    .currentTimeMillis())));
            certGen.setIssuer(new X509Name("CN=" + CNAME));
            certGen.setSubject(x509Name);
            DERObjectIdentifier sigOID = PKCSObjectIdentifiers.sha1WithRSAEncryption;
            AlgorithmIdentifier sigAlgId = new AlgorithmIdentifier(sigOID,
                    new DERNull());
            certGen.setSignature(sigAlgId);
            ByteArrayInputStream bai = new ByteArrayInputStream(
                    pubKey.getEncoded());
            ASN1InputStream ais = new ASN1InputStream(bai);
            certGen.setSubjectPublicKeyInfo(new SubjectPublicKeyInfo(
                    (ASN1Sequence) ais.readObject()));
            bai.close();
            ais.close();

            // We want our keys to live long
            Calendar expiry = Calendar.getInstance();
            expiry.add(Calendar.DAY_OF_YEAR, 365 * 30);

            certGen.setStartDate(new Time(new Date(System.currentTimeMillis())));
            certGen.setEndDate(new Time(expiry.getTime()));
            TBSCertificateStructure tbsCert = certGen.generateTBSCertificate();

            // The signing: We first build a hash of our certificate, than sign
            // it with our private key
            SHA1Digest digester = new SHA1Digest();
            AsymmetricBlockCipher rsa = new PKCS1Encoding(new RSAEngine());
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            DEROutputStream dOut = new DEROutputStream(bOut);
            dOut.writeObject(tbsCert);
            byte[] signature;
            byte[] certBlock = bOut.toByteArray();
            // first create digest
            digester.update(certBlock, 0, certBlock.length);
            byte[] hash = new byte[digester.getDigestSize()];
            digester.doFinal(hash, 0);
            // and sign that
            rsa.init(true, privateKey);
            DigestInfo dInfo = new DigestInfo(new AlgorithmIdentifier(
                    X509ObjectIdentifiers.id_SHA1, null), hash);
            byte[] digest = dInfo.getEncoded(ASN1Encodable.DER);
            signature = rsa.processBlock(digest, 0, digest.length);
            dOut.close();

            // We build a certificate chain containing only one certificate
            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(tbsCert);
            v.add(sigAlgId);
            v.add(new DERBitString(signature));
            X509CertificateObject clientCert = new X509CertificateObject(
                    new X509CertificateStructure(new DERSequence(v)));
            X509Certificate[] chain = new X509Certificate[1];
            chain[0] = clientCert;

            // We add our certificate to a new keystore
            KeyStore keyStore = KeyStore.getInstance("BKS");
            keyStore.load(null);
            keyStore.setKeyEntry(KEY_ALIAS, (Key) privKey,
                    keystorePassword.toCharArray(), chain);

            // We write this keystore to a file
            OutputStream out = ctx.openFileOutput(FILE_NAME,
                    Context.MODE_PRIVATE);
            keyStore.store(out, keystorePassword.toCharArray());
            out.close();
            return true;
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | InvalidCipherTextException | InvalidKeySpecException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean hasKeyStore(Context ctx) {
        try {
            ctx.openFileInput(FILE_NAME).close();
            return true;
        } catch (FileNotFoundException e) {
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    public static String getSHA1Hash(Context ctx, String KEYSTORE_PASSWORD) {
        InputStream in = null;
        KeyStore keyStore;
        try {
            in = ctx.openFileInput(FILE_NAME);
            keyStore = KeyStore.getInstance("BKS");
            keyStore.load(in, KEYSTORE_PASSWORD.toCharArray());
            return new String(Hex.encodeHex(DigestUtils.sha1(keyStore
                    .getCertificate(KEY_ALIAS).getEncoded())));
        } catch (KeyStoreException | NoSuchAlgorithmException | IOException | CertificateException e) {
            e.printStackTrace();
        } finally {
            try {
                if (in != null)
                    in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }
}
