/**
 * Copyright (C) 2012 The named-regexp Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.code.regexp;

import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

/**
 * Tests {@link NamedMatcher}
 */
public class NamedMatcherTest {
    static final String INPUT = "Lorem abcfoo ipsum abcfoo";
    static final String PATT = "(a)(b)(?:c)(?<named>foo)";
    static final NamedPattern P = NamedPattern.compile(PATT);
    NamedMatcher M1;
    NamedMatcher M2;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void beforeTest() {
        M1 = P.matcher(INPUT);
        M2 = P.matcher(INPUT);
    }

    @Test
    public void testFindSucceedsInFindingTaret() {
        assertTrue(P.matcher("abcfoo").find());
    }

    @Test
    public void testFindFailsToFindTarget() {
        assertFalse(P.matcher("hello").find());
    }

    @Test
    public void testStartPositionWithGroupName() {
        NamedMatcher m = P.matcher("abcfooxyz");
        m.find();
        assertEquals(3, m.start("named"));
    }

    @Test
    public void testStartPositionWithGroupIndex() {
        NamedMatcher m = P.matcher("abcfooxyz");
        m.find();
        assertEquals(1, m.start(2)); // 2 = index of (b)
    }

    @Test
    public void testEndPositionWithGroupName() {
        NamedMatcher m = P.matcher("abcfooxyz");
        m.find();
        assertEquals(6, m.end("named"));
    }

    @Test
    public void testEndPositionWithGroupIndex() {
        NamedMatcher m = P.matcher("abcfooxyz");
        m.find();
        assertEquals(2, m.end(2)); // 2 = index of (b)
    }

    @Test
    public void testToMatchResult() {
        NamedMatcher m = P.matcher("abcfoo");

        m.find();
        NamedMatchResult r = m.toMatchResult();
        assertNotNull(r);

        assertEquals("foo", r.group("named"));

        assertEquals(0, r.start());
        assertEquals(3, r.start("named"));
        assertEquals(6, r.end());
        assertEquals(6, r.end("named"));
    }

    @Test
    public void testUsePatternSetsUnderlyingPattern() {
        NamedMatcher m = P.matcher("xyzabcfooabcfoo");
        m.find();
        assertEquals(3, m.start());
        m.find();
        assertEquals(9, m.start());

        // no more matches, m.find() should return false
        assertFalse(m.find());

        // change the pattern so that it matches the chars
        // at the beginning of the string; make sure it
        // doesn't match the previous pattern (which is
        // in the middle of the string)
        m.usePattern(NamedPattern.compile("xy(?<named>z)"));
        m.reset();
        m.find();
        assertEquals(0, m.start());
    }

    @Test( timeout = 5000 )
    public void testResetForcesFindFromBeginning() {
        NamedMatcher m = P.matcher("abcfooabcfoo");

        // advance find to last match; the test's timeout
        // protects from accidental infinite loop
        while(m.find()) ;

        // resetting should force m.find() to search from beginning
        m.reset();
        m.find();
        assertEquals(0, m.start());
    }

    @Test( timeout = 5000 )
    public void testResetCharSequence() {
        NamedMatcher m = P.matcher("abcfooabcx");

        // make sure at least one match is found and then advance
        // find to the end; the test's timeout protects from
        // infinite loop
        assertTrue(m.find());
        while(m.find()) ;

        m.reset("dummy.*pattern");
        assertFalse(m.find());

        // move matching pattern to a diff position than original
        m.reset("hello world abcfoo foo bar");
        assertTrue(m.find());
        assertEquals(12, m.start());
    }

    @Test
    public void testNoMatchesForNamedGroup() {
        NamedMatcher m = P.matcher("abcd");
        assertFalse(m.find());

        // throws IllegalStateException("No match found")
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("No match found");
        m.group("named");
    }

    @Test
    public void testNoMatchesForInvalidGroupName() {
        NamedMatcher m = P.matcher("abcd");
        assertFalse(m.find());

        // throws IllegalStateException("No match found")
        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("No match found");
        m.group("nonexistentName");
    }

