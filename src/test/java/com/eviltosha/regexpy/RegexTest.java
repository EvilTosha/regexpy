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
}
