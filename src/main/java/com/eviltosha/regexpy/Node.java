package com.eviltosha.regexpy;

import java.util.ArrayList;
import java.util.EmptyStackException;

/**
* Created by eviltosha on 6/30/14.
*/
abstract class Node {
  Node() {
    myNextNodes = new ArrayList<Node>();
  }

  protected boolean checkVisit(String str, int strPos, Matcher matcher) {
    // the case (pos == str.length()) and curNode isn't final will be processed below
    if (strPos > str.length() || !matcher.visitAndCheck(this, strPos)) {
      return false;
    }
    return true;
  }

  protected abstract boolean matchMe(String str, int strPos, Matcher matcher);

  void addNextNode(Node node) {
    myNextNodes.add(node);
  }

  protected boolean matchNext(String str, int strPos, Matcher matcher) {
    for (Node node: myNextNodes) {
      if (node.matchMe(str, strPos, matcher)) {
        return true;
      }
    }
    return false;
  }

  private ArrayList<Node> myNextNodes;
}

class CharRangeNode extends Node {
  private static class CharRange {
    char myBegin, myEnd;
    CharRange(char begin, char end) {
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

  // FIXME: probably all parsing methods should be in Regex class
  CharRangeNode(RegexStringProcessor processor) {
    this();
    // first characters that need special treatment: '^' (negates range),
    // '-' (in first position it acts like literal hyphen, also can be part of a range),
    // ']' (in first position it acts like literal closing square bracket, also can be part of a range)
    char ch = processor.next();
    if (ch == '^') {
      setNegate(true);
      // we need to perform the first character analysis once more (for special '-' and ']' cases)
      ch = processor.next();
    }
    // we store parsed char,
    // if next char is not '-', we add it as a char, otherwise construct range
    char storedChar = ch;
    // FIXME: this var seems unnecessary; maybe use Character for storedChar and use null check?
    boolean charIsStored = true;
    boolean asRange = false;
    boolean charRangeFinished = false;
    while (processor.hasNext() && !charRangeFinished) {
      ch = processor.next();
      switch (ch) {
        case ']':
          if (charIsStored) {
            addChar(storedChar);
            // if '-' stands right before the closing bracket it's treated as literal '-'
            if (asRange) {
              addChar('-');
            }
          }
          charRangeFinished = true;
          break;
        case '-':
          if (!charIsStored || asRange) {
            // check whether it's the last char in group (like in "[a--]")
            if (processor.next() == ']') {
              if (asRange) {
                if (storedChar > '-') {
                  throw new RegexSyntaxException("Invalid char range", processor.getRegex());
                }
                addCharRange(storedChar, '-');
              } else {
                addChar('-');
              }
              charRangeFinished = true;
            } else {
              throw new RegexSyntaxException("Incorrect use of hyphen inside char range", processor.getRegex());
            }
          }
          asRange = true;
          break;
        default:
          if (charIsStored) {
            if (asRange) {
              if (storedChar > ch) {
                throw new RegexSyntaxException("Invalid char range", processor.getRegex());
              }
              addCharRange(storedChar, ch);
              charIsStored = false;
            } else {
              addChar(storedChar);
              storedChar = ch;
              // charIsStored remains true
            }
          } else {
            storedChar = ch;
            charIsStored = true;
          }
          asRange = false;
          break;
      }
    }
    if (!charRangeFinished) {
      throw new RegexSyntaxException("Unclosed char range", processor.getRegex());
    }
  }

  void setNegate(boolean negate) { myNegate = negate; }
  void addChar(char ch) {
    myChars.add(ch);
  }
  void addCharRange(char begin, char end) {
    myCharRanges.add(new CharRange(begin, end));
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkVisit(str, strPos, matcher)) { return false; }

    boolean charFound = false;
    if (strPos == str.length()) { return false; }
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

  private ArrayList<CharRange> myCharRanges;
  private ArrayList<Character> myChars;
  private boolean myNegate;
}

class EmptyNode extends Node {
  EmptyNode() {
    super();
  }
  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkVisit(str, strPos, matcher)) { return false; }
    return matchNext(str, strPos, matcher);
  }
}

class EndNode extends EmptyNode {
  EndNode() {
    super();
  }
  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    return (strPos == str.length());
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

  private int myGroupId;
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

  private int myGroupId;
}

// FIXME: group zero (add or specify as excluded functionality)
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

  private int myGroupId;
}

class SymbolNode extends Node {
  SymbolNode(char symbol) {
    super();
    mySymbol = symbol;
  }

  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkVisit(str, strPos, matcher)) { return false; }
    if (strPos < str.length() && str.charAt(strPos) == mySymbol) {
      return matchNext(str, strPos + 1, matcher);
    }
    return false;
  }
  private char mySymbol;
}

class AnySymbolNode extends Node {
  AnySymbolNode() {
    super();
  }
  @Override
  protected boolean matchMe(String str, int strPos, Matcher matcher) {
    if (!checkVisit(str, strPos, matcher)) { return false; }
    return (strPos < str.length() && matchNext(str, strPos + 1, matcher));
  }
}

class RangeQuantifierNode extends EmptyNode {
  // FIXME: is it ok to use -1 as an indicator of infinity?
  RangeQuantifierNode(Node nextNode, int rangeBegin, int rangeEnd) {
    super();
    myNextNode = nextNode;
    myRangeBegin = rangeBegin;
    myRangeEnd = rangeEnd;
  }

  @Override
  protected boolean matchNext(String str, int strPos, Matcher matcher) {
    int counter = matcher.visitCount(this);
    // check upper bound
    if (myRangeEnd > -1 && counter > myRangeEnd) {
      return false;
    }
    if (super.matchNext(str, strPos, matcher)) {
      return true;
    }
    // check lower bound, and go further if counter is in range
    return (myRangeBegin <= counter && myNextNode.matchMe(str, strPos, matcher));
  }

  // FIXME: use Range class
  private int myRangeBegin, myRangeEnd;
  private Node myNextNode;
}