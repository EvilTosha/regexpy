package com.eviltosha.regexpy;

import java.util.ArrayList;
import java.util.Stack;

/**
 *
 */
public class Regex {
  /**
   *
   * @param regex
   * @throws RegexSyntaxException
   */
  public Regex(String regex) throws RegexSyntaxException {
    myRegexString = regex;
    myStartNode = new EmptyNode();
    myGroupIds = new ArrayList<Integer>();
    parse(regex);
  }

  public Matcher matcher() {
    return new Matcher(myStartNode, myGroupIds);
  }

  public boolean match(String str) {
    return matcher().match(str);
  }

  private final String myRegexString;
  private final Node myStartNode;
  private final ArrayList<Integer> myGroupIds;

  private void parse(String regex) throws RegexSyntaxException {
    RegexStringProcessor processor = new RegexStringProcessor(regex);
    Node endNode = new EndNode();
    Node termBeginNode = myStartNode;
    int groupId = 0;

    // TODO: write documentation for these stacks
    Stack<Node> groupStartNodeStack = new Stack<Node>();
    groupStartNodeStack.push(myStartNode);
    Stack<Node> groupEndNodeStack = new Stack<Node>();
    groupEndNodeStack.push(endNode);
    Stack<Node> openGroupNodeStack = new Stack<Node>();

    boolean escaped = false;
    while (processor.hasNext()) {
      Node termEndNode;
      boolean quantifierApplicable = true;
      if (escaped) {
        if (Character.isDigit(processor.peek())) {
          // group recall
          int groupRecallId = processor.nextNumber();
          myGroupIds.add(groupRecallId);
          termEndNode = new GroupRecallNode(groupRecallId);
        } else {
          // special character ranges
          switch (processor.peek()) {
            case 'd':
            case 'D':
            case 's':
            case 'S':
              termEndNode = constructSpecialCharRange(processor.next());
              break;
            default:
              // FIXME: if escaped character is not special, exception should be thrown
              termEndNode = new SymbolNode(processor.next());
              break;
          }
        }
        termBeginNode.addNextNode(termEndNode);
        escaped = false;
      } else {
        char ch = processor.next();
        switch (ch) {
          case '{':
          case '*':
          case '+':
            throw new RegexSyntaxException("Incorrect use of quantifier", myRegexString);
          case '|':
            termBeginNode.addNextNode(groupEndNodeStack.peek());
            termEndNode = groupStartNodeStack.peek();
            quantifierApplicable = false;
            break;
          case '(': { // artificially create scope to reuse some variable names in other cases
            ++groupId;
            myGroupIds.add(groupId);
            // we store this OpenGroupNode, so after the group is closed quantifiers could use it
            // as a termBeginNode
            openGroupNodeStack.push(termBeginNode);

            Node openNode = new OpenGroupNode(groupId);
            termBeginNode.addNextNode(openNode);
            // we create and store CloseGroupNode, so '|' could use it as the end of the group node
            Node closeNode = new CloseGroupNode(groupId);
            groupEndNodeStack.push(closeNode);
            // we store this EmptyNode, so '|' could use it as the start of the group node
            termEndNode = new EmptyNode();
            groupStartNodeStack.push(termEndNode);
            openNode.addNextNode(termEndNode);

            quantifierApplicable = false;
            break;
          }
          case ')': {
            if (openGroupNodeStack.isEmpty()) {
              throw new RegexSyntaxException("Unpaired ')'", myRegexString);
            }
            Node openNode = openGroupNodeStack.pop();
            termEndNode = groupEndNodeStack.pop();
            groupStartNodeStack.pop();

            termBeginNode.addNextNode(termEndNode);
            termBeginNode = openNode;
            break;
          }
          case '[':
            termEndNode = constructCharRangeNode(processor);
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
        // FIXME: refactor for better readability (explicitly create newEmptyNode here)
        termBeginNode = tryApplyQuantifier(processor, termBeginNode, termEndNode);
      } else {
        termBeginNode = termEndNode;
      }
    }
    if (!openGroupNodeStack.empty()) {
      throw new RegexSyntaxException("Unpaired '('", myRegexString);
    }
    termBeginNode.addNextNode(endNode);
  }

  private Node constructCharRangeNode(RegexStringProcessor processor) {
    CharRangeNode charRangeNode = new CharRangeNode();
    // first characters that need special treatment: '^' (negates range),
    // '-' (in first position it acts like literal hyphen, also can be part of a range),
    // ']' (in first position it acts like literal closing square bracket, also can be part of a range)
    char ch = processor.next();
    if (ch == '^') {
      charRangeNode.setNegate(true);
      // we need to perform the first character analysis once more (for special '-' and ']' cases)
      ch = processor.next();
    }
    // we store parsed char,
    // if next char is not '-', we add it as a char, otherwise construct range
    char storedChar = ch;
    boolean charIsStored = true;
    boolean asRange = false;
    boolean charRangeFinished = false;
    while (processor.hasNext() && !charRangeFinished) {
      ch = processor.next();
      switch (ch) {
        case ']':
          if (charIsStored) {
            charRangeNode.addChar(storedChar);
            // if '-' stands right before the closing bracket it's treated as literal '-'
            if (asRange) {
              charRangeNode.addChar('-');
            }
          }
          charRangeFinished = true;
          break;
        case '-':
          if (!charIsStored || asRange) {
            // check whether it's the last char in group (like in "[a--]")
            if (processor.next() == ']') {
              if (asRange) {
                if (storedChar > '-') {
                  throw new RegexSyntaxException("Invalid char range", myRegexString);
                }
                charRangeNode.addCharRange(storedChar, '-');
              } else {
                charRangeNode.addChar('-');
              }
              charRangeFinished = true;
            } else {
              throw new RegexSyntaxException("Incorrect use of hyphen inside char range", myRegexString);
            }
          }
          asRange = true;
          break;
        default:
          if (charIsStored) {
            if (asRange) {
              if (storedChar > ch) {
                throw new RegexSyntaxException("Invalid char range", myRegexString);
              }
              charRangeNode.addCharRange(storedChar, ch);
              charIsStored = false;
            } else {
              charRangeNode.addChar(storedChar);
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
      throw new RegexSyntaxException("Unclosed char range", myRegexString);
    }
    return charRangeNode;
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
                throw new RegexSyntaxException("Malformed range quantifier", myRegexString);
              }
            }
            break;
          case '}':
            rangeEnd = rangeBegin; // single number range
            break;
          default:
            throw new RegexSyntaxException("Invalid range quantifier", myRegexString);
        }
        if (rangeBegin > rangeEnd && rangeEnd > -1) {
          throw new RegexSyntaxException("Invalid range quantifier parameters", myRegexString);
        }
        if (rangeBegin == 0) {
          termBeginNode.addNextNode(newEmptyNode);
        }
        RangeQuantifierNode rangeNode =
            new RangeQuantifierNode(newEmptyNode, rangeBegin, rangeEnd);
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
