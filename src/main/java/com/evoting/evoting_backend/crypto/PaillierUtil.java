package com.evoting.evoting_backend.crypto;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;

public class PaillierUtil implements Serializable {
    private BigInteger p, q, n, nsquare, g, lambda, mu;
    private int bitLength = 512;

    public PaillierUtil() {}

    public void generateKeys() {
        SecureRandom random = new SecureRandom();
        p = BigInteger.probablePrime(bitLength, random);
        q = BigInteger.probablePrime(bitLength, random);
        n = p.multiply(q);
        nsquare = n.multiply(n);
        g = n.add(BigInteger.ONE);
        lambda = p.subtract(BigInteger.ONE).multiply(q.subtract(BigInteger.ONE));
        mu = lambda.modInverse(n);
    }

    public BigInteger encrypt(BigInteger m) {
        SecureRandom random = new SecureRandom();
        BigInteger r = new BigInteger(bitLength, random);
        return g.modPow(m, nsquare).multiply(r.modPow(n, nsquare)).mod(nsquare);
    }

    public BigInteger add(BigInteger c1, BigInteger c2) {
        return c1.multiply(c2).mod(nsquare);
    }

    public BigInteger decrypt(BigInteger c) {
        BigInteger u = c.modPow(lambda, nsquare).subtract(BigInteger.ONE).divide(n).multiply(mu).mod(n);
        return u;
    }

    public BigInteger getN() {
        return n;
    }
    
    public BigInteger getG() {
        return g;
    }

    public BigInteger getP() {
        return p;
    }

    public BigInteger getQ() {
        return q;
    }

    public BigInteger getLambda() {
        return lambda;
    }

    public BigInteger getMu() {
        return mu;
    }

    // Setters for key loading
    public void setN(BigInteger n) { this.n = n; }
    public void setG(BigInteger g) { this.g = g; }
    public void setP(BigInteger p) { this.p = p; }
    public void setQ(BigInteger q) { this.q = q; }
    public void setLambda(BigInteger lambda) { this.lambda = lambda; }
    public void setMu(BigInteger mu) { this.mu = mu; }
    public void setNsquare(BigInteger nsquare) { this.nsquare = nsquare; }
}