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
package net.ripe.rpki.validator3.storage.stores.impl;

import com.google.common.collect.ImmutableMap;
import net.ripe.rpki.validator3.api.Paging;
import net.ripe.rpki.validator3.api.SearchTerm;
import net.ripe.rpki.validator3.api.Sorting;
import net.ripe.rpki.validator3.domain.constraints.ValidLocationURI;
import net.ripe.rpki.validator3.storage.FSTCoder;
import net.ripe.rpki.validator3.storage.Lmdb;
import net.ripe.rpki.validator3.storage.data.RpkiRepository;
import net.ripe.rpki.validator3.storage.data.TrustAnchor;
import net.ripe.rpki.validator3.storage.lmdb.IxMap;
import net.ripe.rpki.validator3.storage.stores.RpkiRepostioryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class LmdbRpkiRepostiories implements RpkiRepostioryStore {

    private static final String RPKI_REPOSITORIES = "rpki-repositories";

    private final IxMap<RpkiRepository> ixMap;

    @Autowired
    public LmdbRpkiRepostiories(Lmdb lmdb) {
        this.ixMap = new IxMap<>(
                lmdb.getEnv(),
                RPKI_REPOSITORIES,
                new FSTCoder<>(),
                ImmutableMap.of());
    }

    @Override
    public RpkiRepository register(TrustAnchor trustAnchor, String uri, RpkiRepository.Type type) {
        return null;
    }

    @Override
    public Optional<RpkiRepository> findByURI(@NotNull @ValidLocationURI String uri) {
        return Optional.empty();
    }

    @Override
    public RpkiRepository get(long id) {
        return null;
    }

    @Override
    public Stream<RpkiRepository> findAll(RpkiRepository.Status optionalStatus, Long taId, boolean hideChildrenOfDownloadedParent, SearchTerm searchTerm, Sorting sorting, Paging paging) {
        return null;
    }

    @Override
    public long countAll(RpkiRepository.Status optionalStatus, Long taId, boolean hideChildrenOfDownloadedParent, SearchTerm searchTerm) {
        return 0;
    }

    @Override
    public Map<RpkiRepository.Status, Long> countByStatus(Long taId, boolean hideChildrenOfDownloadedParent) {
        return null;
    }

    @Override
    public Stream<RpkiRepository> findRsyncRepositories() {
        return null;
    }

    @Override
    public Stream<RpkiRepository> findRrdpRepositories() {
        return null;
    }

    @Override
    public void removeAllForTrustAnchor(TrustAnchor trustAnchor) {

    }

    @Override
    public void remove(long id) {

    }
}
