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

}
