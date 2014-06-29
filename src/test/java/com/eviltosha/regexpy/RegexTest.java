package com.eviltosha.regexpy;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for simple Regex.
 */
public class RegexTest {
  // NOTE: I know that multiple asserts in a single test case is a bad practice, but they test a single aspect
  // in each test case, and use a single regex string, so it's convenient here.
  // Also asserts are lacking messages, because in most cases strings are self-explanatory,
  // and writing additional messages for every assert is unnecessary work
  // TODO: it's probably a good idea to create data files for regex and test strings and simply parse data for tests
  // FIXME: naming conventions

  // Regular expressions components tests
  @Test
  public void testEmptyString() {
    Regex regex = new Regex("");
    assertTrue(regex.match(""));
    assertFalse(regex.match(" "));
  }

  @Test
  public void testExactString() {
    Regex regex = new Regex("abacaba");
    assertTrue(regex.match("abacaba"));
    assertFalse(regex.match("abacab"));
    assertFalse(regex.match("abadaba"));
  }

  @Test
  public void testAsterisk() {
    Regex regex = new Regex("a*");
    assertTrue(regex.match(""));
    assertTrue(regex.match("a"));
    assertTrue(regex.match("aaaaaaaaaaaaaa"));
    assertFalse(regex.match("a*"));
    assertFalse(regex.match("aaaaab"));
  }

  @Test
  public void testPlus() {
    Regex regex = new Regex("c+");
    assertFalse(regex.match(""));
    assertTrue(regex.match("c"));
    assertTrue(regex.match("ccccccccccccccc"));
    assertFalse(regex.match("c+"));
    assertFalse(regex.match("cccccb"));
  }

  @Test
  public void testEmptyGroup() {
    Regex regex = new Regex("()");
    assertTrue(regex.match(""));
    assertFalse(regex.match("()"));
    assertFalse(regex.match("Foo() { bar(123); }"));
  }

  @Test
  public void testSimpleGroup() {
    Regex regex = new Regex("(b*c)d+");
    assertTrue(regex.match("bcd"));
    assertTrue(regex.match("cdd"));
    assertTrue(regex.match("bbbcd"));
    assertFalse(regex.match("dd"));
    assertFalse(regex.match("(bc)d"));
  }

  @Test
  public void testGroupQuantifiers() {
    Regex regex = new Regex("(b*c)*");
    assertTrue(regex.match("bbcbc"));
    assertTrue(regex.match(""));
    assertTrue(regex.match("cc"));
    assertFalse(regex.match("bcb"));
    assertFalse(regex.match("(bc)"));
  }

  @Test
  public void testNestedGroup() {
    Regex regex = new Regex("((c*d)+e)*f");
    assertTrue(regex.match("ccdcddeccdef"));
    assertTrue(regex.match("f"));
    assertTrue(regex.match("def"));
    assertFalse(regex.match("de"));
    assertFalse(regex.match("cef"));
    assertFalse(regex.match("cdedebcef"));
  }

  @Test
  public void testSimpleOr() {
    Regex regex = new Regex("a|b");
    assertTrue(regex.match("a"));
    assertTrue(regex.match("b"));
    assertFalse(regex.match(""));
    assertFalse(regex.match("ab"));
    assertFalse(regex.match("a|b"));
    assertFalse(regex.match("aa"));
  }

  @Test
  public void testSimpleOr_2() {
    Regex regex = new Regex("(b|(c|d)*)+");
    assertTrue(regex.match("b"));
    assertTrue(regex.match("bc"));
    assertTrue(regex.match("bcd"));
    assertTrue(regex.match("bbb"));
    assertTrue(regex.match("bbcbbdbbcd"));
    assertTrue(regex.match("c"));
    assertTrue(regex.match("dd"));
    assertTrue(regex.match(""));
  }

  @Test
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

  @Test
  public void testSimpleRangeQuantifier() {
    Regex regex = new Regex("(ab){2,4}");
    assertTrue(regex.match("abab"));
    assertTrue(regex.match("ababab"));
    assertFalse(regex.match("ab"));
    assertFalse(regex.match("ababababab"));
    assertFalse(regex.match("aab"));
  }

  @Test
  public void testOpenRangeQuantifier() {
    Regex regex = new Regex("(a|b){4,}");
    assertTrue(regex.match("aaaa"));
    assertTrue(regex.match("bbbbaa"));
    assertFalse(regex.match("aaa"));
    assertFalse(regex.match(""));
    assertFalse(regex.match("aabbc"));
  }

  @Test
  public void zeroRangeQuantifierTest() {
    Regex regex = new Regex("a{0,2}");
    assertTrue(regex.match(""));
    assertTrue(regex.match("aa"));
    assertFalse(regex.match("aaa"));
  }

  @Test
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
  @Test
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

  @Test
  public void testRecallAfterQuantifier() {
    Regex regex = new Regex("(\\d|[a-fA-F])*\\1");
    assertTrue(regex.match("11"));
    assertTrue(regex.match("A13BBB"));
    assertFalse(regex.match("12"));
  }

  // TODO: more tests on char ranges (specifically test tricky cases and negation)
  @Test
  public void testCharRange() {
    Regex regex = new Regex("[a-p]+");
    assertTrue(regex.match("abacaba"));
    assertFalse(regex.match("abazaba"));
  }

  @Test
  public void testLettersNumbersHyphens() {
    Regex regex = new Regex("([A-Za-z0-9-]+)");
    assertTrue(regex.match("abacaba"));
    assertTrue(regex.match("S0me-1337-tEXt-w1th-hyphens"));
    assertTrue(regex.match("89123456700"));
    assertFalse(regex.match("+79123456700"));
    assertFalse(regex.match("[123]"));
    assertFalse(regex.match("text with spaces"));
  }

  @Test
  public void testSimpleDot() {
    Regex regex = new Regex("(.)(.)");
    assertTrue(regex.match(" \t"));
    assertTrue(regex.match(".."));
    assertFalse(regex.match("Boobs!"));
    assertFalse(regex.match("a"));
    assertFalse(regex.match(". ."));
  }

  @Test
  public void testQuestionMark() {
    Regex regex = new Regex(".?[abd-wyz]?");
    assertTrue(regex.match(""));
    assertTrue(regex.match("$"));
    assertTrue(regex.match("ab"));
    assertFalse(regex.match("ac"));
    assertFalse(regex.match("abc"));
  }

  // TODO: tests for \D, \s, \S
  @Test
  public void testDates() {
    // Date dd/mm/yyyy, or any mix
    Regex regex = new Regex("(0[1-9]|[1-2]\\d|3[01])/(0[1-9]|1[012])/(\\d{4})");
    assertTrue(regex.match("31/12/2006"));
    assertTrue(regex.match("02/09/0002")); // weird year, I know
    assertFalse(regex.match("2/09/2006"));
    assertFalse(regex.match("32/10/2006"));
    assertFalse(regex.match("02/09/206"));
  }

  // Exceptions tests

  @Test(expected = RegexSyntaxException.class)
  public void testUnclosedRangeQuantifier() {
    Regex regex = new Regex("a{");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testUnclosedOpenGroup() {
    Regex regex = new Regex("a(");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testUnclosedCloseGroup() {
    Regex regex = new Regex("a)");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testEmptyCharRange() {
    Regex regex = new Regex("[]");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testReverseRangeQuantifier() {
    Regex regex = new Regex("a{3,2}");
  }

  // TODO: no exception tests (correct tricky input)

  @Test
  public void testTest() {
    Regex regex = new Regex("(a+b)*");
    assertFalse(regex.match("a"));
  }

  // Various tests

}
