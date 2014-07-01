package com.eviltosha.regexpy;

import java.util.*;

public class Matcher {
  public Matcher(Node startNode, HashSet<Integer> groupIds) {
    myStartNode = startNode;
    myGroupRanges = new HashMap<Integer, Stack<Range>>();
    for (int groupId: groupIds) {
      addGroup(groupId);
    }
    myLastVisitPositions = new HashMap<Node, Integer>();
    myVisitCounters = new HashMap<Node, Integer>();
  }

  public boolean matches(String str) {
    myLastString = str;
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

  // FIXME: bad name
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
  private String myLastString;
}
