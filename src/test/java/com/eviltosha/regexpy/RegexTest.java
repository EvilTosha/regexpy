package com.eviltosha.regexpy;

import org.junit.Ignore;
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
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(""));
    assertFalse(matcher.match(" "));
  }

  @Test
  public void testExactString() {
    Regex regex = new Regex("abacaba");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("abacaba"));
    assertFalse(matcher.match("abacab"));
    assertFalse(matcher.match("abadaba"));
  }

  @Test
  public void testAsterisk() {
    Regex regex = new Regex("a*");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(""));
    assertTrue(matcher.match("a"));
    assertTrue(matcher.match("aaaaaaaaaaaaaa"));
    assertFalse(matcher.match("a*"));
    assertFalse(matcher.match("aaaaab"));
  }

  @Test
  public void testPlus() {
    Regex regex = new Regex("c+");
    Matcher matcher = regex.matcher();
    assertFalse(matcher.match(""));
    assertTrue(matcher.match("c"));
    assertTrue(matcher.match("ccccccccccccccc"));
    assertFalse(matcher.match("c+"));
    assertFalse(matcher.match("cccccb"));
  }

  @Test
  public void testEmptyGroup() {
    Regex regex = new Regex("()");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(""));
    assertFalse(matcher.match("()"));
    assertFalse(matcher.match("Foo() { bar(123); }"));
  }

  @Test
  public void testSimpleGroup() {
    Regex regex = new Regex("(b*c)d+");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("bcd"));
    assertTrue(matcher.match("cdd"));
    assertTrue(matcher.match("bbbcd"));
    assertFalse(matcher.match("dd"));
    assertFalse(matcher.match("(bc)d"));
  }

  @Test
  public void testGroupQuantifiers() {
    Regex regex = new Regex("(b*c)*");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("bbcbc"));
    assertTrue(matcher.match(""));
    assertTrue(matcher.match("cc"));
    assertFalse(matcher.match("bcb"));
    assertFalse(matcher.match("(bc)"));
  }

  @Test
  public void testNestedGroup() {
    Regex regex = new Regex("((c*d)+e)*f");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("ccdcddeccdef"));
    assertTrue(matcher.match("f"));
    assertTrue(matcher.match("def"));
    assertFalse(matcher.match("de"));
    assertFalse(matcher.match("cef"));
    assertFalse(matcher.match("cdedebcef"));
  }

  @Test
  public void testSimpleOr() {
    Regex regex = new Regex("a|b");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("a"));
    assertTrue(matcher.match("b"));
    assertFalse(matcher.match(""));
    assertFalse(matcher.match("ab"));
    assertFalse(matcher.match("a|b"));
    assertFalse(matcher.match("aa"));
  }

  @Test
  public void testSimpleOr_2() {
    Regex regex = new Regex("(b|(c|d)*)+");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("b"));
    assertTrue(matcher.match("bc"));
    assertTrue(matcher.match("bcd"));
    assertTrue(matcher.match("bbb"));
    assertTrue(matcher.match("bbcbbdbbcd"));
    assertTrue(matcher.match("c"));
    assertTrue(matcher.match("dd"));
    assertTrue(matcher.match(""));
  }

  @Test
  public void testOrEmptyClause() {
    Regex regex = new Regex("(|a)(||c||)");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(""));
    assertTrue(matcher.match("c"));
    assertTrue(matcher.match("ac"));
  }

  @Test
  // TODO: multi-digit range
  public void testExactRangeQuantifier() {
    Regex regex = new Regex("a{3}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("aaa"));
    assertFalse(matcher.match("aa"));
    assertFalse(matcher.match(""));
    assertFalse(matcher.match("aaaa"));
    assertFalse(matcher.match("a{3}"));
    assertFalse(matcher.match("aaab"));
  }

  @Test
  public void testSimpleRangeQuantifier() {
    Regex regex = new Regex("(ab){2,4}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("abab"));
    assertTrue(matcher.match("ababab"));
    assertFalse(matcher.match("ab"));
    assertFalse(matcher.match("ababababab"));
    assertFalse(matcher.match("aab"));
  }

  @Test
  public void testOpenRangeQuantifier() {
    Regex regex = new Regex("(a|b){4,}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("aaaa"));
    assertTrue(matcher.match("bbbbaa"));
    assertFalse(matcher.match("aaa"));
    assertFalse(matcher.match(""));
    assertFalse(matcher.match("aabbc"));
  }

  @Test
  public void testZeroRangeQuantifier() {
    Regex regex = new Regex("a{0,2}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(""));
    assertTrue(matcher.match("aa"));
    assertFalse(matcher.match("aaa"));
  }

  @Test
  public void testSimpleGroupRecall() {
    Regex regex = new Regex("(a|b)\\1");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("aa"));
    assertTrue(matcher.match("bb"));
    assertFalse(matcher.match("ab"));
    assertFalse(matcher.match("ba"));
    assertFalse(matcher.match("a1"));
    assertFalse(matcher.match("a\\1"));
  }

  // TODO: multi-digit group recall
  @Test
  public void testMultipleGroupRecall() {
    Regex regex = new Regex("(a+(b|c*)\\2)\\2\\1");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("aa"));
    assertTrue(matcher.match("aaccccccaacccc"));
    assertTrue(matcher.match("abbbabb"));
    assertFalse(matcher.match("abcca"));
    assertFalse(matcher.match("aca"));
    assertFalse(matcher.match("abbb"));
    assertFalse(matcher.match("abbbacc"));
  }

  @Test
  public void testRecallAfterQuantifier() {
    Regex regex = new Regex("(\\d|[a-fA-F])*\\1");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("11"));
    assertTrue(matcher.match("A13BBB"));
    assertFalse(matcher.match("12"));
    assertFalse(matcher.match("1A1"));
    assertFalse(matcher.match("1A1A"));
  }

  @Test
  public void testEmptyRecall() {
    Regex regex = new Regex("(a*|b)\\1");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(""));
  }

  // FIXME: there should be no @Ignores
  @Test
  @Ignore
  public void testRecallInsideDefinition() {
    Regex regex = new Regex("(a|\\1b)*");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("aab"));
  }

  @Test
  public void testRecallBeforeDefinition() {
    Regex regex = new Regex("(\\2*#(a))*");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("#aa#a"));
  }

  @Test
  public void testCharRange() {
    Regex regex = new Regex("[a-p]+");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("abacaba"));
    assertFalse(matcher.match("abazaba"));
  }

  @Test
  public void testLettersNumbersHyphens() {
    Regex regex = new Regex("([A-Za-z0-9-]+)");
    assertTrue(regex.match("abacaba"));
    assertTrue(regex.match("1337-tEXt-w1th-hyph3ns"));
    assertTrue(regex.match("89123456700"));
    assertFalse(regex.match("+79123456700"));
    assertFalse(regex.match("[123]"));
    assertFalse(regex.match("text with spaces"));
  }

  @Test
  public void testNegateHyphenCharRange() {
    Regex regex = new Regex("[^--b]");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("+"));
    assertFalse(matcher.match("a"));
  }

  @Test
  public void testDoubleHyphenCharRange() {
    Regex regex = new Regex("[--]");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("-"));
  }

  @Test
  public void testEscapingInsideCharRange() {
    Regex regex = new Regex("[\\]]");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("\\]"));
  }

  @Test
  public void testFirstPosCloseBracketCharRange() {
    Regex regex = new Regex("[]]");
    assertTrue(regex.match("]"));
  }

  @Test
  public void testBracketInCharRange() {
    Regex regex = new Regex("([)])");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(")"));
  }

  @Test
  public void testSimpleDot() {
    Regex regex = new Regex("(.)(.)");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(" \t"));
    assertTrue(matcher.match(".."));
    assertFalse(matcher.match("Boobs!"));
    assertFalse(matcher.match("a"));
    assertFalse(matcher.match(". ."));
  }

  @Test
  public void testQuestionMark() {
    Regex regex = new Regex(".?[abd-wyz]?");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(""));
    assertTrue(matcher.match("$"));
    assertTrue(matcher.match("ab"));
    assertFalse(matcher.match("ac"));
    assertFalse(matcher.match("abc"));
  }

  @Test
  public void testEscapedCloseBracket() {
    Regex regex = new Regex("(\\))");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(")"));
  }

  @Test
  public void testEscapedBrackets() {
    Regex regex = new Regex("\\)\\]\\}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match(")]}"));
  }

  @Test
  public void testCloseSquareBracket() {
    Regex regex = new Regex("(])");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("]"));
  }

  @Test
  public void testCloseBrace() {
    Regex regex = new Regex("a}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("a}"));
  }

  // TODO: tests for \D, \s, \S
  @Test
  public void testDates() {
    // Date dd/mm/yyyy, or any mix
    Regex regex = new Regex("(0[1-9]|[1-2]\\d|3[01])/(0[1-9]|1[012])/(\\d{4})");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("31/12/2006"));
    assertTrue(matcher.match("02/09/0002")); // weird year, I know
    assertFalse(matcher.match("2/09/2006"));
    assertFalse(matcher.match("32/10/2006"));
    assertFalse(matcher.match("02/09/206"));
  }

  @Test
  public void testSimpleAnchors() {
    Regex regex = new Regex("^abc$");
    assertTrue(regex.match("abc"));
    assertFalse(regex.match("^abc$"));
  }

  @Test
  public void testSimpleAnchorsFalse() {
    Regex regex = new Regex("ab$cd");
    assertFalse(regex.match("abcd"));
  }

  @Test
  public void testUsefulDollarAnchor() {
    Regex regex = new Regex("(b$|c)a*");
    assertTrue(regex.match("b"));
    assertFalse(regex.match("ba"));
  }

  @Test
  public void testUsefulCapAnchor() {
    Regex regex = new Regex("a*^c");
    assertTrue(regex.match("c"));
    assertFalse(regex.match("ac"));
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

  @Test(expected = RegexSyntaxException.class)
  public void testReverseCharRange() {
    Regex regex = new Regex("[b-a]");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testReverseCharRangeWithHyphen() {
    Regex regex = new Regex("[b--]");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testReverseCharRangeWithHyphen_2() {
    Regex regex = new Regex("[--+]");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testAnchorQuantifier() {
    Regex regex = new Regex("^*");
  }

  @Test
  public void testTest() {
    // Date dd/mm/yyyy, or any mix
    Regex regex = new Regex("(0[1-9]|[1-2]\\d|3[01])/(0[1-9]|1[012])/(\\d{4})");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.match("31/12/2006"));
    assertTrue(matcher.match("02/09/0002")); // weird year, I know
    assertFalse(matcher.match("2/09/2006"));
    assertFalse(matcher.match("32/10/2006"));
    assertFalse(matcher.match("02/09/206"));
  }

  // Various tests

}
