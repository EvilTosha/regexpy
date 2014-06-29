package com.eviltosha.regexpy;

import java.lang.Override;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Hello world!
 *
 */
public class Regex
{
  public Regex(String regex) throws RegexSyntaxException {
    myNodes = new ArrayList<Node>();
    myNumGroups = 0;
    construct(regex);
    myGroupRanges = new Range[myNumGroups];
    for (int i = 0; i < myNumGroups; ++i) {
      myGroupRanges[i] = new Range();
    }
  }

  public boolean match(String str) {
    graphClear();
    return match(str, 0, myStartNode);
  }

  // FIXME: should it be encapsulated to own class?
  private Node myStartNode;
  private ArrayList<Node> myNodes;
  private int myNumGroups;
  // FIXME: should this be array? Also can we be fine without additional class?
  // FIXME: what modifiers should it have? (final, ...)
  private Range[] myGroupRanges;

  private class Range {
    // FIXME: is it ok to use -1 as infinity/not set indicator?
    int myBegin, myEnd;

    Range() { reset(); }
    int length() { return myEnd - myBegin;}
    int getBegin() { return myBegin; }
    int getEnd() { return myEnd; }
    void setBegin(int begin) { myBegin = begin; }
    void setEnd(int end) { myEnd = end; }
    void reset() {
      myBegin = -1;
      myEnd = -1;
    }
    boolean isDefined() {
      return (myBegin >= 0 && myEnd >= 0);
    }
  }

  private class CharRange {
    char myBegin, myEnd;
    CharRange(char begin, char end) {
      myBegin = begin;
      myEnd = end;
    }
    boolean has(char ch) {
      return (myBegin <= ch && ch <= myEnd);
    }
  }

  private void graphClear() {
    for (Node node: myNodes) {
      node.clear();
    }
    for (Range range: myGroupRanges) {
      range.reset();
    }
  }

  // FIXME: which modifiers (private/..., static, etc) should apply to inner classes?
  private abstract class Node {
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

  private class CharRangeNode extends Node {
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

  private class EmptyNode extends Node {
    @Override
    int matchPart(String str, int strPos) { return 0; }
  }

  private class EndNode extends EmptyNode {
    @Override
    boolean isEnd() { return true; }
  }

  private class OpenGroupNode extends EmptyNode {
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

  private class CloseGroupNode extends EmptyNode {
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
  private class GroupRecallNode extends Node {
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
      for (int offset = 0; offset < range.length(); ++offset) {
        if (str.charAt(range.getBegin() + offset) != str.charAt(strPos + offset)) {
          return -1;
        }
      }
      return range.length();
    }
  }

  private class SymbolNode extends Node {
    char mySymbol;

    SymbolNode(char symbol) {
      super();
      mySymbol = symbol;
    }
    @Override
    int matchPart(String str, int pos) {
      // FIXME: is there some clever way to check without explicit check?
      if (pos < str.length() && str.charAt(pos) == mySymbol) {
        return 1;
      }
      // FIXME: is it ok to return -1 when no match is possible?
      return -1;
    }
  }

  private class AnySymbolNode extends Node {
    @Override
    int matchPart(String str, int pos) {
      return (pos < str.length() ? 1 : -1);
    }
  }

  private class GateNode extends Node {
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

