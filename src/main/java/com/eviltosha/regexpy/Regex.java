package com.eviltosha.regexpy;

import java.lang.Override;
import java.util.ArrayList;

/**
 * Hello world!
 *
 */
public class Regex
{
  // FIXME: should we store regex string?
  // FIXME: should it be encapsulated to own class?
  private Node myStartNode;

  // FIXME: is it ok to use ArrayList for this purpose? (also later in the code)
  private ArrayList<Node> myNodes;

  // FIXME: what else should this class have? (what methods?)
  class Range {
    // FIXME: is it ok to use -1 as infinity/not set indicator?
    private int myBegin, myEnd;

    Range() { reset(); }
    Range(int begin, int end) {
      myBegin = begin;
      myEnd = end;
    }
    int length() { return myEnd - myBegin;}
    int getBegin() { return myBegin; }
    int getEnd() { return myEnd; }
    void setBegin(int begin) { myBegin = begin; }
    void setEnd(int end) { myEnd = end; }
    void setRange(int begin, int end) {
      myBegin = begin;
      myEnd = end;
    }
    void reset() {
      myBegin = -1;
      myEnd = -1;
    }
    boolean isDefined() {
      return (myBegin >= 0 && myEnd >= 0);
    }
  }

  // FIXME: can we have only one range class?
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

  private int myNumGroups;
  // FIXME: should this be array? Also can we be fine without additional class?
  // FIXME: what modifiers should it have? (final, ...)
  private Range[] myGroupRanges;


  private void graphClear() {
    for (Node node: myNodes) {
      node.clear();
    }
    for (Range range: myGroupRanges) {
      range.reset();
    }
  }

  public Regex(String regex) {
    myNodes = new ArrayList<Node>();
    myStartNode = new EmptyNode();
    Node endNode = new EndNode();
    myNumGroups = 0;
    construct(myStartNode, endNode, regex, 0, regex.length(), 0);
    myGroupRanges = new Range[myNumGroups];
    for (int i = 0; i < myNumGroups; ++i) {
      myGroupRanges[i] = new Range();
    }
  }

  // FIXME: which modifiers (private/..., static, etc) should apply to inner classes?
  abstract class Node {
    ArrayList<Node> myNextNodes;
    ArrayList<Node> getNextNodes() { return myNextNodes; }

    int myLastVisitPos;

    Node() {
      myNextNodes = new ArrayList<Node>();
      myNodes.add(this);
    }

    abstract int matchPart(String str, int strPos);
    void clear() { myLastVisitPos = -1; }
    boolean isEnd() { return false; }
    void addNextNode(Node node) {
      myNextNodes.add(node);
    }
  }

  // FIXME: this class has char interface, but uses Character internally. Probably that's not good
  class CharRangeNode extends Node {
    ArrayList<CharRange> myCharRanges;
    ArrayList<Character> myChars;
    boolean myNegate;

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
    void addCharRange(char begin, char end) {
      myCharRanges.add(new CharRange(begin, end));
    }
    @Override
    int matchPart(String str, int strPos) {
      if (strPos >= str.length()) {
        return -1;
      }
      char strChar = str.charAt(strPos);
      for (Character character: myChars) {
        if (strChar == character) {
          // FIXME: is it ok to use ternary if? (also in code below)
          // FIXME: (myNegate ? -1 : 1) used three times in the code. Use variable?
          return (myNegate ? -1 : 1);
        }
      }
      for (CharRange range: myCharRanges) {
        if (range.has(strChar)) {
          return (myNegate ? -1 : 1);
        }
      }
      return (myNegate ? 1 : -1);
    }
  }

  class EmptyNode extends Node {
    @Override
    int matchPart(String str, int strPos) { return 0; }
  }

  class EndNode extends EmptyNode {
    // FIXME: is it ok to override for this particular behavior?
    @Override
    boolean isEnd() { return true; }
  }

  class OpenGroupNode extends EmptyNode {
    int myGroupId;
    OpenGroupNode(int id) {
      super();
      myGroupId = id;
    }
    @Override
    int matchPart(String str, int strPos) {
      myGroupRanges[myGroupId].setBegin(strPos);
      return super.matchPart(str, strPos);
    }
  }

