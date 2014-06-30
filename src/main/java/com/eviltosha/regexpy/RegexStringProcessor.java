package com.eviltosha.regexpy;

import java.util.Iterator;

/**
* Created by eviltosha on 6/28/14.
*/
// FIXME: probably java.util.Scanner or another standard String iterator can be used instead of this class
// Class for processing string representation of regex
class RegexStringProcessor implements Iterator<Character> {
  public RegexStringProcessor(String regex) {
    myRegex = regex;
    myPos = 0;
  }

  public String getRegex() { return myRegex; }

  @Override
  public boolean hasNext() {
    return (myPos < myRegex.length());
  }

  public Character peek() throws RegexSyntaxException {
    if (!hasNext()) {
      throw new RegexSyntaxException("Unexpected end of string", myRegex);
    }
    return myRegex.charAt(myPos);
  }

  @Override
  public Character next() throws RegexSyntaxException {
    if (!hasNext()) {
      throw new RegexSyntaxException("Unexpected end of string", myRegex);
    }
    Character ch = myRegex.charAt(myPos);
    ++myPos;
    return ch;
  }

  public int nextNumber() throws RegexSyntaxException {
    if (!hasNext() || !Character.isDigit(peek())) {
      throw new RegexSyntaxException("Unexpected input where number expected", myRegex);
    }
    int posStart = myPos;
    while (hasNext() && Character.isDigit(peek())) {
      next();
    }
    return Integer.parseInt(myRegex.substring(posStart, myPos));
  }

  @Override
  public void remove()  {
    throw new RegexSyntaxException("Can't remove elements of regex string", myRegex);
  }

  private String myRegex;
  private int myPos; // char at myPos is not processed yet
}
