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

public class Matcher {
  public Matcher(Node startNode, ArrayList<Integer> groupIds) {
    myStartNode = startNode;
    myGroupRanges = new HashMap<Integer, Stack<Range>>();
    for (int groupId: groupIds) {
      addGroup(groupId);
    }
    myLastVisitPositions = new HashMap<Node, Integer>();
    myVisitCounters = new HashMap<Node, Integer>();
  }

  public boolean match(String str) {
    clear();
    return myStartNode.matchMe(str, 0, this);
  }

  public void addGroup(int groupId) {
    if (!myGroupRanges.containsKey(groupId)) {
      myGroupRanges.put(groupId, new Stack<Range>());
    }
  }

  public boolean visitAndCheck(Node node, int pos) {
    // to avoid looping with empty string
    if (myLastVisitPositions.containsKey(node) && myLastVisitPositions.get(node) == pos) {
      return false;
    }
    myLastVisitPositions.put(node, pos);
    if (myVisitCounters.containsKey(node)) {
      myVisitCounters.put(node, myVisitCounters.get(node) + 1);
    }
    else {
      myVisitCounters.put(node, 1);
    }
    return true;
  }

  public int visitCount(Node node) {
    assert(myVisitCounters.containsKey(node));
    return myVisitCounters.get(node);
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

  public Range getGroupRange(int groupId) throws EmptyStackException {
    assert(myGroupRanges.containsKey(groupId));
    return myGroupRanges.get(groupId).peek();
  }

  private void clear() {
    for (Stack<Range> rangeStack: myGroupRanges.values()) {
      rangeStack.clear();
    }
    myLastVisitPositions.clear();
    myVisitCounters.clear();
  }

  private final HashMap<Integer, Stack<Range>> myGroupRanges;
  private final Node myStartNode;
  private final HashMap<Node, Integer> myLastVisitPositions;
  private final HashMap<Node, Integer> myVisitCounters;
}
