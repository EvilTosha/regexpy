package com.eviltosha.regexpy;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.Stack;

/**
 * Created by eviltosha on 6/30/14.
 */
class Range {
  // FIXME: is it ok to use -1 as infinity/not set indicator?
  Range() { reset(); }

  int length() {
    assert(isDefined());
    return myEnd - myBegin;
  }

  int getBegin() { return myBegin; }
  int getEnd() { return myEnd; }
  void setBegin(int begin) { myBegin = begin; }
  void setEnd(int end) { myEnd = end; }
  boolean beginIsSet() { return (myBegin >= 0); }
  boolean endIsSet() { return (myEnd >= 0); }
  void resetEnd() { myEnd = -1; }

  void reset() {
    myBegin = -1;
    myEnd = -1;
  }

  boolean isDefined() {
    return (beginIsSet() && endIsSet());
  }

  private int myBegin, myEnd;
}

class MatchState {
  public MatchState() {
    myNodes = new ArrayList<Node>();
    myGroupRanges = new HashMap<Integer, Stack<Range>>();
  }

  public void addGroup(int groupId) {
    if (!myGroupRanges.containsKey(groupId)) {
      myGroupRanges.put(groupId, new Stack<Range>());
    }
  }

  public void addNode(Node node) {
    myNodes.add(node);
  }

  public void clear() {
    for (Stack<Range> rangeStack: myGroupRanges.values()) {
      rangeStack.clear();
    }
    for (Node node: myNodes) {
      node.clear();
    }
  }

  public void openGroup(int groupId, int strPos) {
    Range range = new Range();
    range.setBegin(strPos);
    myGroupRanges.get(groupId).push(range);
  }

  public void recoverOpenGroup(int groupId) {
    myGroupRanges.get(groupId).pop();
  }

  public void closeGroup(int groupId, int strPos) {
    assert(!myGroupRanges.get(groupId).isEmpty());
    myGroupRanges.get(groupId).peek().setEnd(strPos);
  }

  public void recoverCloseGroup(int groupId) {
    myGroupRanges.get(groupId).peek().resetEnd();
  }

  public Range getRange(int groupId) throws EmptyStackException {
    assert(myGroupRanges.containsKey(groupId));
    return myGroupRanges.get(groupId).peek();
  }

  // FIXME: what modifiers should it have? (final, ...)
  private HashMap<Integer, Stack<Range>> myGroupRanges;
  private ArrayList<Node> myNodes;
}
