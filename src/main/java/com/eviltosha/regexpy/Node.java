package com.eviltosha.regexpy;

import java.util.ArrayList;
import java.util.EmptyStackException;

/**
* Created by eviltosha on 6/30/14.
*/
abstract class Node {
  Node(MatchState matchState) {
    matchState.addNode(this);
    myNextNodes = new ArrayList<Node>();
  }

  boolean match(String str, int strPos, MatchState matchState) {
    // FIXME: too many returns
    if (strPos == str.length() && isEnd()) {
      return true;
    }
    // the case (pos == str.length()) and curNode isn't final will be processed below
    if (strPos > str.length()) {
      return false;
    }
    // to avoid looping with empty string
    if (myLastVisitPos == strPos) {
      return false;
    }
    myLastVisitPos = strPos;

    int increment = matchMe(str, strPos, matchState);
    if (increment == -1) {
      recoverState(matchState);
      return false;
    }
    if (matchNext(str, strPos + increment, matchState)) {
      return true;
    }
    recoverState(matchState);
    return false;
  }

  void clear() { myLastVisitPos = -1; }
  boolean isEnd() { return false; }
  void addNextNode(Node node) {
    myNextNodes.add(node);
  }

  protected boolean matchNext(String str, int strPos, MatchState matchState) {
    for (Node node: myNextNodes) {
      if (node.match(str, strPos, matchState)) {
        return true;
      }
    }
    return false;
  }

  protected abstract int matchMe(String str, int strPos, MatchState matchState);
  protected void recoverState(MatchState matchState) { /* do nothing */ }

  private ArrayList<Node> myNextNodes;
  private int myLastVisitPos;
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

  CharRangeNode(MatchState matchState) {
    super(matchState);
    myCharRanges = new ArrayList<CharRange>();
    myChars = new ArrayList<Character>();
    myNegate = false;
  }

  // FIXME: probably all parsing methods should be in Regex class
  CharRangeNode(RegexStringProcessor processor, MatchState matchState) {
    this(matchState);
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
  protected int matchMe(String str, int strPos, MatchState matchState) {
    if (strPos >= str.length()) {
      return -1;
    }
    // use this value for return
    // FIXME: Use Integer and null?
    int charFound = (myNegate ? -1 : 1);
    char strChar = str.charAt(strPos);
    for (Character character: myChars) {
      if (strChar == character) {
        return charFound;
      }
    }
    for (CharRange range: myCharRanges) {
      if (range.has(strChar)) {
        return charFound;
      }
    }
    return -charFound;
  }

  private ArrayList<CharRange> myCharRanges;
  private ArrayList<Character> myChars;
  private boolean myNegate;
}

class EmptyNode extends Node {
  EmptyNode(MatchState matchState) {
    super(matchState);
  }
  @Override
  protected int matchMe(String str, int strPos, MatchState matchState) { return 0; }
}

class EndNode extends EmptyNode {
  EndNode(MatchState matchState) {
    super(matchState);
  }
  @Override
  boolean isEnd() { return true; }
}

class OpenGroupNode extends EmptyNode {
  OpenGroupNode(int id, MatchState matchState) {
    super(matchState);
    myGroupId = id;
  }

  @Override
  protected int matchMe(String str, int strPos, MatchState matchState) {
    matchState.openGroup(myGroupId, strPos);
    return super.matchMe(str, strPos, matchState);
  }

  protected void recoverState(MatchState matchState) {
    matchState.recoverOpenGroup(myGroupId);
  }

  private int myGroupId;
}

class CloseGroupNode extends EmptyNode {
  CloseGroupNode(int id, MatchState matchState) {
    super(matchState);
    myGroupId = id;
  }

  @Override
  protected int matchMe(String str, int strPos, MatchState matchState) {
    matchState.closeGroup(myGroupId, strPos);
    return super.matchMe(str, strPos, matchState);
  }

  protected void recoverState(MatchState matchState) {
    matchState.recoverCloseGroup(myGroupId);
  }

  private int myGroupId;
}

// FIXME: group zero (add or specify as excluded functionality)
class GroupRecallNode extends Node {
  GroupRecallNode(int id, MatchState matchState) {
    super(matchState);
    myGroupId = id;
  }
  @Override
  protected int matchMe(String str, int strPos, MatchState matchState) {
    Range range;
    try {
      range = matchState.getRange(myGroupId);
    } catch (EmptyStackException e) {
      return -1;
    }
    if (!range.isDefined() || strPos + range.length() > str.length() ||
        range.getBegin() + range.length() > str.length()) {
      return -1;
    }
    for (int offset = 0; offset < range.length(); ++offset) {
      if (str.charAt(range.getBegin() + offset) != str.charAt(strPos + offset)) {
        return -1;
      }
    }
    return range.length();
  }

  private int myGroupId;
}

class SymbolNode extends Node {
  SymbolNode(char symbol, MatchState matchState) {
    super(matchState);
    mySymbol = symbol;
  }

  @Override
  protected int matchMe(String str, int pos, MatchState matchState) {
    if (pos < str.length() && str.charAt(pos) == mySymbol) {
      return 1;
    }
    return -1;
  }
  private char mySymbol;
}

class AnySymbolNode extends Node {
  AnySymbolNode(MatchState matchState) {
    super(matchState);
  }
  @Override
  protected int matchMe(String str, int pos, MatchState matchState) {
    return (pos < str.length() ? 1 : -1);
  }
}

class RangeQuantifierNode extends Node {
  // FIXME: is it ok to use -1 as an indicator of infinity?
  RangeQuantifierNode(Node nextNode, int rangeBegin, int rangeEnd, MatchState matchState) {
    super(matchState);
    myCounter = 0;
    myNextNode = nextNode;
    myRangeBegin = rangeBegin;
    myRangeEnd = rangeEnd;
  }

  @Override
  void clear() {
    super.clear();
    myCounter = 0;
  }

  @Override
  protected boolean matchNext(String str, int strPos, MatchState matchState) {
    // check upper bound
    if (myRangeEnd > -1 && myCounter > myRangeEnd) {
      return false;
    }
    if (super.matchNext(str, strPos, matchState)) {
      return true;
    }
    // check lower bound, and go further if counter is in range
    return (myRangeBegin <= myCounter && myNextNode.match(str, strPos, matchState));
  }

  @Override
  protected int matchMe(String str, int pos, MatchState matchState) {
    ++myCounter;
    return 0;
  }

  private int myCounter;
  // FIXME: use Range class
  private int myRangeBegin, myRangeEnd;
  private Node myNextNode;
}