    @Test
    public void testNamedGroupAfterUnnamedAndNoncaptureGroups() {
        NamedPattern p = NamedPattern.compile("(a)(b)(?:c)(?<named>x)");
        NamedMatcher m = p.matcher("abcx");
        m.find();
        assertEquals("x", m.group("named"));
    }

    @Test
    public void testNamedGroupAfterUnnamedGroups() {
        NamedPattern p = NamedPattern.compile("(?:c)(?<named>x)");
        NamedMatcher m = p.matcher("abcx");
        m.find();
        assertEquals("x", m.group("named"));
    }

    @Test
    public void testNamedGroupAfterNoncaptureGroups() {
        NamedPattern p = NamedPattern.compile("(?:c)(?<named>x)");
        NamedMatcher m = p.matcher("abcx");
        m.find();
        assertEquals("x", m.group("named"));
    }

    @Test
    public void testNamedGroupOnly() {
        NamedPattern p = NamedPattern.compile("(?<named>x)");
        NamedMatcher m = p.matcher("abcx");
        m.find();
        assertEquals("x", m.group("named"));
    }

    @Test
    public void testMatchNamedGroupAfterAnotherNamedGroup() {
        NamedPattern p = NamedPattern.compile("(a)(?<foo>b)(?:c)(?<named>x)");
        NamedMatcher m = p.matcher("abcx");
        m.find();
        assertEquals("x", m.group("named"));
    }

    @Test
    public void testIndexOfNestedNamedGroup() {
        NamedPattern p = NamedPattern.compile("(a)(?<foo>b)(?:c)(?<bar>d(?<named>x))");
        NamedMatcher m = p.matcher("abcdx");
        m.find();
        assertEquals("x", m.group("named"));
    }

    @Test
    public void testOrderedGroupsHasMatchesInOrder() {
        NamedPattern p = NamedPattern.compile("(a)(b)(?:c)(?<named>foo)");
        NamedMatcher m = p.matcher("abcfoo");
        m.find();
        List<String> matches = m.orderedGroups();
        assertEquals(3, matches.size());
        assertEquals("a", matches.get(0));
        assertEquals("b", matches.get(1));
        assertEquals("foo", matches.get(2));
    }

    @Test
    public void testNamedGroupsDoesNotThrowIndexOutOfBounds() {
        // NamedMatcher.namedGroups() is used to get a map of
        // group names to group values. This should ignore unnamed
        // groups (exclude them from the map), but the unnamed
        // groups were throwing off the function, causing it to
        // fetch a named group at a non-existent index.
        // See Issue #1
        NamedPattern p = NamedPattern.compile("(a)(?<foo>b)(?:c)(?<bar>d(?<named>x))");
        NamedMatcher m = p.matcher("abcdx");
        try {
            m.namedGroups();
            // verified here: IndexOutOfBoundsException did not occur
        } catch (IndexOutOfBoundsException e) {
            fail("IndexOutOfBoundsException should have been fixed");
        }
    }

    @Test
    public void testNamedGroupsGetsOnlyNamedGroups() {
        NamedPattern p = NamedPattern.compile("(a)(?<foo>b)(?:c)(?<bar>d(?<named>x))");
        NamedMatcher m = p.matcher("abcdxyz");

        Map<String, String> map = m.namedGroups();
        assertEquals(3, map.size());
        assertEquals("b", map.get("foo"));
        assertEquals("dx", map.get("bar"));
        assertEquals("x", map.get("named"));
    }

    @Test
    public void testNamedGroupsWithNoMatchGetsEmptyMap() {
        NamedPattern p = NamedPattern.compile("(a)(?<foo>b)(?:c)(?<bar>d(?<named>x))");
        NamedMatcher m = p.matcher("nada");

        Map<String, String> map = m.namedGroups();
        assertEquals(0, map.size());
    }

    @Test
    public void testStandardPatternGetsOrigWithoutNamed() {
        final String PATT_W_NO_NAMED_GRPS = "(a)(b)(?:c)(foo)";
        NamedMatcher m = P.matcher("abcfoo");
        assertEquals(PATT_W_NO_NAMED_GRPS, m.standardPattern().pattern());
    }

