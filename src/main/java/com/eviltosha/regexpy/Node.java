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
  Node() { }

  /** Check whether strPos is within bounds and perform actions required upon visiting the node */
  protected final boolean checkAndVisit(final String str, final int strPos, final Matcher matcher) {
    return (strPos <= str.length() && matcher.visitAndCheck(this, strPos));
  }

  /** Performs check whether there exists a path from this Node to the end node of the graph */
  protected abstract boolean matchMe(final String str, final int strPos, final Matcher matcher);

  /** Performs check whether there exists a path from any of adjacent Nodes of this Node to the end of the graph */
  protected boolean matchNext(final String str, final int strPos, final Matcher matcher) {
    for (Node node: myNextNodes) {
      if (node.matchMe(str, strPos, matcher)) {
        return true;
      }
    }
    return false;
  }

  final void addNextNode(final Node node) {
    myNextNodes.add(node);
  }

  /** List of adjacent Nodes of this Node */
  private final List<Node> myNextNodes = new ArrayList<Node>();
}

/** A Node representing character range (like [a-zA-Z]) */
class CharRangeNode extends Node {
  private static final class CharRange {
    final char myBegin, myEnd;
    CharRange(final char begin, final char end) throws IllegalArgumentException {
      if (begin > end) {
        throw new IllegalArgumentException("Char range begin > end");
      }
      myBegin = begin;
      myEnd = end;
    }
    boolean within(final char ch) {
      return (myBegin <= ch && ch <= myEnd);
    }
  }

  CharRangeNode() { super(); }

  void setNegate(final boolean negate) { myNegate = negate; }
  void addChar(final char ch) { myChars.add(ch); }
  void addCharRange(final char begin, char end) throws IllegalArgumentException {
    myCharRanges.add(new CharRange(begin, end));
  }

  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    if (!checkAndVisit(str, strPos, matcher) || strPos == str.length()) { return false; }

    boolean charFound = false;
    final char strChar = str.charAt(strPos);
    for (Character character: myChars) {
      if (strChar == character) {
        charFound = true;
      }
    }
    for (CharRange range: myCharRanges) {
      if (range.within(strChar)) {
        charFound = true;
      }
    }

    return ((charFound ^ myNegate) && matchNext(str, strPos + 1, matcher));
  }

  private final List<CharRange> myCharRanges = new ArrayList<CharRange>();
  private final List<Character> myChars = new ArrayList<Character>();
  private boolean myNegate = false;
}

/** An empty node, matches any valid position in the string */
class EmptyNode extends Node {
  EmptyNode() { super(); }
  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && matchNext(str, strPos, matcher));
  }
}

/** A Node representing group opening, performs required actions for group recalling */
class OpenGroupNode extends EmptyNode {
  OpenGroupNode(final int id) {
    super();
    myGroupId = id;
  }

  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    if (!checkAndVisit(str, strPos, matcher)) { return false; }
    matcher.openGroup(myGroupId, strPos);
    if (matchNext(str, strPos, matcher)) {
      return true;
    }
    matcher.undoOpenGroup(myGroupId);
    return false;
  }

  private final int myGroupId;
}

/** A Node representing group closing, performs required actions for group recalling */
class CloseGroupNode extends EmptyNode {
  CloseGroupNode(final int id) {
    super();
    myGroupId = id;
  }

  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    if (!checkAndVisit(str, strPos, matcher)) { return false; }
    matcher.closeGroup(myGroupId, strPos);
    if (matchNext(str, strPos, matcher)) {
      return true;
    }
    matcher.undoCloseGroup(myGroupId);
    return false;
  }

  private final int myGroupId;
}

/**
 * A Node representing the end of the regular expression. It's the only Node that can return true
 * from the matchMe method without matching nodes further in the graph.
 * Also it's a CloseGroupNode for group 0.
 */
class EndNode extends CloseGroupNode {
  EndNode() {
    // EndNode always has capturing id = 0
    super(0);
  }
  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    matcher.closeGroup(0, strPos);
    return (strPos == str.length());
  }
}

/** A Node representing group recall operation (like \\2) */
class GroupRecallNode extends Node {
  GroupRecallNode(final int id) {
    super();
    myGroupId = id;
  }
  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    if (!checkAndVisit(str, strPos, matcher)) { return false; }
    final Range range;
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

/** A Node that matcher a single specified character */
class SymbolNode extends Node {
  SymbolNode(final char symbol) {
    super();
    mySymbol = symbol;
  }

  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && strPos < str.length() &&
        str.charAt(strPos) == mySymbol && matchNext(str, strPos + 1, matcher));
  }

  private final char mySymbol;
}

/** A Node that matches any single character (a dot operation) */
class AnySymbolNode extends Node {
  AnySymbolNode() { super(); }
  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && strPos < str.length() &&
        matchNext(str, strPos + 1, matcher));
  }
}

/** A Node that performs required operations for range quantifier operation (like {3, 4} or {3, } or {3}) */
class RangeQuantifierNode extends EmptyNode {
  RangeQuantifierNode(final InfinityRange range, final Node nextNode) {
    super();
    myRange = range;
    myNextNode = nextNode;
  }

  @Override
  protected boolean matchNext(final String str, final int strPos, final Matcher matcher) {
    final int counter = matcher.visitCount(this);
    if (!myRange.checkUpper(counter)) {
      return false;
    }
    if (super.matchNext(str, strPos, matcher)) {
      return true;
    }
    return (myRange.checkLower(counter) && myNextNode.matchMe(str, strPos, matcher));
  }

  private final InfinityRange myRange;
  private final Node myNextNode;
}

class AnchorStartStringNode extends EmptyNode {
  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && strPos == 0 && matchNext(str, strPos, matcher));
  }
}

class AnchorEndStringNode extends EmptyNode {
  @Override
  protected boolean matchMe(final String str, final int strPos, final Matcher matcher) {
    return (checkAndVisit(str, strPos, matcher) && strPos == str.length() && matchNext(str, strPos, matcher));
  }
}