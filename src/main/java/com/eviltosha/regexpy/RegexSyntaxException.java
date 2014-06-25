package com.eviltosha.regexpy;

/**
 * Created by eviltosha on 6/26/14.
 */
public class RegexSyntaxException extends IllegalArgumentException {
  private final String myDesc;
  private final String myRegex;

  public RegexSyntaxException(String desc, String regex) {
    myDesc = desc;
    myRegex = regex;
  }

  public String getDescription() { return myDesc; }
  public String getRegex() { return myRegex; }
}