    @Test
    public void testNamedPatternCallGetsOriginalInstance() {
        assertEquals(P, P.matcher("abcfoo").namedPattern());
    }

    @Test
    public void testMatchesCallReturnsTrueForMatch() {
        assertTrue(P.matcher("abcfoo").matches());
    }

    @Test
    public void testMatchesCallReturnsFalseForMismatch() {
        assertFalse(P.matcher("foo").matches());
    }

    @Test
    public void testFindCallReturnsTrueForMatchFromBeginning() {
        assertTrue(P.matcher("abcfoo").find(0));
    }

    @Test
    public void testFindCallReturnsFalseForMismatchFromBeginning() {
        assertFalse(P.matcher("foo").find(0));
    }

    @Test
    public void testFindCallReturnsTrueForMatchFromMiddle() {
        assertTrue(P.matcher("Lorem ipsum abcfoo dolor sit amet").find(5));
    }

    @Test
    public void testFindCallReturnsFalseForMismatchFromMiddle() {
        assertFalse(P.matcher("Lorem ipsum abcXfoo dolor sit amet").find(5));
    }

    @Test
    public void testFindCallReturnsTrueForMatchFromEnd() {
        assertTrue(P.matcher("Lorem ipsum abcfoo").find(12));
    }

    @Test
    public void testFindCallReturnsFalseForMismatchFromEnd() {
        assertFalse(P.matcher("Lorem ipsum abcXfoo").find(12));
    }

    @Test
    public void testLookingAtCallReturnsTrueWhenAtMatchingText() {
        assertTrue(P.matcher("abcfoo Lorem ipsum").lookingAt());
    }

    @Test
    public void testLookingAtCallReturnsFalseWhenAtMismatchingText() {
        assertFalse(P.matcher("Lorem abcx ipsum").lookingAt());
    }

    @Test
    public void testEqualsReturnsTrueForSameMatcher() {
        assertTrue(M1.equals(M1));
    }

    @Test
    public void testEqualsReturnsFalseForTwoMatchersWithIdenticalValues() {
        assertFalse(M1.equals(M2));
    }

    @Test
    public void testEqualsReturnsFalseForTwoMatchersWithDifferentValues() {
        NamedMatcher m2 = P.matcher("foo bar");
        assertFalse(M1.equals(m2));
    }

    @Test
    public void testEqualsReturnsFalseForTwoMatcherWithDifferentParentPatterns() {
        NamedPattern p2 = NamedPattern.compile("(a)(b)(?:c)(?<named>x)");
        NamedMatcher m1 = P.matcher("Lorem abcx ipsum");
        NamedMatcher m2 = p2.matcher("Lorem abcx ipsum");
        assertFalse(m1.equals(m2));
    }

    @Test
    public void testEqualsReturnsFalseWhenComparedWithNull() {
        assertFalse(M1.equals(null));
    }

    @Test
    public void testEqualsReturnsFalseWhenComparedWithDifferentDataType() {
        assertFalse(M1.equals(new Object()));
    }

    @Test
    public void testHashCodeGetsUniqueHashForTwoMatchersWithIdenticalValues() {
        assertFalse(M1.hashCode() == M2.hashCode());
    }

    @Test
    public void testHashCodeGetsUniqueHashForTwoMatchersWithDifferentValues() {
        NamedMatcher m2 = P.matcher("foo bar");
        assertFalse(M1.hashCode() == m2.hashCode());
    }

    @Test
    public void testHashCodeGetsUniqueHashForTwoMatchersWithDifferentParentPatterns() {
        NamedPattern p2 = NamedPattern.compile("foo bar");
        NamedMatcher m2 = p2.matcher("Lorem abcx ipsum");
        assertFalse(M1.hashCode() == m2.hashCode());
    }

