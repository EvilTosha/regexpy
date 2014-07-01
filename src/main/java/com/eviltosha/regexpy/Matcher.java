package com.eviltosha.regexpy;

import java.util.*;

public class Matcher {
  public Matcher(Node startNode, int numGroups) {
    myStartNode = startNode;
    for (int groupId = 0; groupId <= numGroups; ++groupId) {
      addGroup(groupId);
    }
  }

  public boolean matches(String str) {
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

  public void undoOpenGroup(int groupId) {
    myGroupRanges.get(groupId).pop();
  }

  public void closeGroup(int groupId, int strPos) {
    assert(!myGroupRanges.get(groupId).isEmpty());
    myGroupRanges.get(groupId).peek().setEnd(strPos);
  }

  public void undoCloseGroup(int groupId) {
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

  private final Map<Integer, Stack<Range>> myGroupRanges = new HashMap<Integer, Stack<Range>>();
  private final Node myStartNode;
  private final Map<Node, Integer> myLastVisitPositions = new HashMap<Node, Integer>();
  private final Map<Node, Integer> myVisitCounters = new HashMap<Node, Integer>();
}
