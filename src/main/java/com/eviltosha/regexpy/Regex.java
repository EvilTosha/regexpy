package com.eviltosha.regexpy;

import com.eviltosha.regexpy.RegexSyntaxException;

import java.lang.Exception;
import java.lang.Override;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Hello world!
 *
 */
public class Regex
{
  // FIXME: are there default setters?
  public Regex(String regex) {
    myRegex = regex;
    myGraph = new Graph();
    myGraph.construct(regex);
  }

  // FIXME: which modifiers (private/..., static, etc) should apply to inner classes?
  static abstract class Node extends Object {
    ArrayList<Node> myNextNodes;

    ArrayList<Node> getNextNodes() { return myNextNodes; }

    Node() {
      myNextNodes = new ArrayList<Node>();
    }

    abstract int matchPart(String str, int strPos);
    boolean isFinal() { return false; }
    void addNextNode(Node node) {
      myNextNodes.add(node);
      // FIXME: debug output, remove before deployment
      System.out.println("Connecting nodes: ");
      print();
      node.print();
      System.out.println("-------");
    }

    // FIXME: debug method, remove before deployment
    abstract void print();
  }

  static class EmptyNode extends Node {
    @Override
    int matchPart(String str, int strPos) { return 0; }
    @Override
    void print() { System.out.println("Empty node"); }
  }

  static class FinalNode extends EmptyNode {
    // FIXME: is it ok to override for this particular behavior?
    @Override
    boolean isFinal() { return true; }
  }

  static class SymbolNode extends Node {
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

  static class Graph extends Object {
    Node myStart = new EmptyNode();

    // FIXME: should we pass myRegex, or use it directly from here?
    void construct(String regex) {
      int pos = 0;
      Node curNode = myStart;
      while (pos < regex.length()) {
        char nextChar = regex.charAt(pos);
        // FIXME: google style guide for switches (and find all other switches in the code)
        switch (nextChar) {
          case '*':
            // fall through
          case '+':
            // FIXME: is this the correct way to throw exceptions?
            throw new RegexSyntaxException("Incorrect use of quantifier", regex);
          default:
            Node newNode = new SymbolNode(nextChar);
            newNode.print();
            curNode.addNextNode(newNode);
            ++pos;
            // FIXME: code dubbing, possibly extract or refactor
            // FIXME: is it ok to reuse same var for different purpose?
            if (pos < regex.length()) {
              nextChar = regex.charAt(pos);
              switch (nextChar) {
                case '*':
                  newNode.addNextNode(newNode);
                  Node newEmptyNode = new EmptyNode();
                  newNode.addNextNode(newEmptyNode);
                  curNode.addNextNode(newEmptyNode);
                  curNode = newEmptyNode;
                  ++pos;
                  break;
                case '+':
                  newNode.addNextNode(newNode);
                  ++pos;
                  // fall through
                default:
                  // we don't increment pos here, because we'll process this char next in the loop
                  curNode = newNode;
              }
            } else {
              curNode = newNode;
            }
        }
      }
      Node finalNode = new FinalNode();
      curNode.addNextNode(finalNode);
    }

    boolean match(String str) {
      // FIXME: this is quite dirty hack, should bypass it
      return match(str, 0, myStart);
    }

    // FIXME: is it ok to override (or how's it called?) with same name but different signature and number of parameters?
    boolean match(String str, int pos, Node curNode) {
      if (pos == str.length() && curNode.isFinal()) {
        return true;
      }
      // the case (pos == str.length() - 1) and curNode isn't final will be processed below
      if (pos > str.length()) {
        return false;
      }

      // FIXME: is it ok to get internal field without getter?
      for (Node nextNode: curNode.getNextNodes()) {
        int increment = nextNode.matchPart(str, pos);
        if (increment > -1 && match(str, pos + increment, nextNode)) {
          return true;
        }
      }
      return false;
    }
  }

  public boolean match(String str) {
    return myGraph.match(str);
  }

  private Graph myGraph;
  private String myRegex;
}
