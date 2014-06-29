package com.eviltosha.regexpy;

import java.lang.Override;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;
import java.util.regex.Pattern;

/**
 * Hello world!
 *
 */

class Range {
  // FIXME: is it ok to use -1 as infinity/not set indicator?
  int myBegin, myEnd;

  Range() { reset(); }
  int length() { return myEnd - myBegin;}
  int getBegin() { return myBegin; }
  int getEnd() { return myEnd; }
  void setBegin(int begin) { myBegin = begin; }
  void setEnd(int end) { myEnd = end; }
  void resetEnd() { myEnd = -1; }
  void reset() {
    myBegin = -1;
    myEnd = -1;
  }
  boolean isDefined() {
    return (myBegin >= 0 && myEnd >= 0);
  }
}

public class Regex {
  public Regex(String regex) throws RegexSyntaxException {
    myNodes = new ArrayList<Node>();
    myNumGroups = 0;
    myGroupRanges = new HashMap<Integer, Stack<Range>>();
    construct(regex);
  }

  public boolean match(String str) {
    graphClear();
    return myStartNode.match(str, 0);
  }

  private Node myStartNode;
  private ArrayList<Node> myNodes;
  private int myNumGroups;
  // FIXME: should this be array? Also can we be fine without additional class?
  // FIXME: what modifiers should it have? (final, ...)
  // FIXME: srsly? HashMap of stacks of ranges?
  private HashMap<Integer, Stack<Range>> myGroupRanges;

  private void graphClear() {
    for (Node node: myNodes) {
      node.clear();
    }
    for (Stack<Range> rangeStack: myGroupRanges.values()) {
      rangeStack.clear();
    }
  }

  private abstract class Node {
    ArrayList<Node> myNextNodes;
    ArrayList<Node> getNextNodes() { return myNextNodes; }

    int myLastVisitPos;

    Node() {
      myNextNodes = new ArrayList<Node>();
      myNodes.add(this);
    }

    boolean match(String str, int strPos) {
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

      int increment = matchPart(str, strPos);
      if (increment == -1) {
        dematchPart();
        return false;
      }
      for (Node node: getNextNodes()) {
        if (node.match(str, strPos + increment)) {
          return true;
        }
      }
      dematchPart();
      return false;
    }

    abstract int matchPart(String str, int strPos);
    void dematchPart() { /* do nothing */ }
    void clear() { myLastVisitPos = -1; }
    boolean isEnd() { return false; }
    void addNextNode(Node node) {
      myNextNodes.add(node);
    }
  }

  private class CharRangeNode extends Node {
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

    CharRangeNode() {
      super();
      myCharRanges = new ArrayList<CharRange>();
      myChars = new ArrayList<Character>();
      myNegate = false;
    }

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
    int matchPart(String str, int strPos) {
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
      Range range = new Range();
      range.setBegin(strPos);
      myGroupRanges.get(myGroupId).push(range);
      return super.matchPart(str, strPos);
    }

    void dematchPart() {
      myGroupRanges.get(myGroupId).pop();
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
      assert(!myGroupRanges.get(myGroupId).isEmpty());
      myGroupRanges.get(myGroupId).peek().setEnd(strPos);
      return super.matchPart(str, strPos);
    }

