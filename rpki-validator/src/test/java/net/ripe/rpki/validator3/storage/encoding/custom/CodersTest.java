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
package net.ripe.rpki.validator3.storage.encoding.custom;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.math.BigInteger;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JUnitQuickcheck.class)
public class CodersTest {
    @Property
    public void testBoolean(boolean b) {
        assertEquals(b, Coders.toBoolean(Coders.toBytes(b)));
    }

    @Property
    public void testInt(int b) {
        assertEquals(b, Coders.toInt(Coders.toBytes(b)));
    }

    @Property
    public void testLong(long b) {
        assertEquals(b, Coders.toLong(Coders.toBytes(b)));
    }

    @Property
    public void testString(String b) {
        assertEquals(b, Coders.toString(Coders.toBytes(b)));
    }

    @Property
    public void testBigInteger(BigInteger b) {
        assertEquals(b, Coders.toBigInteger(Coders.toBytes(b)));
    }

    @Property
    public void testListString(List<String> b) {
        assertEquals(b, Coders.fromBytes(Coders.toBytes(b, Coders::toBytes), Coders::toString));
    }
    @Property
    public void testListStringWithNulls(List<String> b, List<Integer> nulled) {
        for(Integer idx : nulled){
            int safeIdx = Math.abs(idx) % b.size();
            b.set(safeIdx, null);
        }
        assertEquals(b, Coders.fromBytes(Coders.toBytes(b, Coders::toBytes), Coders::toString));
    }
}