  class CloseGroupNode extends EmptyNode {
    int myGroupId;
    CloseGroupNode(int id) {
      super();
      myGroupId = id;
    }
    @Override
    int matchPart(String str, int strPos) {
      myGroupRanges[myGroupId].setEnd(strPos);
      return super.matchPart(str, strPos);
    }
  }

  // FIXME: group zero (add or specify as excluded functionality)
  class GroupRecallNode extends Node {
    int myGroupId;
    GroupRecallNode(int id) {
      super();
      myGroupId = id;
    }
    @Override
    int matchPart(String str, int strPos) {
      Range range = myGroupRanges[myGroupId];
      // FIXME: is it ok to use assert?
      assert(range.isDefined());
      if (range.length() > str.length() - strPos) {
        return -1;
      }
      for (int i = 0; i < range.length(); ++i) {
        if (str.charAt(range.getBegin() + i) != str.charAt(strPos + i)) {
          return -1;
        }
      }
      return range.length();
    }
  }

  class SymbolNode extends Node {
    char mySymbol;

    SymbolNode(char symbol) {
      super();
      mySymbol = symbol;
    }
    @Override
    int matchPart(String str, int pos) {
      // FIXME: is there some clever way to check without explicit check?
      if (pos < str.length() && pos >= 0 && str.charAt(pos) == mySymbol) {
        return 1;
      }
      // FIXME: is it ok to return -1 when no match is possible?
      return -1;
    }
  }

  class GateNode extends Node {
    boolean myOpen;

    GateNode() {
      super();
      myOpen = true;
    }

    void setOpen(boolean open) { myOpen = open; }
    @Override
    int matchPart(String str, int pos) {
      // FIXME: use ternary if?
      if (myOpen) { return 0; } else { return -1; }
    }
  }

  class RangeQuantifierNode extends Node {
    int myCounter;
    int myRangeBegin, myRangeEnd;
    // FIXME: does this ruin consistency of the Node's classes?
    GateNode myGateNode;
    // FIXME: is it ok to use -1 as an indicator of infinity?
    RangeQuantifierNode(GateNode gateNode, int rangeBegin, int rangeEnd) {
      super();
      myCounter = 0;
      myGateNode = gateNode;
      myRangeBegin = rangeBegin;
      myRangeEnd = rangeEnd;
    }
    @Override
    void clear() {
      super.clear();
      myCounter = 0;
    }
    @Override
    int matchPart(String str, int pos) {
      ++myCounter;
      if (myRangeEnd > -1 && myCounter > myRangeEnd) {
        return -1;
      }
      myGateNode.setOpen(myRangeBegin <= myCounter && (myCounter <= myRangeEnd || myRangeEnd == -1));
      return 0;
    }
  }