  private class RangeQuantifierNode extends Node {
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
  private void construct(String regex) throws RegexSyntaxException{
    RegexStringProcessor processor = new RegexStringProcessor(regex);
    myStartNode = new EmptyNode();
    Node endNode = new EndNode();
    Node termBeginNode = myStartNode;

    // FIXME: refactor this
    Stack<Node> groupStartNodeStack = new Stack<Node>();
    groupStartNodeStack.push(myStartNode);
    Stack<OpenGroupNode> groupNodeStack = new Stack<OpenGroupNode>();
    // FIXME: also refactor this
    Stack<Node> closeGroupNodeStack = new Stack<Node>();
    closeGroupNodeStack.push(endNode);

    boolean escaped = false;
    while (processor.hasNext()) {
      Node termEndNode;
      boolean quantifierApplicable = true;
      char ch = processor.peek();
      if (escaped) {
        if (Character.isDigit(ch)) {
          // we subtract 1, because groups in regex start with 1, but array indices start with 0
          int groupRecallId = processor.eatNumber() - 1;
          // FIXME: group recall inside the group itself behavior
          if (myNumGroups <= groupRecallId) {
            throw new RegexSyntaxException("Group recall before group definition", processor.getRegex());
          }
          termEndNode = new GroupRecallNode(groupRecallId);
          termBeginNode.addNextNode(termEndNode);
        } else {
          switch (ch) {
            case 'd': {
              // FIXME: code dubbing
              processor.eatSilently();
              CharRangeNode rangeNode = new CharRangeNode();
              rangeNode.addCharRange('0', '9');
              termEndNode = rangeNode;
              termBeginNode.addNextNode(termEndNode);
              break;
            }
            case 'D': {
              processor.eatSilently();
              CharRangeNode rangeNode = new CharRangeNode();
              rangeNode.addCharRange('0', '9');
              rangeNode.setNegate(true);
              termEndNode = rangeNode;
              termBeginNode.addNextNode(termEndNode);
              break;
            }
            case 's': {
              processor.eatSilently();
              CharRangeNode rangeNode = new CharRangeNode();
              rangeNode.addChar('\r');
              rangeNode.addChar('\n');
              rangeNode.addChar('\t');
              rangeNode.addChar('\f');
              termEndNode = rangeNode;
              termBeginNode.addNextNode(termEndNode);
              break;
            }
            case 'S': {
              processor.eatSilently();
              CharRangeNode rangeNode = new CharRangeNode();
              rangeNode.addChar('\r');
              rangeNode.addChar('\n');
              rangeNode.addChar('\t');
              rangeNode.addChar('\f');
              rangeNode.setNegate(true);
              termEndNode = rangeNode;
              termBeginNode.addNextNode(termEndNode);
              break;
            }
            default:
              processor.eatSilently();
              termEndNode = new SymbolNode(ch);
              termBeginNode.addNextNode(termEndNode);
              break;
          }
        }
        escaped = false;
      } else {
        switch (processor.eat()) {
          case '{':
          case '*':
          case '+':
            throw new RegexSyntaxException("Incorrect use of quantifier", processor.getRegex());
          case '|':
            termBeginNode.addNextNode(closeGroupNodeStack.peek());
            termEndNode = groupStartNodeStack.peek();
            quantifierApplicable = false;
            break;
          case '(': { // artificially create scope to reuse some variable names in other cases
            OpenGroupNode openNode = new OpenGroupNode(myNumGroups);
            groupNodeStack.push(openNode);
            CloseGroupNode closeNode = new CloseGroupNode(myNumGroups);
            closeGroupNodeStack.push(closeNode);
            termEndNode = new EmptyNode();
            groupStartNodeStack.push(termEndNode);
            openNode.addNextNode(termEndNode);
            ++myNumGroups;

            termBeginNode.addNextNode(openNode);
            termBeginNode = openNode;
            quantifierApplicable = false;
            break;
          }
          case ')': {
            if (groupNodeStack.isEmpty()) {
              throw new RegexSyntaxException("Unpaired ')'", processor.getRegex());
            }
            OpenGroupNode openNode = groupNodeStack.pop();
            termEndNode = closeGroupNodeStack.pop();
            termBeginNode.addNextNode(termEndNode);
            termBeginNode = openNode;
            groupStartNodeStack.pop();
            break;
          }
          case '[':
            CharRangeNode rangeNode = new CharRangeNode();
            // first characters that need special treatment: '^' (negates range),
            // '-' (in first position it acts like literal hyphen, also can be part of a range),
            // ']' (in first position it acts like literal closing square bracket, also can be part of a range)
            ch = processor.eat();
            if (ch == '^') {
              rangeNode.setNegate(true);
              // we need to perform the first character analysis once more (for special '-' and ']' cases)
              ch = processor.eat();
            }
            // we store parsed char,
            // if next char is not '-', we add it as a char, otherwise construct range
            char storedChar = ch;
            // FIXME: this var seems unnecessary; maybe use Character for storedChar and use null check?
            boolean charIsStored = true;
            boolean asRange = false;
            boolean charRangeFinished = false;
            while (processor.hasNext() && !charRangeFinished) {
              ch = processor.eat();
              switch (ch) {
                case ']':
                  if (charIsStored) {
                    rangeNode.addChar(storedChar);
                    // if '-' stands right before the closing bracket it's treated as literal '-'
                    if (asRange) {
                      rangeNode.addChar('-');
                    }
                  }
                  charRangeFinished = true;
                  break;
                case '-':
                  if (!charIsStored || asRange) {
                    // check whether it's the last char in group (like in "[a--]")
                    if (processor.eat() == ']') {
                      if (asRange) {
                        rangeNode.addCharRange(storedChar, '-');
                      } else {
                        rangeNode.addChar('-');
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
                      rangeNode.addCharRange(storedChar, ch);
                      charIsStored = false;
                    } else {
                      rangeNode.addChar(storedChar);
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
            // FIXME: this is obviously bad code, refactor
            termEndNode = rangeNode;
            termBeginNode.addNextNode(termEndNode);
            break;
          case '\\':
            escaped = true;
            quantifierApplicable = false;
            // FIXME: this is not necessary
            termEndNode = new EmptyNode();
            termBeginNode.addNextNode(termEndNode);
            break;
          case '.':
            termEndNode = new AnySymbolNode();
            termBeginNode.addNextNode(termEndNode);
            break;
          default:
            termEndNode = new SymbolNode(ch);
            termBeginNode.addNextNode(termEndNode);
            break;
        }
      }
      // quantifier application (if present & applicable)
      if (processor.hasNext() && quantifierApplicable) {
        termBeginNode = tryApplyQuantifier(processor, termBeginNode, termEndNode);
      } else {
        termBeginNode = termEndNode;
      }
    }
    termBeginNode.addNextNode(endNode);
  }

  private Node tryApplyQuantifier(RegexStringProcessor processor, Node termBeginNode, Node termEndNode)
      throws RegexSyntaxException {
    Node newEmptyNode = new EmptyNode();
    switch (processor.peek()) {
      case '{':
        processor.eatSilently();
        int rangeBegin, rangeEnd;
        // we don't perform checks because eatNumber will perform them
        rangeBegin = processor.eatNumber();
        switch (processor.eat()) {
          case ',':
            if (processor.peek() == '}') {
              processor.eatSilently();
              rangeEnd = -1; // -1 denotes infinity
            } else {
              rangeEnd = processor.eatNumber();
              if (processor.eat() != '}') {
                throw new RegexSyntaxException("Malformed range quantifier", processor.getRegex());
              }
            }
            break;
          case '}':
            rangeEnd = rangeBegin; // single number range
            break;
          default:
            throw new RegexSyntaxException("Invalid range quantifier", processor.getRegex());
        }
        GateNode gateNode = new GateNode();
        RangeQuantifierNode rangeNode =
            new RangeQuantifierNode(gateNode, rangeBegin, rangeEnd);
        rangeNode.addNextNode(gateNode);
        rangeNode.addNextNode(termBeginNode);
        termEndNode.addNextNode(rangeNode);
        gateNode.addNextNode(newEmptyNode);
        break;
      case '?':
        processor.eatSilently();
        termBeginNode.addNextNode(newEmptyNode);
        termEndNode.addNextNode(newEmptyNode);
        break;
      case '*':
        termBeginNode.addNextNode(newEmptyNode);
        // fall through
      case '+':
        processor.eatSilently();
        termEndNode.addNextNode(termBeginNode);
        // fall through
      default:
        // don't eat char here, we'll process it later
        termEndNode.addNextNode(newEmptyNode);
        break;
    }
    return newEmptyNode;
  }

  private boolean match(String str, int pos, Node curNode) {
    if (pos == str.length() && curNode.isEnd()) {
      return true;
    }
    // the case (pos == str.length()) and curNode isn't final will be processed below
    if (pos > str.length()) {
      return false;
    }

    // FIXME: is it ok to directly call to fields of a nested class? (also in other places in the code)
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
