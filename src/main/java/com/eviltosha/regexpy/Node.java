package com.eviltosha.regexpy;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.List;

/**
 * One node of the regex graph.
 * <p>
 * No Node stores any matcher-time information, thus multiple matchers can be used at the same time
 * (or even concurrently)
 */
abstract class Node {
  /** List of adjacent Nodes of this Node */
  private List<Node> myNextNodes = new ArrayList<Node>();

  Node() { }

  void addNextNode(Node node) {
    myNextNodes.add(node);
  }

  /** Check whether strPos is within bounds and perform actions required upon visiting the node */
  protected boolean checkAndVisit(String str, int strPos, Matcher matcher) {
    return ((strPos <= str.length()) && matcher.visitAndCheck(this, strPos));
  }

  /** Performs check whether there exists a path from this Node to the end node of the graph */
  protected abstract boolean matchMe(String str, int strPos, Matcher matcher);

  /** Performs check whether there exists a path from any of adjacent Nodes of this Node to the end of the graph */
  protected boolean matchNext(String str, int strPos, Matcher matcher) {
    for (Node node : myNextNodes) {
      if (node.matchMe(str, strPos, matcher)) {
        return true;
      }
    }
    return false;
  }
}

/** A Node representing character range (like [a-zA-Z]) */
class CharRangeNode extends Node {
  private List<CharRange> myCharRanges = new ArrayList<CharRange>();
  private List<Character> myChars = new ArrayList<Character>();
  private boolean myNegate = false;

  CharRangeNode() { super(); }

  void setNegate(boolean negate) { myNegate = negate; }

  void addChar(char ch) { myChars.add(ch); }

  void addCharRange(char begin, char end) throws IllegalArgumentException {
    myCharRanges.add(new CharRange(begin, end));
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkAndVisit(str, strPos, matcher) || (strPos == str.length())) { return false; }

    boolean charFound = false;
    char strChar = str.charAt(strPos);
    for (Character character : myChars) {
      if (strChar == character) {
        charFound = true;
      }
    }
    for (CharRange range : myCharRanges) {
      if (range.within(strChar)) {
        charFound = true;
      }
    }

    return ((charFound ^ myNegate) && matchNext(str, strPos + 1, matcher));
  }

  /** An empty node, matches any valid position in the string */
  private static class CharRange {
    char myBegin, myEnd;
    CharRange(char begin, char end) throws IllegalArgumentException {
      if (begin > end) {
        throw new IllegalArgumentException("Char range begin > end");
      }
      myBegin = begin;
      myEnd = end;
    }

    boolean within(char ch) {
      return ((myBegin <= ch) && (ch <= myEnd));
    }
  }
}

class EmptyNode extends Node {
  EmptyNode() { super(); }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && matchNext(str, strPos, matcher));
  }
}

/** A Node representing group opening, performs required actions for group recalling */
class OpenGroupNode extends EmptyNode {
  private int myGroupId;

  OpenGroupNode(int id) {
    super();
    myGroupId = id;
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkAndVisit(str, strPos, matcher)) { return false; }
    matcher.openGroup(myGroupId, strPos);
    if (matchNext(str, strPos, matcher)) {
      return true;
    }
    matcher.undoOpenGroup(myGroupId);
    return false;
  }
}

/** A Node representing group closing, performs required actions for group recalling */
class CloseGroupNode extends EmptyNode {
  private int myGroupId;

  CloseGroupNode(int id) {
    super();
    myGroupId = id;
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkAndVisit(str, strPos, matcher)) { return false; }
    matcher.closeGroup(myGroupId, strPos);
    if (matchNext(str, strPos, matcher)) {
      return true;
    }
    matcher.undoCloseGroup(myGroupId);
    return false;
  }
}

/**
 * A Node representing the end of the regular expression. It's the only Node that can return true
 * from the matchMe method without matching nodes further in the graph.
 * Also it's a CloseGroupNode for group 0.
 */
class EndNode extends CloseGroupNode {
  EndNode() {
    /* EndNode always has capturing id = 0 */
    super(0);
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    matcher.closeGroup(0, strPos);
    return (strPos == str.length());
  }
}

/** A Node representing group recall operation (like \\2) */
class GroupRecallNode extends Node {
  private int myGroupId;

  GroupRecallNode(int id) {
    super();
    myGroupId = id;
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkAndVisit(str, strPos, matcher)) { return false; }
    Range range;
    try {
      range = matcher.getGroupRange(myGroupId);
    } catch (EmptyStackException e) {
      return false;
    }
    if (!range.isDefined() || (strPos + range.length() > str.length()) ||
        (range.getBegin() + range.length() > str.length())) {
      return false;
    }
    for (int offset = 0; offset < range.length(); ++offset) {
      if (str.charAt(range.getBegin() + offset) != str.charAt(strPos + offset)) {
        return false;
      }
    }
    return matchNext(str, strPos + range.length(), matcher);
  }
}

/** A Node that matches a single specified character */
class SymbolNode extends Node {
  private char mySymbol;

  SymbolNode(char symbol) {
    super();
    mySymbol = symbol;
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && (strPos < str.length()) &&
        (str.charAt(strPos) == mySymbol) && matchNext(str, strPos + 1, matcher));
  }
}

/** A Node that matches any single character (a dot operation) */
class AnySymbolNode extends Node {
  AnySymbolNode() { super(); }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && (strPos < str.length()) &&
        matchNext(str, strPos + 1, matcher));
  }
}

/** A Node that performs required operations for range quantifier operation (like {3, 4} or {3, } or {3}) */
class RangeQuantifierNode extends EmptyNode {
  private InfinityRange myRange;
  private Node myNextNode;

  RangeQuantifierNode(InfinityRange range, Node nextNode) {
    super();
    myRange = range;
    myNextNode = nextNode;
  }

  @Override
  protected boolean matchNext(String str, int strPos, Matcher matcher) {
    int counter = matcher.visitCount(this);
    if (!myRange.checkUpper(counter)) {
      return false;
    }
    if (super.matchNext(str, strPos, matcher)) {
      return true;
    }
    return (myRange.checkLower(counter) && myNextNode.matchMe(str, strPos, matcher));
  }
}

class AnchorStartStringNode extends EmptyNode {
  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && (strPos == 0) && matchNext(str, strPos, matcher));
  }
}

class AnchorEndStringNode extends EmptyNode {
  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && (strPos == str.length()) && matchNext(str, strPos, matcher));
  }
}