    @Test
    public void testUsePatternNullThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("newPattern cannot be null");
        M1.usePattern(null);
    }

    @Test
    public void testAppendReplacementReturnsOrigInstance() {
        M1.find();
        assertEquals(M1, M1.appendReplacement(new StringBuffer("foo"), "bar"));
    }

    @Test
    public void testAppendReplacementReplacesMatchAndAppendsToBuffer() {
        // M1.find() must be called before appendReplacement(), which replaces
        // the found match with a given string and appends the result to a
        // given buffer. Note the result is a substring of the full string,
        // from the beginning of the string up to the last character of the
        // replacement string.
        StringBuffer sb = new StringBuffer("origText ");
        M1.find();
        M1.appendReplacement(sb, "foo");
        assertEquals("origText Lorem foo", sb.toString());
        M1.find();
        M1.appendReplacement(sb, "bar");
        assertEquals("origText Lorem foo ipsum bar", sb.toString());
    }

    @Test
    public void testAppendReplacementWithNamedRefs() {
        StringBuffer sb = new StringBuffer("origText ");
        M1.find();
        M1.appendReplacement(sb, "${named}foo ${named}bar");
        assertEquals("origText Lorem foofoo foobar", sb.toString());
    }

    @Test
    public void testAppendReplacementWithInvalidNamedRefs() {
        thrown.expect(PatternSyntaxException.class);
        thrown.expectMessage("unknown group name near index 2\n" +
                             "${nonexistentName} foobar!\n" +
                             "  ^");
        M1.appendReplacement(new StringBuffer(), "${nonexistentName} foobar!");
    }

    @Test
    public void testAppendTailAppendsRemainderToBuffer() {
        StringBuffer sb = new StringBuffer("origText ");
        M1.find();
        M1.appendReplacement(sb, "foo");

        // appendTail() should append the rest of the matcher
        // string to the buffer. Even if the string contains
        // a match, the match must not be replaced.
        M1.appendTail(sb);
        assertEquals("origText Lorem foo ipsum abcfoo", sb.toString());
    }

    @Test
    public void testGroupGetsTheMatchingText() {
        M1.find();
        assertEquals("abcfoo", M1.group());
    }

    @Test
    public void testRegionInvalidStartIndexThrowsException() {
        thrown.expect(IndexOutOfBoundsException.class);
        thrown.expectMessage("start");
        M1.region(-100, 1);
        M1.region(1000000000, 1);
    }

    @Test
    public void testRegionInvalidEndIndexThrowsException() {
        thrown.expect(IndexOutOfBoundsException.class);
        thrown.expectMessage("end");
        M1.region(0, -100);
        M1.region(0, 1000000000);
    }

    @Test
    public void testRegionSetsLimitsForMatcher() {
        int firstPos = INPUT.indexOf("abcfoo");
        int lastPos = INPUT.lastIndexOf("abcfoo");

        NamedMatcher m = M1.region(firstPos + 1, INPUT.length());
        assertTrue(m.find());
        assertTrue(firstPos != lastPos);
        assertEquals(lastPos, m.start());
        assertEquals(lastPos + "abcfoo".length(), m.end());
    }

    @Test
    public void testRegionStartGetsStartPosOfRegion() {
        assertEquals(2, M1.region(2, 3).regionStart());
    }

    @Test
    public void testRegionEndGetsEndPosOfRegion() {
        assertEquals(3, M1.region(2, 3).regionEnd());
    }

    @Test( timeout = 5000 )
    public void testHitEndGetsTrueWhenNoMoreMatches() {
        // the test's timeout protects from infinite loop
        while (M1.find()) ;
        assertTrue(M1.hitEnd());
    }

    @Test
    public void testHitEndGetsFalseWhenMoreMatches() {
        assertFalse(M1.hitEnd());
    }

    @Test
    public void testRequireEnd() {
        // requireEnd is normally false (see grepcode OpenJDK)
        assertFalse(M1.requireEnd());
    }

    @Test
    public void testHasTransparentBoundsTrueWhenUseTransTrue() {
        assertTrue(M1.useTransparentBounds(true).hasTransparentBounds());
    }

    @Test
    public void testHasTransparentBoundsFalseWhenUseTransFalse() {
        assertFalse(M1.useTransparentBounds(false).hasTransparentBounds());
    }

    @Test
    public void testUseTransparentBounds() {
        String text  = "Madagascar is best seen by car or bike.";
        NamedMatcher m = NamedPattern.compile("\\b(?<target>car)\\b").matcher(text);

        // Set starting bound to char 7 in "Madagascar" (which is "car")
        // and then try to find the "car" word toward the end. Without
        // transparent bounds, the search will find "car" at the beginning
        // because the matcher can't see beyond the bounds to determine
        // that this is not a word boundary (from "\\b" in regex).
        m.region(7, text.length()).find();
        assertEquals(7, m.start());

        m.reset().useTransparentBounds(true).find();
        assertEquals(27, m.start());
    }

    @Test
    public void testHasAnchoringBoundsTrueWhenUseAnchorTrue() {
        assertTrue(M1.useAnchoringBounds(true).hasAnchoringBounds());
    }

    @Test
    public void testHasAnchoringBoundsFalseWhenUseAnchorFalse() {
        assertFalse(M1.useAnchoringBounds(false).hasAnchoringBounds());
    }

    @Test
    public void testUseAnchoringBounds() {
        String text  = "The fox jumped over the white picket fence.";
        NamedMatcher m = NamedPattern.compile("^(?<target>fox jumped) over").matcher(text);

        // setting the starting region to "fox" will make
        // the matcher think that it's found the target
        // since "fox" is at the beginning of its search
        // region (only true when anchoring bounds false)
        m.region(4, text.length());
        assertTrue(m.find());
        assertEquals(4, m.start());

        // setting the anchoring bounds to false lets the
        // matcher realize the target is not actually at
        // the beginning of the string
        assertFalse(m.reset().useAnchoringBounds(false).find());
    }

    @Test
    public void testReplaceAll() {
        assertEquals("Lorem xyz ipsum xyz", M1.replaceAll("xyz"));
    }

    @Test
    public void testReplaceAllWithNamedRefs() {
        assertEquals("Lorem foo@foo# ipsum foo@foo#", M1.replaceAll("${named}@${named}#"));
    }

    @Test
    public void testReplaceAllWithInvalidNamedRefs() {
        thrown.expect(PatternSyntaxException.class);
        thrown.expectMessage("unknown group name near index 2\n" +
                             "${nonexistentName} foobar!\n" +
                             "  ^");
        M1.replaceAll("${nonexistentName} foobar!");
    }

    @Test
    public void testReplaceFirst() {
        assertEquals("Lorem xyz ipsum abcfoo", M1.replaceFirst("xyz"));
    }

    @Test
    public void testReplaceFirstWithNamedRefs() {
        assertEquals("Lorem foo@foo# ipsum abcfoo", M1.replaceFirst("${named}@${named}#"));
    }

    @Test
    public void testReplaceFirstWithInvalidNamedRefs() {
        thrown.expect(PatternSyntaxException.class);
        thrown.expectMessage("unknown group name near index 2\n" +
                             "${nonexistentName} foobar!\n" +
                             "  ^");
        M1.replaceFirst("${nonexistentName} foobar!");
    }

    @Test
    public void testToString() {
        assertNotNull(M1.toString());
        assertTrue(M1.toString().trim().length() > 0);
    }

    @Test
    public void testBackrefMatches() {
        NamedPattern p = NamedPattern.compile("(?<a>xyz)(?<num>\\d+)abc\\k<num>def");
        NamedMatcher m = p.matcher("xyz12345abc12345def");
        assertTrue(m.find());
        assertEquals("12345", m.group("num"));
    }

    @Test
    public void testBackrefNoMatch() {
        NamedPattern p = NamedPattern.compile("(?<a>xyz)(?<num>\\d+)abc\\k<num>def");
        // this should not match because the 2nd number is not equal
        // to the first captured number
        assertFalse(p.matcher("xyz12345abc123456def").find());
    }
}
