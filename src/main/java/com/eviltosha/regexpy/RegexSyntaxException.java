package com.eviltosha.regexpy;

/**
 *
 */
public class RegexSyntaxException extends IllegalArgumentException {
  // TODO: this exception should indicate position of error in string
  private final String myDesc;
  private final String myRegex;

  public RegexSyntaxException(String desc, String regex) {
    myDesc = desc;
    myRegex = regex;
  }

  public String getDescription() { return myDesc; }
  public String getRegex() { return myRegex; }
}
