package com.eviltosha.regexpy;

import java.util.ArrayList;
import java.util.EmptyStackException;

/**
*
*/
abstract class Node {
  Node() {
    myNextNodes = new ArrayList<Node>();
  }

  protected final boolean checkVisit(String str, int strPos, Matcher matcher) {
    return (strPos <= str.length() && matcher.visitAndCheck(this, strPos));
  }

  protected abstract boolean matchMe(String str, int strPos, Matcher matcher);

  protected boolean matchNext(String str, int strPos, Matcher matcher) {
    for (Node node: myNextNodes) {
      if (node.matchMe(str, strPos, matcher)) {
        return true;
      }
    }
    return false;
  }

  final void addNextNode(Node node) {
    myNextNodes.add(node);
  }

  private final ArrayList<Node> myNextNodes;
}

class CharRangeNode extends Node {
  private static class CharRange {
    char myBegin, myEnd;
    CharRange(char begin, char end) throws IllegalArgumentException {
      if (begin > end) {
        throw new IllegalArgumentException("Char range begin > end");
      }
      myBegin = begin;
      myEnd = end;
    }
    boolean has(char ch) {
      return (myBegin <= ch && ch <= myEnd);
    }
  }

  CharRangeNode() {
    super();
    myCharRanges = new ArrayList<CharRange>();
    myChars = new ArrayList<Character>();
    myNegate = false;
  }

  void setNegate(boolean negate) { myNegate = negate; }
  void addChar(char ch) {
    myChars.add(ch);
  }
  void addCharRange(char begin, char end) throws IllegalArgumentException {
    myCharRanges.add(new CharRange(begin, end));
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkVisit(str, strPos, matcher)) { return false; }
    if (strPos == str.length()) { return false; }

    boolean charFound = false;
    char strChar = str.charAt(strPos);
    for (Character character: myChars) {
      if (strChar == character) {
        charFound = true;
      }
    }
    for (CharRange range: myCharRanges) {
      if (range.has(strChar)) {
        charFound = true;
      }
    }

    return ((charFound ^ myNegate) && matchNext(str, strPos + 1, matcher));
  }

  private final ArrayList<CharRange> myCharRanges;
  private final ArrayList<Character> myChars;
  // FIXME: this better be final
  private boolean myNegate;
}

class EmptyNode extends Node {
  EmptyNode() {
    super();
  }
  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    return (checkVisit(str, strPos, matcher) && matchNext(str, strPos, matcher));
  }
}

class OpenGroupNode extends EmptyNode {
  OpenGroupNode(int id) {
    super();
    myGroupId = id;
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkVisit(str, strPos, matcher)) { return false; }
    matcher.openGroup(myGroupId, strPos);
    if (matchNext(str, strPos, matcher)) {
      return true;
    }
    matcher.recoverOpenGroup(myGroupId);
    return false;
  }

  private final int myGroupId;
}

class CloseGroupNode extends EmptyNode {
  CloseGroupNode(int id) {
    super();
    myGroupId = id;
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkVisit(str, strPos, matcher)) { return false; }
    matcher.closeGroup(myGroupId, strPos);
    if (matchNext(str, strPos, matcher)) {
      return true;
    }
    matcher.recoverCloseGroup(myGroupId);
    return false;
  }

  private final int myGroupId;
}

class EndNode extends CloseGroupNode {
  EndNode() {
    // EndNode always has capturing id = 0
    super(0);
  }
  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    matcher.closeGroup(0, strPos);
    return (strPos == str.length());
  }
}

class GroupRecallNode extends Node {
  GroupRecallNode(int id) {
    super();
    myGroupId = id;
  }
  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkVisit(str, strPos, matcher)) { return false; }
    Range range;
    try {
      range = matcher.getGroupRange(myGroupId);
    } catch (EmptyStackException e) {
      return false;
    }
    if (!range.isDefined() || strPos + range.length() > str.length() ||
        range.getBegin() + range.length() > str.length()) {
      return false;
    }
    for (int offset = 0; offset < range.length(); ++offset) {
      if (str.charAt(range.getBegin() + offset) != str.charAt(strPos + offset)) {
        return false;
      }
    }
    return matchNext(str, strPos + range.length(), matcher);
  }

  private final int myGroupId;
}

class SymbolNode extends Node {
  SymbolNode(char symbol) {
    super();
    mySymbol = symbol;
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    return (checkVisit(str, strPos, matcher) && strPos < str.length() &&
        str.charAt(strPos) == mySymbol && matchNext(str, strPos + 1, matcher));
  }

  private final char mySymbol;
}

class AnySymbolNode extends Node {
  AnySymbolNode() {
    super();
  }
  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    return (checkVisit(str, strPos, matcher) && strPos < str.length() &&
        matchNext(str, strPos + 1, matcher));
  }
}

class RangeQuantifierNode extends EmptyNode {
  // FIXME: is it ok to use -1 as an indicator of infinity?
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
    // check lower bound, and go further if counter is in range
    return (myRange.checkLower(counter) && myNextNode.matchMe(str, strPos, matcher));
  }

  // FIXME: use Range class
  private final InfinityRange myRange;
  private final Node myNextNode;
}