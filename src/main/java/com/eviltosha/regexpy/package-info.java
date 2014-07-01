package com.eviltosha.regexpy;

/** * Provides classes Regex and Matcher for matching strings against regular expressions. Regex class
 stores a compiled representation of the regular expression. Several Matchers for a single Regex object
 can exist and operate at the same time.

 Basic usage:
 Regex regex = new Regex("foo|bar");
 Matcher matcher = regex.matcher();
 boolean matches = matcher.matches("foo");
 */