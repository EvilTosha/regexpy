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

  // FIXME: are there default setters?
  public Regex(String regex) {
    myNodes = new ArrayList<Node>();
    myStartNode = new EmptyNode();
    Node endNode = new EndNode();
    construct(myStartNode, endNode, regex, 0, regex.length(), 1);
  }

  // FIXME: which modifiers (private/..., static, etc) should apply to inner classes?
  abstract class Node extends Object {
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
  }

  class CloseGroupNode extends EmptyNode {
    int myGroupId;
    CloseGroupNode(int id) {
      super();
      myGroupId = id;
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

  // FIXME: is this method too long?
  private void construct(Node startNode, Node endNode, String regex,
                         int startPos, int endPos, int groupId) {
    int pos = startPos;
    Node curNode = startNode;
    while (pos < endPos) {
      // FIXME: maybe refactor names for better readability
      Node newNode;
      boolean quantifierApplicable = true;
      char nextChar = regex.charAt(pos);
      switch (nextChar) {
        case '{':
        case '*':
        case '+':
          // FIXME: is this the correct way to throw exceptions? (there are more occurrences below)
          throw new RegexSyntaxException("Incorrect use of quantifier", regex);
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
          int newGroupId = groupId + 1;
          int braceCount = 1;
          // FIXME: is there a way not to use increment 2 times?
          ++pos;
          int openGroupPos = pos;
          while (pos < endPos && braceCount > 0) {
            nextChar = regex.charAt(pos);
            switch(nextChar) {
              case '(':
                ++newGroupId;
                ++braceCount;
                break;
              case ')':
                --braceCount;
                break;
              default:
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
        default:
          newNode = new SymbolNode(nextChar);
          curNode.addNextNode(newNode);
          ++pos;
      }
      Node newEmptyNode = new EmptyNode();
      // quantifier application (if present)
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
    for (Node node: myNodes) {
      node.clear();
    }
    return match(str, 0, myStartNode);
  }

  private boolean match(String str, int pos, Node curNode) {
    if (pos == str.length() && curNode.isEnd()) {
      return true;
    }
    // the case (pos == str.length() - 1) and curNode isn't final will be processed below
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
