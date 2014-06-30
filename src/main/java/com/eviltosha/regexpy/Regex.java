package com.eviltosha.regexpy;

import java.util.Stack;

/**
 * Hello world!
 *
 */
public class Regex {
  public Regex(String regex) throws RegexSyntaxException {
    myNumGroups = 0;
    myMatchState = new MatchState();
    construct(regex);
  }

  public boolean match(String str) {
    myMatchState.clear();
    return myStartNode.match(str, 0, myMatchState);
  }

  private Node myStartNode;
  // FIXME: maybe we can just use local var in the construct method
  private int myNumGroups;
  private MatchState myMatchState;

  private void construct(String regex) throws RegexSyntaxException {
    RegexStringProcessor processor = new RegexStringProcessor(regex);
    myStartNode = new EmptyNode(myMatchState);
    Node endNode = new EndNode(myMatchState);
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
          int groupRecallId = processor.nextNumber();
          myMatchState.addGroup(groupRecallId);
          termEndNode = new GroupRecallNode(groupRecallId, myMatchState);
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
              termEndNode = new SymbolNode(processor.next(), myMatchState);
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
            // FIXME: refactor this (names are not always what they represent)
            ++myNumGroups;
            myMatchState.addGroup(myNumGroups);
            OpenGroupNode openNode = new OpenGroupNode(myNumGroups, myMatchState);
            openGroupNodeStack.push(termBeginNode);
            CloseGroupNode closeNode = new CloseGroupNode(myNumGroups, myMatchState);
            closeGroupNodeStack.push(closeNode);
            termEndNode = new EmptyNode(myMatchState);
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
            termEndNode = new CharRangeNode(processor, myMatchState);
            termBeginNode.addNextNode(termEndNode);
            break;
          case '\\':
            escaped = true;
            quantifierApplicable = false;
            termEndNode = termBeginNode;
            break;
          case '.':
            termEndNode = new AnySymbolNode(myMatchState);
            termBeginNode.addNextNode(termEndNode);
            break;
          default:
            termEndNode = new SymbolNode(ch, myMatchState);
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
    CharRangeNode rangeNode = new CharRangeNode(myMatchState);
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
    Node newEmptyNode = new EmptyNode(myMatchState);
    switch (processor.peek()) {
      case '{':
        // FIXME: this logic should be encapsulated
        processor.next();
        int rangeBegin, rangeEnd;
        // we don't perform checks because nextNumber will perform them
        rangeBegin = processor.nextNumber();
        switch (processor.next()) {
          case ',':
            if (processor.peek() == '}') {
              processor.next();
              rangeEnd = -1; // -1 denotes infinity
            } else {
              rangeEnd = processor.nextNumber();
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
        RangeQuantifierNode rangeNode =
            new RangeQuantifierNode(newEmptyNode, rangeBegin, rangeEnd, myMatchState);
        termEndNode.addNextNode(rangeNode);
        rangeNode.addNextNode(termBeginNode);
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
