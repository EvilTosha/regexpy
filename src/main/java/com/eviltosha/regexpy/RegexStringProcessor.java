package com.eviltosha.regexpy;

import java.util.Stack;

/**
* Created by eviltosha on 6/28/14.
*/
// Class for processing string representation of regex
class RegexStringProcessor {
  public RegexStringProcessor(String regex) {
    myRegex = regex;
    myPos = 0;
  }

  public String getRegex() { return myRegex; }

  public boolean hasNext() {
    return (myPos < myRegex.length());
  }

  public char peek() throws RegexSyntaxException {
    if (!hasNext()) {
      throw new RegexSyntaxException("Unexpected end of string", myRegex);
    }
    return myRegex.charAt(myPos);
  }

  public char eat() throws RegexSyntaxException {
    if (!hasNext()) {
      throw new RegexSyntaxException("Unexpected end of string", myRegex);
    }
    char ch = myRegex.charAt(myPos);
    ++myPos;
    return ch;
  }

  public void eatSilently() throws RegexSyntaxException {
    if (!hasNext()) {
      throw new RegexSyntaxException("Unexpected end of string", myRegex);
    }
    ++myPos;
  }

  public int eatNumber() throws RegexSyntaxException {
    // FIXME: rewrite with only one throw (and try-catch)
    if (!hasNext()) {
      throw new RegexSyntaxException("Unexpected end of string", myRegex);
    }
    if (!Character.isDigit(peek())) {
      throw new RegexSyntaxException("Unexpected non-digit char", myRegex);
    }
    int posStart = myPos;
    while (hasNext() && Character.isDigit(peek())) {
      eatSilently();
    }
    return Integer.parseInt(myRegex.substring(posStart, myPos));
  }

  private String myRegex;
  private int myPos; // char at myPos is not processed yet
}
