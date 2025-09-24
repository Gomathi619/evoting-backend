package com.evoting.evoting_backend.service;

import com.evoting.evoting_backend.crypto.PaillierUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.math.BigInteger;
import java.util.Base64;

@Service
public class PaillierKeyService {

    @Value("${paillier.key.file.path}")
    private String keyFilePath;

    private PaillierUtil paillierUtil;

    @PostConstruct
    public void init() {
        this.paillierUtil = loadKeys();
    }

    public PaillierUtil getPaillierUtil() {
        return paillierUtil;
    }

    private PaillierUtil loadKeys() {
        File keyFile = new File(keyFilePath);
        if (keyFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(keyFile))) {
                PaillierUtil loadedUtil = new PaillierUtil();
                loadedUtil.setN((BigInteger) ois.readObject());
                loadedUtil.setG((BigInteger) ois.readObject());
                loadedUtil.setNsquare(loadedUtil.getN().multiply(loadedUtil.getN()));
                loadedUtil.setLambda((BigInteger) ois.readObject());
                loadedUtil.setMu((BigInteger) ois.readObject());
                System.out.println("Paillier keys loaded from file.");
                return loadedUtil;
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Failed to load Paillier keys. Generating new ones.");
            }
        }
        return generateAndSaveKeys();
    }

    private PaillierUtil generateAndSaveKeys() {
        PaillierUtil newUtil = new PaillierUtil();
        newUtil.generateKeys();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(keyFilePath))) {
            oos.writeObject(newUtil.getN());
            oos.writeObject(newUtil.getG());
            oos.writeObject(newUtil.getLambda());
            oos.writeObject(newUtil.getMu());
            System.out.println("New Paillier keys generated and saved to file.");
        } catch (IOException e) {
            System.err.println("Failed to save Paillier keys: " + e.getMessage());
        }
        return newUtil;
    }

    public String getPublicKey() {
        return Base64.getEncoder().encodeToString(paillierUtil.getN().toByteArray());
    }
}