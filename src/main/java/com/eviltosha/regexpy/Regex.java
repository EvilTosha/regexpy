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
    boolean isEnd() { return false; }
    void addNextNode(Node node) {
      myNextNodes.add(node);
      // FIXME: debug output, remove before deployment
//      System.out.println("Connecting nodes: ");
//      print();
//      node.print();
//      System.out.println("-------");
    }

    // FIXME: debug method, remove before deployment
    abstract void print();
  }

  class EmptyNode extends Node {
    @Override
    int matchPart(String str, int strPos) { return 0; }
    @Override
    void print() { System.out.println("Empty node"); }
  }

  class EndNode extends EmptyNode {
    // FIXME: is it ok to override for this particular behavior?
    @Override
    boolean isEnd() { return true; }
  }

  class OpenGroupNode extends EmptyNode {
    int myGroupId;
    OpenGroupNode(int id) { myGroupId = id; }
    @Override
    void print() { System.out.println("Open group node " + myGroupId); }
  }

  class CloseGroupNode extends EmptyNode {
    int myGroupId;
    CloseGroupNode(int id) { myGroupId = id; }
    @Override
    void print() { System.out.println("Close group node " + myGroupId); }
  }

  class SymbolNode extends Node {
    char mySymbol;

    SymbolNode(char symbol) { mySymbol = symbol; }

    @Override
    int matchPart(String str, int pos) {
      // FIXME: is there some clever way to check without explicit check?
      if (pos < str.length() && pos >= 0 && str.charAt(pos) == mySymbol) {
        return 1;
      }
      // FIXME: is it ok to return -1 when no match is possible?
      return -1;
    }
    @Override
    void print() { System.out.println("Symbol node: " + mySymbol); }
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
        case '*':
        case '+':
          // FIXME: is this the correct way to throw exceptions? (there are more occurrences below)
          throw new RegexSyntaxException("Incorrect use of quantifier", regex);
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
            throw new RegexSyntaxException("Unmatched opening parenthesis", regex);
          }

          curNode.addNextNode(openNode);
          curNode = openNode;
          break;
        case ')':
          throw new RegexSyntaxException("Unpaired closing parenthesis", regex);
        default:
          newNode = new SymbolNode(nextChar);
          newNode.print();
          curNode.addNextNode(newNode);
          ++pos;
      }
      Node newEmptyNode = new EmptyNode();
      newNode.addNextNode(newEmptyNode);
      // FIXME: code dubbing, possibly extract or refactor
      // FIXME: is it ok to reuse same var for different purpose?
      // TODO: this code should also apply to groups (and ranges)
      if (pos < endPos && quantifierApplicable) {
        nextChar = regex.charAt(pos);
        switch (nextChar) {
          case '*':
            curNode.addNextNode(newEmptyNode);
            // fall through
          case '+':
            newNode.addNextNode(curNode);
            ++pos;
            // fall through
          default:
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
      // FIXME: is it ok to directly call to fields of nested class? (also in other places in the code)
      node.myLastVisitPos = -1;
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
