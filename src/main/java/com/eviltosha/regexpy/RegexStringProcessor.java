package com.eviltosha.regexpy;

import java.util.Stack;

/**
* Created by eviltosha on 6/28/14.
*/
// Class for processing string representation of regex
// TODO: public, private, ...
class RegexStringProcessor {
  String myRegex;
  int myPos; // char at myPos is not processed yet

  RegexStringProcessor(String regex) {
    myRegex = regex;
    myPos = 0;
  }

  String getRegex() { return myRegex; }

  boolean hasNext() {
    return (myPos < myRegex.length());
  }

  char peek() {
    if (!hasNext()) {
      // FIXME: should it be different type of exception?
      throw new RegexSyntaxException("Unexpected end of string", myRegex);
    }
    return myRegex.charAt(myPos);
  }

  char eat() {
    if (!hasNext()) {
      throw new RegexSyntaxException("Unexpected end of string", myRegex);
    }
    char ch = myRegex.charAt(myPos);
    ++myPos;
    return ch;
  }

  void eatSilently() {
    if (!hasNext()) {
      throw new RegexSyntaxException("Unexpected end of string", myRegex);
    }
    ++myPos;
  }

  int eatNumber() {
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
    int res = Integer.parseInt(myRegex.substring(posStart, myPos));
    return res;
  }
}
