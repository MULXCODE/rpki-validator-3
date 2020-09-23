/**
 * The BSD License
 *
 * Copyright (c) 2010-2018 RIPE NCC
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *   - Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *   - Neither the name of the RIPE NCC nor the names of its contributors may be
 *     used to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.ripe.rpki.validator3.storage.data;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.ripe.rpki.commons.crypto.CertificateRepositoryObject;
import net.ripe.rpki.commons.crypto.UnknownCertificateRepositoryObject;
import net.ripe.rpki.commons.crypto.cms.RpkiSignedObject;
import net.ripe.rpki.commons.crypto.cms.ghostbuster.GhostbustersCms;
import net.ripe.rpki.commons.crypto.cms.manifest.ManifestCms;
import net.ripe.rpki.commons.crypto.cms.roa.RoaCms;
import net.ripe.rpki.commons.crypto.crl.X509Crl;
import net.ripe.rpki.commons.crypto.util.CertificateRepositoryObjectFactory;
import net.ripe.rpki.commons.crypto.x509cert.X509ResourceCertificate;
import net.ripe.rpki.commons.crypto.x509cert.X509RouterCertificate;
import net.ripe.rpki.commons.validation.ValidationResult;
import net.ripe.rpki.validator3.api.util.InstantWithoutNanos;
import net.ripe.rpki.validator3.storage.Binary;
import net.ripe.rpki.validator3.util.Bench;
import net.ripe.rpki.validator3.util.Sha256;
import org.joda.time.DateTime;

import javax.validation.constraints.NotNull;
import java.math.BigInteger;
import java.util.Optional;

import static net.ripe.rpki.validator3.domain.RpkiObjectUtils.newValidationResult;

@EqualsAndHashCode(callSuper = true)
@Data
@Binary
@ToString(callSuper = true, exclude = "encoded")
public class RpkiObject extends Base<RpkiObject> {

    public static final int MIN_SIZE = 1;
    public static final int MAX_SIZE = 10 * 1024 * 1024;

    public enum Type {
        CER, MFT, CRL, ROA, GBR, ROUTER_CER, OTHER
    }

    @Override
    public Key key() {
        return Key.of(sha256);
    }

    @Getter
    @NotNull
    private Type type;

    private BigInteger serialNumber;

    private InstantWithoutNanos signingTime;

    private byte[] authorityKeyIdentifier;

    @NotNull
    private byte[] sha256;

    @NotNull
    private byte[] encoded;

    public RpkiObject() {
    }

    public RpkiObject(CertificateRepositoryObject object) {
        byte[] encoded = object.getEncoded();
        this.sha256 = Sha256.hash(encoded);
        this.encoded = encoded;
        if (object instanceof X509ResourceCertificate) {
            this.serialNumber = ((X509ResourceCertificate) object).getSerialNumber();
            this.signingTime = null; // Use not valid before instead?
            this.authorityKeyIdentifier = ((X509ResourceCertificate) object).getAuthorityKeyIdentifier();
            this.type = Type.CER; // FIXME separate certificate types? CA, EE, Router, ?
        } else  if (object instanceof X509RouterCertificate) {
            this.serialNumber = ((X509RouterCertificate) object).getSerialNumber();
            this.signingTime = null;
            this.authorityKeyIdentifier = ((X509RouterCertificate) object).getAuthorityKeyIdentifier();
            this.type = Type.ROUTER_CER;
        } else if (object instanceof X509Crl) {
            this.serialNumber = ((X509Crl) object).getNumber();
            final DateTime signingTime = ((X509Crl) object).getThisUpdateTime();
            this.signingTime = signingTime != null ? InstantWithoutNanos.ofEpochMilli(signingTime.getMillis()) : null;
            this.authorityKeyIdentifier = ((X509Crl) object).getAuthorityKeyIdentifier();
            this.type = Type.CRL;
        } else if (object instanceof RpkiSignedObject) {
            this.serialNumber = ((RpkiSignedObject) object).getCertificate().getSerialNumber();
            final DateTime signingTime = ((RpkiSignedObject) object).getSigningTime();
            this.signingTime = signingTime != null ? InstantWithoutNanos.ofEpochMilli(signingTime.getMillis()) : null;
            this.authorityKeyIdentifier = ((RpkiSignedObject) object).getCertificate().getAuthorityKeyIdentifier();
            if (object instanceof ManifestCms) {
                this.type = Type.MFT;
                this.serialNumber = ((ManifestCms) object).getNumber();
            } else if (object instanceof RoaCms) {
                this.type = Type.ROA;
            } else if (object instanceof GhostbustersCms) {
                this.type = Type.GBR;
            } else {
                this.type = Type.OTHER;
            }
        } else if (object instanceof UnknownCertificateRepositoryObject) {
            // FIXME store these at all?
            throw new IllegalArgumentException("unsupported certificate repository object type " + object);
        } else {
            throw new IllegalArgumentException("unsupported certificate repository object type " + object);
        }
    }

    public <T extends CertificateRepositoryObject> Optional<T> get(Class<T> clazz, ValidationResult validationResult) {
        ValidationResult temporary = newValidationResult(validationResult.getCurrentLocation());
        try {
            CertificateRepositoryObject candidate = Bench.mark("createCertificateRepositoryObject", () ->
                    CertificateRepositoryObjectFactory.createCertificateRepositoryObject(
                            encoded,
                            temporary
                    ));

            temporary.rejectIfNull(candidate, "rpki.object.parsable");
            if (temporary.hasFailureForCurrentLocation()) {
                return Optional.empty();
            }

            final boolean isNeededInstance = clazz.isInstance(candidate);
            if (!isNeededInstance) {
                temporary.error( "rpki.object.type.matches", clazz.getSimpleName(), candidate.getClass().getSimpleName());
                if (temporary.hasFailureForCurrentLocation()) {
                    return Optional.empty();
                }
            }

            return Optional.of(clazz.cast(candidate));
        } finally {
            validationResult.addAll(temporary);
        }
    }

    public <T extends CertificateRepositoryObject> Optional<T> get(final Class<T> clazz, final String location) {
        ValidationResult temporary = newValidationResult(location);
        return get(clazz, temporary);
    }

}
