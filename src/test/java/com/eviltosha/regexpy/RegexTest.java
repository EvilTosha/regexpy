package com.eviltosha.regexpy;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for simple Regex.
 */
public class RegexTest {
  // NOTE: I know that multiple asserts in a single test case is a bad practice, but they test a single aspect
  // in each test case, and use a single regex string, so it's quite convenient here.
  // Asserts are lacking messages, because in most cases test strings are self-explanatory,
  // and writing additional messages for every assert is unnecessary work
  // TODO: it's probably a good idea to create data files for regex and test strings and simply parse data for tests

  // Regular expressions components tests
  @Test
  public void testEmptyString() {
    Regex regex = new Regex("");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(""));
    assertFalse(matcher.matches(" "));
  }

  @Test
  public void testExactString() {
    Regex regex = new Regex("aba caba");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("aba caba"));
    assertFalse(matcher.matches("abacaba"));
    assertFalse(matcher.matches("aba daba"));
  }

  @Test
  public void testAsterisk() {
    Regex regex = new Regex("a*");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(""));
    assertTrue(matcher.matches("a"));
    assertTrue(matcher.matches("aaaaaaaaaaaaaa"));
    assertFalse(matcher.matches("a*"));
    assertFalse(matcher.matches("aaaaab"));
  }

  @Test
  public void testPlus() {
    Regex regex = new Regex("c+");
    Matcher matcher = regex.matcher();
    assertFalse(matcher.matches(""));
    assertTrue(matcher.matches("c"));
    assertTrue(matcher.matches("ccccccccccccccc"));
    assertFalse(matcher.matches("c+"));
    assertFalse(matcher.matches("cccccb"));
  }

  @Test
  public void testEmptyGroup() {
    Regex regex = new Regex("()");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(""));
    assertFalse(matcher.matches("()"));
    assertFalse(matcher.matches("Foo() { bar(123); }"));
  }

  @Test
  public void testSimpleGroup() {
    Regex regex = new Regex("(b*c)d+");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("bcd"));
    assertTrue(matcher.matches("cdd"));
    assertTrue(matcher.matches("bbbcd"));
    assertFalse(matcher.matches("dd"));
    assertFalse(matcher.matches("(bc)d"));
  }

  @Test
  public void testGroupQuantifiers() {
    Regex regex = new Regex("(b*c)*");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("bbcbc"));
    assertTrue(matcher.matches(""));
    assertTrue(matcher.matches("cc"));
    assertFalse(matcher.matches("bcb"));
    assertFalse(matcher.matches("(bc)"));
  }

  @Test
  public void testNestedGroup() {
    Regex regex = new Regex("((c*d)+e)*f");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("ccdcddeccdef"));
    assertTrue(matcher.matches("f"));
    assertTrue(matcher.matches("def"));
    assertFalse(matcher.matches("de"));
    assertFalse(matcher.matches("cef"));
    assertFalse(matcher.matches("cdedebcef"));
  }

  @Test
  public void testSimpleOr() {
    Regex regex = new Regex("ab|cd");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("ab"));
    assertTrue(matcher.matches("cd"));
    assertFalse(matcher.matches(""));
    assertFalse(matcher.matches("b"));
    assertFalse(matcher.matches("ab|cd"));
    assertFalse(matcher.matches("abab"));
  }

  @Test
  public void testSimpleOr_2() {
    Regex regex = new Regex("(b|(c|d)*)+");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("b"));
    assertTrue(matcher.matches("bc"));
    assertTrue(matcher.matches("bcd"));
    assertTrue(matcher.matches("bbb"));
    assertTrue(matcher.matches("bbcbbdbbcd"));
    assertTrue(matcher.matches("c"));
    assertTrue(matcher.matches("dd"));
    assertTrue(matcher.matches(""));
  }

  @Test
  public void testOrEmptyClause() {
    Regex regex = new Regex("(|a)(||c||)");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(""));
    assertTrue(matcher.matches("c"));
    assertTrue(matcher.matches("ac"));
  }

  @Test
  public void testExactRangeQuantifier() {
    Regex regex = new Regex("a{3}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("aaa"));
    assertFalse(matcher.matches("aa"));
    assertFalse(matcher.matches(""));
    assertFalse(matcher.matches("aaaa"));
    assertFalse(matcher.matches("a{3}"));
    assertFalse(matcher.matches("aaab"));
  }

  @Test
  public void testSimpleRangeQuantifier() {
    Regex regex = new Regex("(ab){2,4}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("abab"));
    assertTrue(matcher.matches("ababab"));
    assertFalse(matcher.matches("ab"));
    assertFalse(matcher.matches("ababababab"));
    assertFalse(matcher.matches("aab"));
  }

  @Test
  public void testOpenRangeQuantifier() {
    Regex regex = new Regex("(a|b){4,}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("aaaa"));
    assertTrue(matcher.matches("bbbbaa"));
    assertFalse(matcher.matches("aaa"));
    assertFalse(matcher.matches(""));
    assertFalse(matcher.matches("aabbc"));
  }

  @Test
  public void testZeroRangeQuantifier() {
    Regex regex = new Regex("a{0,2}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(""));
    assertTrue(matcher.matches("aa"));
    assertFalse(matcher.matches("aaa"));
  }

  @Test
  public void testMultiDigitRangeQuantifier() {
    Regex regex = new Regex("a{1,12}");
    assertTrue(regex.matches("aaaaaaaaaaaa")); // 12 a's
    assertFalse(regex.matches("aaaaaaaaaaaaa")); // 13 a's
  }

  @Test
  public void testSimpleGroupRecall() {
    Regex regex = new Regex("(a|b)\\1");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("aa"));
    assertTrue(matcher.matches("bb"));
    assertFalse(matcher.matches("ab"));
    assertFalse(matcher.matches("ba"));
    assertFalse(matcher.matches("a1"));
    assertFalse(matcher.matches("a\\1"));
  }

  @Test
  public void testMultipleGroupRecall() {
    Regex regex = new Regex("(a+(b|c*)\\2)\\2\\1");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("aa"));
    assertTrue(matcher.matches("aaccccccaacccc"));
    assertTrue(matcher.matches("abbbabb"));
    assertFalse(matcher.matches("abcca"));
    assertFalse(matcher.matches("aca"));
    assertFalse(matcher.matches("abbb"));
    assertFalse(matcher.matches("abbbacc"));
  }

  @Test
  public void testMultiDigitGroupRecall() {
    Regex regex = new Regex("((((((((((((a))))))))))))\\11"); // 12 nested groups
    assertTrue(regex.matches("aa"));
  }

  @Test
  public void testRecallAfterQuantifier() {
    Regex regex = new Regex("(\\d|[a-fA-F])*\\1");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("11"));
    assertTrue(matcher.matches("A13BBB"));
    assertFalse(matcher.matches("12"));
    assertFalse(matcher.matches("1A1"));
    assertFalse(matcher.matches("1A1A"));
  }

  @Test
  public void testEmptyRecall() {
    Regex regex = new Regex("(a*|b)\\1");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(""));
  }

  @Test
  public void testRecallInsideDefinition() {
    Regex regex = new Regex("(a|\\1b)*");
    Matcher matcher = regex.matcher();
    assertFalse(matcher.matches("aab"));
  }

  @Test
  public void testRecallBeforeDefinition() {
    Regex regex = new Regex("(\\2*#(a))*");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("#aa#a"));
  }

  @Test
  public void testCharRange() {
    Regex regex = new Regex("[a-p]+");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("abacaba"));
    assertFalse(matcher.matches("abazaba"));
  }

  @Test
  public void testLettersNumbersHyphens() {
    Regex regex = new Regex("([A-Za-z0-9-]+)");
    assertTrue(regex.matches("abacaba"));
    assertTrue(regex.matches("1337-tEXt-w1th-hyph3ns"));
    assertTrue(regex.matches("89123456700"));
    assertFalse(regex.matches("+79123456700"));
    assertFalse(regex.matches("[123]"));
    assertFalse(regex.matches("text with spaces"));
  }

  @Test
  public void testNegateHyphenCharRange() {
    Regex regex = new Regex("[^--b]");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("+"));
    assertFalse(matcher.matches("a"));
  }

  @Test
  public void testDoubleHyphenCharRange() {
    Regex regex = new Regex("[--]");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("-"));
  }

  @Test
  public void testEscapingInsideCharRange() {
    Regex regex = new Regex("[\\]]");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("\\]"));
  }

  @Test
  public void testFirstPosCloseBracketCharRange() {
    Regex regex = new Regex("[]]");
    assertTrue(regex.matches("]"));
  }

  @Test
  public void testBracketInCharRange() {
    Regex regex = new Regex("([)])");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(")"));
  }

  @Test
  public void testSimpleDot() {
    Regex regex = new Regex("(.)(.)");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(" \t"));
    assertTrue(matcher.matches(".."));
    assertFalse(matcher.matches("Boobs!"));
    assertFalse(matcher.matches("a"));
    assertFalse(matcher.matches(". ."));
  }

  @Test
  public void testQuestionMark() {
    Regex regex = new Regex(".?[abd-wyz]?");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(""));
    assertTrue(matcher.matches("$"));
    assertTrue(matcher.matches("ab"));
    assertFalse(matcher.matches("ac"));
    assertFalse(matcher.matches("abc"));
  }

  @Test
  public void testEscapedCloseBracket() {
    Regex regex = new Regex("(\\))");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(")"));
  }

  @Test
  public void testEscapedBrackets() {
    Regex regex = new Regex("\\)\\]\\}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches(")]}"));
  }

  @Test
  public void testBackslashEscape() {
    Regex regex = new Regex("\\\\");
    assertTrue(regex.matches("\\"));
  }

  @Test
  public void testCloseSquareBracket() {
    Regex regex = new Regex("(])");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("]"));
  }

  @Test
  public void testCloseBrace() {
    Regex regex = new Regex("a}");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("a}"));
  }

  @Test
  public void testDates() {
    // Date dd/mm/yyyy, or any mix
    Regex regex = new Regex("(0[1-9]|[1-2]\\d|3[01])/(0[1-9]|1[012])/(\\d{4})");
    Matcher matcher = regex.matcher();
    assertTrue(matcher.matches("31/12/2006"));
    assertTrue(matcher.matches("02/09/0002")); // weird year, I know
    assertFalse(matcher.matches("2/09/2006"));
    assertFalse(matcher.matches("32/10/2006"));
    assertFalse(matcher.matches("02/09/206"));
  }

  @Test
  public void testWhiteSpaceCharRanges() {
    Regex regex = new Regex("\\S{3}\\s\\S+");
    assertTrue(regex.matches("aba caba"));
    assertFalse(regex.matches("aba caba "));
  }

  @Test
  public void testSimpleAnchors() {
    Regex regex = new Regex("^abc$");
    assertTrue(regex.matches("abc"));
    assertFalse(regex.matches("^abc$"));
  }

  @Test
  public void testSimpleAnchorsFalse() {
    Regex regex = new Regex("ab$cd");
    assertFalse(regex.matches("abcd"));
  }

  @Test
  public void testUsefulDollarAnchor() {
    Regex regex = new Regex("(b$|c)a*");
    assertTrue(regex.matches("b"));
    assertFalse(regex.matches("ba"));
  }

  @Test
  public void testUsefulCapAnchor() {
    Regex regex = new Regex("a*^c");
    assertTrue(regex.matches("c"));
    assertFalse(regex.matches("ac"));
  }

  @Test
  public void testMultipleMatchers() {
    Regex regex = new Regex("a*(b|c)");
    Matcher matcher1 = regex.matcher();
    Matcher matcher2 = regex.matcher();
    assertTrue(matcher1.matches("ab"));
    assertFalse(regex.matches("abc"));
    Matcher matcher3 = regex.matcher();
    assertFalse(matcher3.matches("bb"));
    assertTrue(matcher2.matches("b"));
  }
  // TODO: write concurrent test with multiple matchers

  // Exceptions tests

  @Test(expected = RegexSyntaxException.class)
  public void testUnclosedRangeQuantifier() {
    Regex regex = new Regex("a{");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testEmptyRangeQuantifier() {
    Regex regex = new Regex("a{}");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testCharsInsideRangeQuantifier() {
    Regex regex = new Regex("a{2,a3}");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testUnclosedOpenGroup() {
    Regex regex = new Regex("a(");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testWrongBracketSequence() {
    Regex regex = new Regex("(()(())()");
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

  @Test(expected = RegexSyntaxException.class)
  public void testMultipleQuantifiers() {
    Regex regex = new Regex("a*+");
  }

  @Test(expected = RegexSyntaxException.class)
  public void testRecallNonexistentGroup() {
    Regex regex = new Regex("(ab)\\2");
  }

  // Various tests
  @Test
  public void testEmailAddress() {
    Regex regex = new Regex("[a-zA-Z0-9_.-]+@[a-zA-Z_]+\\.[a-zA-Z]{2,6}");
    assertTrue(regex.matches("some-email@example.com"));
    assertFalse(regex.matches("someEmail@examplecom"));
  }

  @Test
  public void testTrickyCharRange() {
    Regex regex = new Regex("[a[^b]]");
    assertTrue(regex.matches("[]"));
    assertTrue(regex.matches("^]"));
    assertTrue(regex.matches("b]"));
  }

  @Test
  public void testHtmlTag() {
    Regex regex = new Regex("\\</?[^>]+\\>");
    assertTrue(regex.matches("<p>"));
    assertTrue(regex.matches("</body>"));
  }
}