  // FIXME: is this method too long? Yes it is, even Idea says so
  private void construct(Node startNode, Node endNode, String regex,
                         int startPos, int endPos, int groupId) {
    int pos = startPos;
    boolean escaped = false;
    Node curNode = startNode;
    while (pos < endPos) {
      // FIXME: maybe refactor names for better readability
      Node newNode;
      boolean quantifierApplicable = true;
      char nextChar = regex.charAt(pos);
      if (escaped) {
        if (Character.isDigit(nextChar)) {
          int newPos = pos + 1;
          while (newPos < endPos && Character.isDigit(regex.charAt(newPos))) {
            ++newPos;
          }
          // FIXME: try-catch?
          // we subtract 1, because groups in regex start with 1, but array indices start with 0
          int groupRecallId = Integer.parseInt(regex.substring(pos, newPos)) - 1;
          // FIXME: group recall inside the group itself behavior
          if (groupId <= groupRecallId) {
            throw new RegexSyntaxException("Group recall before group definition", regex);
          }
          newNode = new GroupRecallNode(groupRecallId);
          curNode.addNextNode(newNode);
          pos = newPos;
        } else {
          switch (nextChar) {
            // TODO: add special characters and escape sequences
            default:
              newNode = new SymbolNode(nextChar);
              curNode.addNextNode(newNode);
              ++pos;
              break;
          }
        }
        escaped = false;
      } else {
        switch (nextChar) {
          case '{':
          case '*':
          case '+':
            // FIXME: is this the correct way to throw exceptions? (there are more occurrences below)
            throw new RegexSyntaxException("Incorrect use of quantifier", regex);
          // FIXME: this is not special character (shouldn't be an error); also add test
          case '}':
            throw new RegexSyntaxException("Unmatched '}'", regex);
          case '|':
            curNode.addNextNode(endNode);
            newNode = startNode;
            quantifierApplicable = false;
            ++pos;
            break;
          case '(':
            Node openNode = new OpenGroupNode(groupId);
            Node groupStartNode = new EmptyNode();
            openNode.addNextNode(groupStartNode);
            newNode = new CloseGroupNode(groupId);
            // we do this increment every time we encounter an open brace, so we'll count all the groups
            ++myNumGroups;
            int newGroupId = groupId + 1;
            int braceCount = 1;
            // FIXME: is there a way not to use increment 2 times?
            ++pos;
            int openGroupPos = pos;
            while (pos < endPos && braceCount > 0) {
              nextChar = regex.charAt(pos);
              switch (nextChar) {
                // FIXME: ([)]) case; also write test(s)
                case '(':
                  ++newGroupId;
                  ++braceCount;
                  break;
                case ')':
                  --braceCount;
                  break;
                default:
                  // do nothing
                  break;
              }
              ++pos;
            }
            if (braceCount == 0) {
              construct(groupStartNode, newNode, regex, openGroupPos, pos - 1, groupId + 1);
              groupId = newGroupId;
            } else {
              throw new RegexSyntaxException("Unmatched '('", regex);
            }
            curNode.addNextNode(openNode);
            curNode = openNode;
            break;
          case ')':
            throw new RegexSyntaxException("Unpaired ')'", regex);
          case '[':
            CharRangeNode rangeNode = new CharRangeNode();
            // FIXME: newNode is of type Node; is it ok to use CharRangeNode methods?
            // first characters that need special treatment: '^' (negates range),
            // '-' (in first position it acts like literal hyphen, also can be part of a range),
            // ']' (in first position it acts like literal closing square bracket, also can be part of a range)
            ++pos;
            nextChar = regex.charAt(pos);
            // we store parsed char,
            // if next char is not '-', we add it as a char, otherwise construct range
            char storedChar;
            // FIXME: this var seems unnecessary; maybe use Character for using null?
            boolean charIsStored = false;
            boolean asRange = false;
            if (nextChar == '^') {
              rangeNode.setNegate(true);
              // we need to perform first character analysis once more (for special '-' and ']' cases)
              ++pos;
              nextChar = regex.charAt(pos);
            }
            storedChar = nextChar;
            charIsStored = true;
            // FIXME: too many ++pos and no boundary checks (also in other places in the code)
            ++pos;
            boolean rangeClosed = false;
            while (pos < endPos && !rangeClosed) {
              nextChar = regex.charAt(pos);
              switch (nextChar) {
                case ']':
                  if (charIsStored) {
                    rangeNode.addChar(storedChar);
                    // if '-' stands right before the closing bracket it's treated as literal '-'
                    if (asRange) {
                      rangeNode.addChar('-');
                    }
                  }
                  rangeClosed = true;
                  break;
                case '-':
                  if (!charIsStored || asRange) {
                    // FIXME: [a-] and test
                    // check whether it's the last char in group
                    ++pos;
                    nextChar = regex.charAt(pos);
                    if (nextChar == ']') {
                      rangeNode.addChar('-');
                      rangeClosed = true;
                    } else {
                      throw new RegexSyntaxException("Incorrect use of hyphen inside char range", regex);
                    }
                  }
                  asRange = true;
                  break;
                default:
                  if (charIsStored) {
                    if (asRange) {
                      rangeNode.addCharRange(storedChar, nextChar);
                    } else {
                      rangeNode.addChar(nextChar);
                    }
                    charIsStored = false;
                  } else {
                    storedChar = nextChar;
                    charIsStored = true;
                  }
                  asRange = false;
                  break;
              }
              ++pos;
            }
            if (pos == endPos) {
              throw new RegexSyntaxException("Unclosed char range", regex);
            }
            // FIXME: this is obviously bad code, refactor
            newNode = rangeNode;
            curNode.addNextNode(newNode);
            break;
          case '\\':
            escaped = true;
            quantifierApplicable = false;
            // FIXME: this is not necessary
            newNode = new EmptyNode();
            curNode.addNextNode(newNode);
            ++pos;
            break;
          default:
            newNode = new SymbolNode(nextChar);
            curNode.addNextNode(newNode);
            ++pos;
            break;
        }
      }
      Node newEmptyNode = new EmptyNode();
      // quantifier application (if present & applicable)
      // FIXME: is it ok to reuse same var for different purpose?
      if (pos < endPos && quantifierApplicable) {
        nextChar = regex.charAt(pos);
        switch (nextChar) {
          case '{':
            int indexOfComma = -1;
            int openBracePos = pos;
            boolean endReached = false;
            while (pos < endPos - 1 && !endReached) {
              ++pos;
              // FIXME: is it ok to reuse nextChar here?
              nextChar = regex.charAt(pos);
              if (nextChar == ',') {
                if (indexOfComma != -1) {
                  throw new RegexSyntaxException("Double comma in quantifier range", regex);
                }
                indexOfComma = pos;
              } else if (nextChar == '}') {
                endReached = true;
                int rangeBegin, rangeEnd;
                if (indexOfComma == -1) {
                  try {
                    rangeBegin = Integer.parseInt(regex.substring(openBracePos + 1, pos));
                    rangeEnd = rangeBegin;
                    // FIXME: is this the correct way to rethrow exceptions?
                  } catch (NumberFormatException e) {
                    throw new RegexSyntaxException("Illegal range quantifier", regex);
                  }
                } else {
                  try {
                    rangeBegin = Integer.parseInt(regex.substring(openBracePos + 1, indexOfComma));
                    if (indexOfComma + 1 == pos) {
                      rangeEnd = -1;
                    } else {
                      rangeEnd = Integer.parseInt(regex.substring(indexOfComma + 1, pos));
                    }
                  } catch (NumberFormatException e) {
                    throw new RegexSyntaxException("Illegal range quantifier", regex);
                  }
                }
                GateNode gateNode = new GateNode();
                RangeQuantifierNode rangeNode =
                    new RangeQuantifierNode(gateNode, rangeBegin, rangeEnd);
                rangeNode.addNextNode(gateNode);
                rangeNode.addNextNode(curNode);
                newNode.addNextNode(rangeNode);
                gateNode.addNextNode(newEmptyNode);
                curNode = newEmptyNode;
              }
            }
            ++pos;
            break;
          case '*':
            curNode.addNextNode(newEmptyNode);
            // fall through
          case '+':
            newNode.addNextNode(curNode);
            ++pos;
            // fall through
          default:
            newNode.addNextNode(newEmptyNode);
            // we don't increment pos here, because we'll process this char next in the loop
            curNode = newEmptyNode;
        }
      } else {
        curNode = newNode;
      }
    }
    curNode.addNextNode(endNode);
  }

  public boolean match(String str) {
    graphClear();
    return match(str, 0, myStartNode);
  }

  private boolean match(String str, int pos, Node curNode) {
    if (pos == str.length() && curNode.isEnd()) {
      return true;
    }
    // the case (pos == str.length()) and curNode isn't final will be processed below
    if (pos > str.length()) {
      return false;
    }

    // FIXME: is it ok to directly call to fields of nested class? (also in other places in the code)
    // to avoid looping with empty string
    if (curNode.myLastVisitPos == pos) {
      return false;
    }

    curNode.myLastVisitPos = pos;

    for (Node nextNode: curNode.getNextNodes()) {
      int increment = nextNode.matchPart(str, pos);
      if (increment > -1 && match(str, pos + increment, nextNode)) {
        return true;
      }
    }
    return false;
  }
}
