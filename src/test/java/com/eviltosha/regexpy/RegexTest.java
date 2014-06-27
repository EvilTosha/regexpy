package com.eviltosha.regexpy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple Regex.
 */
public class RegexTest
    extends TestCase {
  /**
   * Create the test case
   *
   * @param testName name of the test case
   */
  public RegexTest(String testName) {
    super(testName);
  }

  /**
   * @return the suite of tests being tested
   */
  public static Test suite() {
    return new TestSuite(RegexTest.class);
  }

  // TODO: Exceptions tests (keyword "expected")

  /**
   * Rigourous Test :-)
   */
  // TODO: check all tests with java.util.regexp
  // FIXME: naming conventions
  public void testEmptyString() {
    Regex regex = new Regex("");
    assertTrue(regex.match(""));
    assertFalse(regex.match("a"));
    assertFalse(regex.match(" "));
  }

  public void testExactString() {
    Regex regex = new Regex("abacaba");
    assertTrue(regex.match("abacaba"));
    assertFalse(regex.match("abacab"));
    assertFalse(regex.match("abcaba"));
    assertFalse(regex.match("abadaba"));
  }

  public void testAsterisk() {
    Regex regex = new Regex("a*");
    assertTrue(regex.match(""));
    assertTrue(regex.match("a"));
    assertTrue(regex.match("aaaaaaaaaaaaaa"));
    assertFalse(regex.match("a*"));
    assertFalse(regex.match("b"));
    assertFalse(regex.match("baaaaa"));
    assertFalse(regex.match("aaaaab"));
    assertFalse(regex.match("apple pie"));
  }

  public void testPlus() {
    Regex regex = new Regex("c+");
    assertFalse(regex.match(""));
    assertTrue(regex.match("c"));
    assertTrue(regex.match("ccccccccccccccc"));
    assertFalse(regex.match("c+"));
    assertFalse(regex.match("b"));
    assertFalse(regex.match("bccccc"));
    assertFalse(regex.match("cccccb"));
    assertFalse(regex.match("cast the spell!"));
  }

  public void testEmptyGroup() {
    Regex regex = new Regex("()");
    assertTrue(regex.match(""));
    assertFalse(regex.match("()"));
    assertFalse(regex.match("Lorem ipsum"));
    assertFalse(regex.match("Foo() { bar(123); }"));
  }

  public void testSimpleGroup() {
    Regex regex = new Regex("a(b*c)d+");
    assertTrue(regex.match("abcd"));
    assertTrue(regex.match("acdd"));
    assertTrue(regex.match("abbbcd"));
    assertFalse(regex.match("add"));
    assertFalse(regex.match("a(bc)d"));
    assertFalse(regex.match("a(b*c)d+"));
  }

  public void testGroupQuantifiers() {
    Regex regex = new Regex("a(b*c)*d+");
    assertTrue(regex.match("abbcbcdd"));
    assertTrue(regex.match("add"));
    assertTrue(regex.match("accd"));
    assertFalse(regex.match("a(b*c)*d+"));
    assertFalse(regex.match("abbd"));
    assertFalse(regex.match("abcbdd"));
    assertFalse(regex.match("a(bc)d"));
    assertFalse(regex.match("abccbd"));
  }

  public void testMultiGroup() {
    Regex regex = new Regex("(b*c)*d()e(fg+)*");
    assertTrue(regex.match("de"));
    assertTrue(regex.match("defg"));
    assertTrue(regex.match("cdefgggfg"));
    assertTrue(regex.match("cbcbbcde"));
    assertFalse(regex.match("bbcdefgf"));
  }

  public void testNestedGroup() {
    Regex regex = new Regex("a(b+(c*d)+e)*f");
    assertTrue(regex.match("abbccdcddebccdef"));
    assertTrue(regex.match("af"));
    assertTrue(regex.match("abdef"));
    assertFalse(regex.match("abde"));
    assertFalse(regex.match("abcef"));
    assertFalse(regex.match("abcdebdebcef"));
  }

  public void testSimpleOr() {
    Regex regex = new Regex("a|b");
    assertTrue(regex.match("a"));
    assertTrue(regex.match("b"));
    assertFalse(regex.match("ab"));
    assertFalse(regex.match("a|b"));
    assertFalse(regex.match("aa"));
    assertFalse(regex.match("bb"));
    assertFalse(regex.match("ba"));
  }

  public void testSimpleOr_2() {
    Regex regex = new Regex("a(b|(c|d)*)+e");
    assertTrue(regex.match("abe"));
    assertTrue(regex.match("abce"));
    assertTrue(regex.match("abcde"));
    assertTrue(regex.match("abbbe"));
    assertTrue(regex.match("abcbde"));
    assertTrue(regex.match("abbcbbdbbcde"));
    assertTrue(regex.match("ace"));
    assertTrue(regex.match("adde"));
    assertTrue(regex.match("accdddbbe"));
    assertTrue(regex.match("ae"));
    assertFalse(regex.match("abfe"));
  }

  // TODO: multi-digit range
  public void testExactRangeQuantifier() {
    Regex regex = new Regex("a{3}");
    assertTrue(regex.match("aaa"));
    assertFalse(regex.match("aa"));
    assertFalse(regex.match(""));
    assertFalse(regex.match("aaaa"));
    assertFalse(regex.match("a{3}"));
    assertFalse(regex.match("aaab"));
  }

  public void testSimpleRangeQuantified() {
    Regex regex = new Regex("(ab){2,3}");
    assertTrue(regex.match("abab"));
    assertTrue(regex.match("ababab"));
    assertFalse(regex.match("ab"));
    assertFalse(regex.match("ababababab"));
    assertFalse(regex.match("abb"));
    assertFalse(regex.match("aaab"));
    assertFalse(regex.match("aabab"));
    assertFalse(regex.match("ababb"));
    assertFalse(regex.match("(ab){2, 3}"));
  }

  public void testOpenRangeQuantifier() {
    Regex regex = new Regex("(a|b){4,}");
    assertTrue(regex.match("aaaa"));
    assertTrue(regex.match("ababab"));
    assertTrue(regex.match("bbbbaaa"));
    assertTrue(regex.match("bbaa"));
    assertFalse(regex.match("a{4}"));
    assertFalse(regex.match("aaa"));
    assertFalse(regex.match("aba"));
    assertFalse(regex.match(""));
    assertFalse(regex.match("aabbc"));
  }

  public void testSimpleGroupRecall() {
    Regex regex = new Regex("(a|b)\\1");
    assertTrue(regex.match("aa"));
    assertTrue(regex.match("bb"));
    assertFalse(regex.match("ab"));
    assertFalse(regex.match("ba"));
    assertFalse(regex.match("a1"));
    assertFalse(regex.match("a\\1"));
  }

  // TODO: multi-digit group recall
  public void testMultipleGroupRecall() {
    Regex regex = new Regex("(a+(b|c*)\\2)\\2\\1");
    assertTrue(regex.match("aa"));
    assertTrue(regex.match("aaccccccaacccc"));
    assertTrue(regex.match("abbbabb"));
    assertFalse(regex.match("abcca"));
    assertFalse(regex.match("aca"));
    assertFalse(regex.match("abbb"));
    assertFalse(regex.match("abbbacc"));
  }
}
