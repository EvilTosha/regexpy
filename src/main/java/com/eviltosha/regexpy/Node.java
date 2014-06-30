package com.eviltosha.regexpy;

import java.util.ArrayList;
import java.util.EmptyStackException;

/**
* Created by eviltosha on 6/30/14.
*/
// FIXME: refactor methods/fields order && private/public/...
abstract class Node {
  ArrayList<Node> myNextNodes;

  int myLastVisitPos;

  Node(MatchState matchState) {
    matchState.addNode(this);
    myNextNodes = new ArrayList<Node>();
  }

  public boolean match(String str, int strPos, MatchState matchState) {
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

  boolean matchNext(String str, int strPos, MatchState matchState) {
    for (Node node: myNextNodes) {
      if (node.match(str, strPos, matchState)) {
        return true;
      }
    }
    return false;
  }
  abstract int matchMe(String str, int strPos, MatchState matchState);
  void recoverState(MatchState matchState) { /* do nothing */ }
  void clear() { myLastVisitPos = -1; }
  boolean isEnd() { return false; }
  void addNextNode(Node node) {
    myNextNodes.add(node);
  }
}

class CharRangeNode extends Node {
  class CharRange {
    char myBegin, myEnd;
    CharRange(char begin, char end) {
      myBegin = begin;
      myEnd = end;
    }
    boolean has(char ch) {
      return (myBegin <= ch && ch <= myEnd);
    }
  }

  ArrayList<CharRange> myCharRanges;
  ArrayList<Character> myChars;
  boolean myNegate;

  CharRangeNode(MatchState matchState) {
    super(matchState);
    myCharRanges = new ArrayList<CharRange>();
    myChars = new ArrayList<Character>();
    myNegate = false;
  }

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
  int matchMe(String str, int strPos, MatchState matchState) {
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
}

class EmptyNode extends Node {
  EmptyNode(MatchState matchState) {
    super(matchState);
  }
  @Override
  int matchMe(String str, int strPos, MatchState matchState) { return 0; }
}

class EndNode extends EmptyNode {
  EndNode(MatchState matchState) {
    super(matchState);
  }
  @Override
  boolean isEnd() { return true; }
}

class OpenGroupNode extends EmptyNode {
  int myGroupId;
  OpenGroupNode(int id, MatchState matchState) {
    super(matchState);
    myGroupId = id;
  }
  @Override
  int matchMe(String str, int strPos, MatchState matchState) {
    matchState.openGroup(myGroupId, strPos);
    return super.matchMe(str, strPos, matchState);
  }

  void recoverState(MatchState matchState) {
    matchState.recoverOpenGroup(myGroupId);
  }
}

class CloseGroupNode extends EmptyNode {
  int myGroupId;
  CloseGroupNode(int id, MatchState matchState) {
    super(matchState);
    myGroupId = id;
  }
  @Override
  int matchMe(String str, int strPos, MatchState matchState) {
    matchState.closeGroup(myGroupId, strPos);
    return super.matchMe(str, strPos, matchState);
  }

  void recoverState(MatchState matchState) {
    matchState.recoverCloseGroup(myGroupId);
  }
}

// FIXME: group zero (add or specify as excluded functionality)
class GroupRecallNode extends Node {
  int myGroupId;
  GroupRecallNode(int id, MatchState matchState) {
    super(matchState);
    myGroupId = id;
  }
  @Override
  int matchMe(String str, int strPos, MatchState matchState) {
    // FIXME: probably this will look better with a single try-catch block
    Range range;
    try {
      range = matchState.getRange(myGroupId);
    } catch (EmptyStackException e) {
      return -1;
    }
    if (!range.isDefined() || range.length() > str.length() - strPos) {
      return -1;
    }
    for (int offset = 0; offset < range.length(); ++offset) {
      if (range.getBegin() + offset >= str.length() || strPos + offset >= str.length() ||
          str.charAt(range.getBegin() + offset) != str.charAt(strPos + offset)) {
        return -1;
      }
    }
    return range.length();
  }
}

class SymbolNode extends Node {
  char mySymbol;

  SymbolNode(char symbol, MatchState matchState) {
    super(matchState);
    mySymbol = symbol;
  }
  @Override
  int matchMe(String str, int pos, MatchState matchState) {
    if (pos < str.length() && str.charAt(pos) == mySymbol) {
      return 1;
    }
    return -1;
  }
}

class AnySymbolNode extends Node {
  AnySymbolNode(MatchState matchState) {
    super(matchState);
  }
  @Override
  int matchMe(String str, int pos, MatchState matchState) {
    return (pos < str.length() ? 1 : -1);
  }
}

class RangeQuantifierNode extends Node {
  int myCounter;
  int myRangeBegin, myRangeEnd;
  Node myNextNode;
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
  boolean matchNext(String str, int strPos, MatchState matchState) {
    if (myRangeEnd > -1 && myCounter > myRangeEnd) {
      return false;
    }
    if (super.matchNext(str, strPos, matchState)) {
      return true;
    }
    if (myRangeBegin <= myCounter && (myCounter <= myRangeEnd || myRangeEnd == -1)) {
      return myNextNode.match(str, strPos, matchState);
    }
    return false;
  }
  @Override
  int matchMe(String str, int pos, MatchState matchState) {
    ++myCounter;
    return 0;
  }
}