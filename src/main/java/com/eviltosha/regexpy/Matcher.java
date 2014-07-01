package com.eviltosha.regexpy;

import java.util.*;

/** A class for matching strings against regexp. Multiple matchers for the single Regex are allowed. */
public class Matcher {
  Matcher(final Node startNode, final int numGroups) {
    myStartNode = startNode;
    for (int groupId = 0; groupId <= numGroups; ++groupId) {
      addGroup(groupId);
    }
  }

  public boolean matches(final String str) {
    clear();
    return myStartNode.matchMe(str, 0, this);
  }

  boolean visitAndCheck(final Node node, final int pos) {
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

  int visitCount(final Node node) {
    assert(myVisitCounters.containsKey(node));
    return myVisitCounters.get(node);
  }

  void openGroup(final int groupId, final int strPos) {
    final Range range = new Range();
    range.setBegin(strPos);
    myGroupRanges.get(groupId).push(range);
  }

  void undoOpenGroup(final int groupId) {
    myGroupRanges.get(groupId).pop();
  }

  void closeGroup(final int groupId, final int strPos) {
    assert(!myGroupRanges.get(groupId).isEmpty());
    myGroupRanges.get(groupId).peek().setEnd(strPos);
  }

  void undoCloseGroup(final int groupId) {
    myGroupRanges.get(groupId).peek().resetEnd();
  }

  Range getGroupRange(final int groupId) throws EmptyStackException {
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

  private void addGroup(final int groupId) {
    if (!myGroupRanges.containsKey(groupId)) {
      myGroupRanges.put(groupId, new Stack<Range>());
    }
  }

  // FIXME: probably should use array for this
  private final Map<Integer, Stack<Range>> myGroupRanges = new HashMap<Integer, Stack<Range>>();
  private final Node myStartNode;
  private final Map<Node, Integer> myLastVisitPositions = new HashMap<Node, Integer>();
  private final Map<Node, Integer> myVisitCounters = new HashMap<Node, Integer>();
}
