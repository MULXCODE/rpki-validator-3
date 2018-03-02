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
package net.ripe.rpki.validator3.api.bgp;

import lombok.extern.slf4j.Slf4j;
import net.ripe.ipresource.Asn;
import net.ripe.ipresource.IpRange;
import net.ripe.ipresource.UniqueIpResource;
import net.ripe.rpki.validator3.util.Http;
import org.eclipse.jetty.client.HttpClient;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

@Component
@Slf4j
public class BgpRisDownloader {

//    private static List<String> DEFAULT_URLS = Arrays.asList(
//            "http://www.ris.ripe.net/dumps/riswhoisdump.IPv4.gz",
//            "http://www.ris.ripe.net/dumps/riswhoisdump.IPv6.gz");

    private HttpClient httpClient;

    @PostConstruct
    public void postConstruct() throws Exception {
        httpClient = new HttpClient();
        httpClient.start();
    }

    public BgpRisDump download(String uri) {
        final List<BgpRisEntry> entries = Http.readStream(httpClient, uri, s -> {
            try {
                return parse(new GZIPInputStream(s));
            } catch (Exception e) {
                log.error("Error downloading RIS dump: " + uri);
                return Collections.emptyList();
            }
        });
        return BgpRisDump.of(uri, DateTime.now(), entries);
    }

    /*
        TODO Make it return Stream<BgpRisEntry> and don't allocate the whole list for that.
     */
    public List<BgpRisEntry> parse(final InputStream is) {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(is));

        String line = null;
        final List<BgpRisEntry> entries = new ArrayList<>();
        final IdentityMap id = new IdentityMap();
        do {
            try {
                line = reader.readLine();
                final BgpRisEntry e = parseLine(line, id::unique);
                if (e != null) {
                    entries.add(e);
                }
            } catch (Exception e) {
                log.error("Unparseable line: " + line);
            }
        } while (line != null);

        return entries;
    }

    private static Pattern regexp = Pattern.compile("^\\s*([0-9]+)\\s+([0-9a-fA-F.:/]+)\\s+([0-9]+)\\s*$");

    private BgpRisEntry parseLine(final String line, final Function<Object, Object> uniq) {
        final Matcher matcher = regexp.matcher(line);
        if (matcher.matches()) {
            final Asn asn = (Asn) uniq.apply(Asn.parse(matcher.group(1)));
            IpRange parsed = IpRange.parse(matcher.group(2));
            final UniqueIpResource start = (UniqueIpResource) uniq.apply(parsed.getStart());
            final UniqueIpResource end = (UniqueIpResource) uniq.apply(parsed.getEnd());
            final IpRange prefix = (IpRange) start.upTo(end);
            final int visibility = Integer.parseInt(matcher.group(3));
            return BgpRisEntry.of(asn, prefix, visibility);
        }
        return null;
    }

    // This is to avoid distinct object instances for objects that are equal
    private static class IdentityMap {
        private Map<Object, Object> unique = new HashMap<>();

        Object unique(final Object o) {
            final Object u = unique.get(o);
            if (u == null) {
                unique.put(o, o);
                return o;
            }
            return u;
        }
    }

    private String formatAsRFC2616(DateTime d) {
        return d.toDateTime(DateTimeZone.UTC).toString("EEE, dd MMM yyyy HH:mm:ss ZZZ");
    }
}