    void dematchPart() {
      myGroupRanges.get(myGroupId).peek().resetEnd();
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
      // FIXME: probably this will look better with a single try-catch block
      if (myGroupRanges.get(myGroupId).isEmpty()) {
        return -1;
      }
      Range range = myGroupRanges.get(myGroupId).peek();
      if (!range.isDefined()) {
        return -1;
      }
      if (range.length() > str.length() - strPos) {
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

  private class SymbolNode extends Node {
    char mySymbol;

    SymbolNode(char symbol) {
      super();
      mySymbol = symbol;
    }
    @Override
    int matchPart(String str, int pos) {
      if (pos < str.length() && str.charAt(pos) == mySymbol) {
        return 1;
      }
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
      return (myOpen ? 0 : -1);
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

  private void construct(String regex) throws RegexSyntaxException {
    RegexStringProcessor processor = new RegexStringProcessor(regex);
    myStartNode = new EmptyNode();
    Node endNode = new EndNode();
    Node termBeginNode = myStartNode;

    Stack<Node> groupStartNodeStack = new Stack<Node>();
    groupStartNodeStack.push(myStartNode);
    Stack<Node> openGroupNodeStack = new Stack<Node>();
    Stack<Node> closeGroupNodeStack = new Stack<Node>();
    closeGroupNodeStack.push(endNode);

    boolean escaped = false;
    while (processor.hasNext()) {
      Node termEndNode;
      boolean quantifierApplicable = true;
      if (escaped) {
        if (Character.isDigit(processor.peek())) {
          // group recall
          int groupRecallId = processor.eatNumber();
          // FIXME: group recall inside the group itself behavior
          if (myNumGroups < groupRecallId) {
            throw new RegexSyntaxException("Group recall before group definition", processor.getRegex());
          }
          termEndNode = new GroupRecallNode(groupRecallId);
          termBeginNode.addNextNode(termEndNode);
        } else {
          // special character ranges
          switch (processor.peek()) {
            case 'd':
            case 'D':
            case 's':
            case 'S':
              termEndNode = constructSpecialCharRange(processor.next());
              termBeginNode.addNextNode(termEndNode);
              break;
            default:
              // FIXME: if escaped character is not special, exception should be thrown
              termEndNode = new SymbolNode(processor.next());
              termBeginNode.addNextNode(termEndNode);
              break;
          }
        }
        escaped = false;
      } else {
        char ch = processor.next();
        switch (ch) {
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
            ++myNumGroups;
            myGroupRanges.put(myNumGroups, new Stack<Range>());
            OpenGroupNode openNode = new OpenGroupNode(myNumGroups);
            openGroupNodeStack.push(termBeginNode);
            CloseGroupNode closeNode = new CloseGroupNode(myNumGroups);
            closeGroupNodeStack.push(closeNode);
            termEndNode = new EmptyNode();
            groupStartNodeStack.push(termEndNode);
            openNode.addNextNode(termEndNode);

            termBeginNode.addNextNode(openNode);
            termBeginNode = openNode;
            quantifierApplicable = false;
            break;
          }
          case ')': {
            if (openGroupNodeStack.isEmpty()) {
              throw new RegexSyntaxException("Unpaired ')'", processor.getRegex());
            }
            Node openNode = openGroupNodeStack.pop();
            termEndNode = closeGroupNodeStack.pop();
            termBeginNode.addNextNode(termEndNode);
            termBeginNode = openNode;
            groupStartNodeStack.pop();
            break;
          }
          case '[':
            termEndNode = new CharRangeNode(processor);
            termBeginNode.addNextNode(termEndNode);
            break;
          case '\\':
            escaped = true;
            quantifierApplicable = false;
            termEndNode = termBeginNode;
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
    if (!openGroupNodeStack.empty()) {
      throw new RegexSyntaxException("Unpaired '('", processor.getRegex());
    }
    termBeginNode.addNextNode(endNode);
  }

  private Node constructSpecialCharRange(char rangeId) {
    CharRangeNode rangeNode = new CharRangeNode();
    switch (rangeId) {
      case 'D':
        rangeNode.setNegate(true);
        // fall through
      case 'd':
        rangeNode.addCharRange('0', '9');
      break;
      case 'S':
        rangeNode.setNegate(true);
        // fall through
      case 's':
        rangeNode.addChar('\r');
        rangeNode.addChar('\n');
        rangeNode.addChar('\t');
        rangeNode.addChar('\f');
        rangeNode.addChar(' ');
        break;
    }
    return rangeNode;
  }

  private Node tryApplyQuantifier(RegexStringProcessor processor, Node termBeginNode, Node termEndNode)
      throws RegexSyntaxException {
    Node newEmptyNode = new EmptyNode();
    switch (processor.peek()) {
      case '{':
        // FIXME: this logic should be encapsulated
        processor.next();
        int rangeBegin, rangeEnd;
        // we don't perform checks because eatNumber will perform them
        rangeBegin = processor.eatNumber();
        switch (processor.next()) {
          case ',':
            if (processor.peek() == '}') {
              processor.next();
              rangeEnd = -1; // -1 denotes infinity
            } else {
              rangeEnd = processor.eatNumber();
              if (processor.next() != '}') {
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
        if (rangeBegin > rangeEnd && rangeEnd > -1) {
          throw new RegexSyntaxException("Invalid range quantifier parameters", processor.getRegex());
        }
        if (rangeBegin == 0) {
          termBeginNode.addNextNode(newEmptyNode);
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
        processor.next();
        termBeginNode.addNextNode(newEmptyNode);
        termEndNode.addNextNode(newEmptyNode);
        break;
      case '*':
        termBeginNode.addNextNode(newEmptyNode);
        // fall through
      case '+':
        processor.next();
        termEndNode.addNextNode(termBeginNode);
        // fall through
      default:
        // don't eat char here, we'll process it later
        termEndNode.addNextNode(newEmptyNode);
        break;
    }
    return newEmptyNode;
  }
}
