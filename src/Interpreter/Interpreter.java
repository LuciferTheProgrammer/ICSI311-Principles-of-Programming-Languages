package Interpreter;

import AST.*;

import java.util.*;

public class Interpreter {

    public TranNode head;
    public HashMap<String, BuiltInMethodDeclarationNode> builtIn;

    /** Constructor - get the interpreter ready to run. Set members from parameters and "prepare" the class.
     *
     * Store the tran node.
     * Add any built-in methods to the AST
     * @param top - the head of the AST
     */
    public Interpreter(TranNode top) {
        head = top;
        ClassNode cw = new ClassNode();
        cw.name = "console";
        ConsoleWrite cws = new ConsoleWrite();
        cws.name = "write";
        cws.isShared = true;
        cws.isVariadic = true;
        cw.methods.add(cws);
        head.Classes.add(cw);
    }

    /**
     * This is the public interface to the interpreter. After parsing, we will create an interpreter and call start to
     * start interpreting the code.
     *
     * Search the classes in Tran for a method that is "isShared", named "start", that is not private and has no parameters
     * Call "InterpretMethodCall" on that method, then return.
     * Throw an exception if no such method exists.
     */
    public void start() {
        // Find the "start" method
        int numberClasses = head.Classes.size();
        for(int i = 0; i < numberClasses; i++) {
            int numberOfMethods = head.Classes.get(i).methods.size();
            for(int k = 0; k < numberOfMethods; k++) {
                MethodDeclarationNode mde = head.Classes.get(i).methods.get(k);
                boolean shared = head.Classes.get(i).methods.get(k).isShared;
                boolean privateHolder = head.Classes.get(i).methods.get(k).isPrivate;
                String name = head.Classes.get(i).methods.get(k).name;
                int noParam = head.Classes.get(i).methods.get(k).parameters.size();
                if(shared && !privateHolder && name.equals("start") && noParam == 0) {
                    List<InterpreterDataType> param = new ArrayList<>();
                    interpretMethodCall(Optional.empty(), mde, param);
                    return;
                }
            }
        }
        throw new RuntimeException("No 'start' method found");
    }
    //              Running Methods

    /**
     * Find the method (local to this class, shared (like Java's system.out.print), or a method on another class)
     * Evaluate the parameters to have a list of values
     * Use interpretMethodCall() to actually run the method.
     *
     * Call GetParameters() to get the parameter value list
     * Find the method. This is tricky - there are several cases:
     * someLocalMethod() - has NO object name. Look in "object"
     * console.write() - the objectName is a CLASS and the method is shared
     * bestStudent.getGPA() - the objectName is a local or a member
     *
     * Once you find the method, call InterpretMethodCall() on it. Return the list that it returns.
     * Throw an exception if we can't find a match.
     * @param object - the object we are inside right now (might be empty)
     * @param locals - the current local variables
     * @param mc - the method call
     * @return - the return values
     */
    private List<InterpreterDataType> findMethodForMethodCallAndRunIt(Optional<ObjectIDT> object, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc) {
        List<InterpreterDataType> holder = getParameters(object, locals, mc);
        List<InterpreterDataType> result;
        if(mc.objectName.isEmpty() && object.isPresent()) {
            int numMethods = object.get().astNode.methods.size();
            for(int i = 0; i < numMethods; i++) {
                MethodDeclarationNode mde = object.get().astNode.methods.get(i);
                if(mde.name.equals(mc.methodName) && mde.parameters.size() == mc.parameters.size()
                    && mde.returns.size() == mc.returnValues.size()) {
                    result = interpretMethodCall(object, mde, holder);
                    return result;
                }
            }
        }
        else if(mc.objectName.isPresent() && object.isEmpty()) {
            //String builtInName = mc.objectName.get() + "." + mc.methodName;
            //if(builtIn.containsKey(builtInName)) {
            //    ConsoleWrite cr = (ConsoleWrite) builtIn.get(builtInName);
            //    result = interpretMethodCall(Optional.empty(), cr, holder);
            //    return result;
            //}
            int numClasses = head.Classes.size();
            for(int i = 0; i < numClasses; i++) {
                if(mc.objectName.get().equals(head.Classes.get(i).name)) {
                    int methods = head.Classes.get(i).methods.size();
                    for(int k = 0; k < methods; k++) {
                        MethodDeclarationNode mde = head.Classes.get(i).methods.get(k);
                        if(mc.methodName.equals(mde.name) && mde.isShared && mde.parameters.size() == mc.parameters.size()
                            && mde.returns.size() == mc.returnValues.size()) {
                            result = interpretMethodCall(Optional.empty(), mde, holder);
                            return result;
                        }
                    }
                }
            }
        }
        else if(mc.objectName.isPresent()) {
            String placement = mc.objectName.get();
            if(locals.containsKey(placement)) {
                ObjectIDT idt = (ObjectIDT)locals.get(placement);
                int numMethods = idt.astNode.methods.size();
                for(int i = 0; i < numMethods; i++) {
                    MethodDeclarationNode mde = idt.astNode.methods.get(i);
                    if(mde.name.equals(mc.methodName) && mde.parameters.size() == mc.parameters.size() &&
                        mde.returns.size() == mc.returnValues.size()) {
                        result = interpretMethodCall(Optional.of(idt), mde, holder);
                        return result;
                    }
                }
            }
            if(object.isPresent()) {
                if(object.get().members.containsKey(placement)) {
                    ObjectIDT idt = (ObjectIDT) object.get().members.get(placement);
                    int numMethods = idt.astNode.methods.size();
                    for(int i = 0; i < numMethods; i++) {
                        MethodDeclarationNode mde = idt.astNode.methods.get(i);
                        if(mde.name.equals(mc.methodName) && mde.parameters.size() == mc.parameters.size()
                            && mde.returns.size() == mc.returnValues.size()) {
                            result = interpretMethodCall(Optional.of(idt), mde, holder);
                            return result;
                        }
                    }
                }

            }
        }
        throw new RuntimeException("No method to match");
    }

    /**
     * Run a "prepared" method (found, parameters evaluated)
     * This is split from findMethodForMethodCallAndRunIt() because there are a few cases where we don't need to do the finding:
     * in start() and dealing with loops with iterator objects, for example.
     *
     * Check to see if "m" is a built-in. If so, call Execute() on it and return
     * Make local variables, per "m"
     * If the number of passed in values doesn't match m's "expectations", throw
     * Add the parameters by name to locals.
     * Call InterpretStatementBlock
     * Build the return list - find the names from "m", then get the values for those names and add them to the list.
     * @param object - The object this method is being called on (might be empty for shared)
     * @param m - Which method is being called
     * @param values - The values to be passed in
     * @return the returned values from the method
     */
    private List<InterpreterDataType> interpretMethodCall(Optional<ObjectIDT> object, MethodDeclarationNode m, List<InterpreterDataType> values) {
        var retVal = new LinkedList<InterpreterDataType>();
        return retVal;
    }

    //              Running Constructors

    /**
     * This is a special case of the code for methods. Just different enough to make it worthwhile to split it out.
     *
     * Call GetParameters() to populate a list of IDT's
     * Call GetClassByName() to find the class for the constructor
     * If we didn't find the class, throw an exception
     * Find a constructor that is a good match - use DoesConstructorMatch()
     * Call InterpretConstructorCall() on the good match
     * @param callerObj - the object that we are inside when we called the constructor
     * @param locals - the current local variables (used to fill parameters)
     * @param mc  - the method call for this construction
     * @param newOne - the object that we just created that we are calling the constructor for
     */
    private void findConstructorAndRunIt(Optional<ObjectIDT> callerObj, HashMap<String, InterpreterDataType> locals, MethodCallStatementNode mc, ObjectIDT newOne) {
    }

    /**
     * Similar to interpretMethodCall, but "just different enough" - for example, constructors don't return anything.
     *
     * Creates local variables (as defined by the ConstructorNode), calls Instantiate() to do the creation
     * Checks to ensure that the right number of parameters were passed in, if not throw.
     * Adds the parameters (with the names from the ConstructorNode) to the locals.
     * Calls InterpretStatementBlock
     * @param object - the object that we allocated
     * @param c - which constructor is being called
     * @param values - the parameter values being passed to the constructor
     */
    private void interpretConstructorCall(ObjectIDT object, ConstructorNode c, List<InterpreterDataType> values) {
    }

    //              Running Instructions

    /**
     * Given a block (which could be from a method or an "if" or "loop" block, run each statement.
     * Blocks, by definition, do ever statement, so iterating over the statements makes sense.
     *
     * For each statement in statements:
     * check the type:
     *      For AssignmentNode, FindVariable() to get the target. Evaluate() the expression. Call Assign() on the target with the result of Evaluate()
     *      For MethodCallStatementNode, call doMethodCall(). Loop over the returned values and copy the into our local variables
     *      For LoopNode - there are 2 kinds.
     *          Setup:
     *          If this is a Loop over an iterator (an Object node whose class has "iterator" as an interface)
     *              Find the "getNext()" method; throw an exception if there isn't one
     *          Loop:
     *          While we are not done:
     *              if this is a boolean loop, Evaluate() to get true or false.
     *              if this is an iterator, call "getNext()" - it has 2 return values. The first is a boolean (was there another?), the second is a value
     *              If the loop has an assignment variable, populate it: for boolean loops, the true/false. For iterators, the "second value"
     *              If our answer from above is "true", InterpretStatementBlock() on the body of the loop.
     *       For If - Evaluate() the condition. If true, InterpretStatementBlock() on the if's statements. If not AND there is an else, InterpretStatementBlock on the else body.
     * @param object - the object that this statement block belongs to (used to get member variables and any members without an object)
     * @param statements - the statements to run
     * @param locals - the local variables
     */
    private void interpretStatementBlock(Optional<ObjectIDT> object, List<StatementNode> statements, HashMap<String, InterpreterDataType> locals) {
        for(StatementNode statement: statements) {
            if(statement instanceof AssignmentNode a) {
                InterpreterDataType targetResult = findVariable(a.target.name, locals, object);
                InterpreterDataType eval = evaluate(locals, object, a.expression);
                targetResult.Assign(eval);
            }
            else if(statement instanceof MethodCallStatementNode ms) {
                List<InterpreterDataType> result = findMethodForMethodCallAndRunIt(object, locals, ms);
                int numReturns = result.size();
                for(int i  = 0; i < numReturns; i++) {
                    locals.put(ms.returnValues.get(i).name, result.get(i));
                }
            }
            else if(statement instanceof LoopNode loop) {
                if(object.isPresent()) {
                    ObjectIDT obj = object.get();
                    int numInterfaces = obj.astNode.interfaces.size();
                    boolean flagger = false;
                    for(int i = 0; i < numInterfaces; i++) {
                        String currInterface = obj.astNode.interfaces.get(i);
                        if(currInterface.equals("iterator")) {
                            flagger = true;
                            break;
                        }
                    }
                    if(flagger) {
                        boolean flagger2 = false;
                        int numMethods = obj.astNode.methods.size();
                        for (int i = 0; i < numMethods; i++) {
                            MethodDeclarationNode mde = obj.astNode.methods.get(i);
                            // Might implement number of "returns"
                            if(mde.name.equals("getNext") && mde.parameters.isEmpty()) {
                                flagger2 = true;
                                break;
                            }
                        }
                        if(!flagger2) {
                            throw new RuntimeException("No iterator method found");
                        }
                        boolean done = false;
                        while(!done) {
                            InterpreterDataType result = evaluate(locals, object, loop.expression);
                            if(result instanceof BooleanIDT bool){
                                if(loop.assignment.isPresent()) {

                                }
                            }
                        }
                    }
                }
            }
        }

    }

    /**
     *  evaluate() processes everything that is an expression - math, variables, boolean expressions.
     *  There is a good bit of recursion in here, since math and comparisons have left and right sides that need to be evaluated.
     *
     * See the How To Write an Interpreter document for examples
     * For each possible ExpressionNode, do the work to resolve it:
     * BooleanLiteralNode - create a new BooleanLiteralNode with the same value
     *      - Same for all of the basic data types
     * BooleanOpNode - Evaluate() left and right, then perform either and/or on the results.
     * CompareNode - Evaluate() both sides. Do good comparison for each data type
     * MathOpNode - Evaluate() both sides. If they are both numbers, do the math using the built-in operators. Also handle String + String as concatenation (like Java)
     * MethodCallExpression - call doMethodCall() and return the first value
     * VariableReferenceNode - call findVariable()
     * @param locals the local variables
     * @param object - the current object we are running
     * @param expression - some expression to evaluate
     * @return a value
     */
    private InterpreterDataType evaluate(HashMap<String, InterpreterDataType> locals, Optional<ObjectIDT> object, ExpressionNode expression) {
        // Possibly implement BooleanLiteralNode for BooleanIDT later....
        if(expression instanceof NumericLiteralNode number)
            return new NumberIDT(number.value);
        if(expression instanceof StringLiteralNode string)
            return new StringIDT(string.value);
        if(expression instanceof CharLiteralNode chars)
            return new CharIDT(chars.value);
        if(expression instanceof CompareNode c) {
            InterpreterDataType left = evaluate(locals, object, c.left);
            InterpreterDataType right = evaluate(locals, object, c.right);
            if (left instanceof NumberIDT l && right instanceof NumberIDT r) {
                if(c.op == CompareNode.CompareOperations.lt) {
                    return new BooleanIDT(l.Value < r.Value);
                }
                if(c.op == CompareNode.CompareOperations.gt) {
                    return new BooleanIDT(l.Value > r.Value);
                }
                if(c.op == CompareNode.CompareOperations.eq) {
                    return new BooleanIDT(l.Value == r.Value);
                }
                if(c.op == CompareNode.CompareOperations.ne) {
                    return new BooleanIDT(l.Value != r.Value);
                }
                if(c.op == CompareNode.CompareOperations.le) {
                    return new BooleanIDT(l.Value <= r.Value);
                }
                if(c.op == CompareNode.CompareOperations.ge) {
                    return new BooleanIDT(l.Value >= r.Value);
                }
            }
            if (left instanceof CharIDT l && right instanceof CharIDT r) {
                if(c.op == CompareNode.CompareOperations.lt) {
                    return new BooleanIDT(l.Value < r.Value);
                }
                if(c.op == CompareNode.CompareOperations.gt) {
                    return new BooleanIDT(l.Value > r.Value);
                }
                if(c.op == CompareNode.CompareOperations.eq) {
                    return new BooleanIDT(l.Value == r.Value);
                }
                if(c.op == CompareNode.CompareOperations.ne) {
                    return new BooleanIDT(l.Value != r.Value);
                }
                if(c.op == CompareNode.CompareOperations.le) {
                    return new BooleanIDT(l.Value <= r.Value);
                }
                if(c.op == CompareNode.CompareOperations.ge) {
                    return new BooleanIDT(l.Value >= r.Value);
                }
            }
            if (left instanceof StringIDT l && right instanceof StringIDT r) {
                if(c.op == CompareNode.CompareOperations.lt) {
                    return new BooleanIDT(l.Value.compareTo(r.Value) < 0);
                }
                if(c.op == CompareNode.CompareOperations.gt) {
                    return new BooleanIDT(l.Value.compareTo(r.Value) > 0);
                }
                if(c.op == CompareNode.CompareOperations.eq) {
                    return new BooleanIDT(l.Value.compareTo(r.Value) == 0);
                }
                if(c.op == CompareNode.CompareOperations.ne) {
                    return new BooleanIDT(l.Value.compareTo(r.Value) != 0);
                }
                if(c.op == CompareNode.CompareOperations.le) {
                    return new BooleanIDT(l.Value.compareTo(r.Value) <= 0);
                }
                if(c.op == CompareNode.CompareOperations.ge) {
                    return new BooleanIDT(l.Value.compareTo(r.Value) >= 0);
                }
            }

        }
        if(expression instanceof MathOpNode math) {
            InterpreterDataType left = evaluate(locals, object, math.left);
            InterpreterDataType right = evaluate(locals, object, math.right);
            if(left instanceof NumberIDT l && right instanceof NumberIDT r) {
                switch(math.op) {
                    case add -> {
                        return new NumberIDT(l.Value + r.Value);
                    }
                    case subtract -> {
                        return new NumberIDT(l.Value - r.Value);
                    }
                    case multiply -> {
                        return new NumberIDT(l.Value * r.Value);
                    }
                    case divide -> {
                        return new NumberIDT(l.Value / r.Value);
                    }
                    case modulo -> {
                        return new NumberIDT(l.Value % r.Value);
                    }
                }
            }
            if(left instanceof StringIDT l && right instanceof StringIDT r) {
                switch(math.op) {
                    case add -> {
                        return new StringIDT(l.Value + r.Value);
                    }
                }
            }
        }
        if(expression instanceof MethodCallExpressionNode mce) {
            MethodCallStatementNode mse = new MethodCallStatementNode(mce);
            List<InterpreterDataType> result = findMethodForMethodCallAndRunIt(object, locals, mse);
            return result.get(0);

        }
        if(expression instanceof VariableReferenceNode variable) {
            return findVariable(variable.name, locals, object);
        }
        throw new IllegalArgumentException();
    }

    //              Utility Methods

    /**
     * Used when trying to find a match to a method call. Given a method declaration, does it match this methoc call?
     * We double check with the parameters, too, although in theory JUST checking the declaration to the call should be enough.
     *
     * Match names, parameter counts (both declared count vs method call and declared count vs value list), return counts.
     * If all of those match, consider the types (use TypeMatchToIDT).
     * If everything is OK, return true, else return false.
     * Note - if m is a built-in and isVariadic is true, skip all of the parameter validation.
     * @param m - the method declaration we are considering
     * @param mc - the method call we are trying to match
     * @param parameters - the parameter values for this method call
     * @return does this method match the method call?
     */
    private boolean doesMatch(MethodDeclarationNode m, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        if(m instanceof BuiltInMethodDeclarationNode && ((BuiltInMethodDeclarationNode) m).isVariadic) {
            if(m.name.equals(mc.methodName)) {
                return true;
            }
            return false;
        }
        if (m.name.equals(mc.methodName) && m.parameters.size() == mc.parameters.size() && m.parameters.size() == parameters.size()
                && m.returns.size() == mc.returnValues.size()) {
            int numOfParameters = m.parameters.size();
            for (int i = 0; i < numOfParameters; i++) {
                VariableDeclarationNode vr = m.parameters.get(i);
                InterpreterDataType idt = parameters.get(i);
                if(!typeMatchToIDT(vr.type, idt)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Very similar to DoesMatch() except simpler - there are no return values, the name will always match.
     * @param c - a particular constructor
     * @param mc - the method call
     * @param parameters - the parameter values
     * @return does this constructor match the method call?
     */
    private boolean doesConstructorMatch(ConstructorNode c, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        if(c.parameters.size() == mc.parameters.size() && c.parameters.size() == parameters.size()) {
            int numberOfParameters = c.parameters.size();
            for(int i = 0; i < numberOfParameters; i++) {
                VariableDeclarationNode vr = c.parameters.get(i);
                InterpreterDataType idt = parameters.get(i);
                if(!(typeMatchToIDT(vr.type, idt))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Used when we call a method to get the list of values for the parameters.
     *
     * for each parameter in the method call, call Evaluate() on the parameter to get an IDT and add it to a list
     * @param object - the current object
     * @param locals - the local variables
     * @param mc - a method call
     * @return the list of method values
     */
    private List<InterpreterDataType> getParameters(Optional<ObjectIDT> object, HashMap<String,InterpreterDataType> locals, MethodCallStatementNode mc) {
        int numParameters = mc.parameters.size();
        List<InterpreterDataType> result = new ArrayList<>();
        for(int i = 0; i < numParameters; i++) {
            ExpressionNode holder = mc.parameters.get(i);
            InterpreterDataType idt = evaluate(locals, object, holder);
            result.add(idt);
        }
        return result;
    }
    /**
     * Used when we have an IDT and we want to see if it matches a type definition
     * Commonly, when someone is making a function call - do the parameter values match the method declaration?
     *
     * If the IDT is a simple type (boolean, number, etc) - does the string type match the name of that IDT ("boolean", etc)
     * If the IDT is an object, check to see if the name matches OR the class has an interface that matches
     * If the IDT is a reference, check the inner (refered to) type
     * @param type the name of a data type (parameter to a method)
     * @param idt the IDT someone is trying to pass to this method
     * @return is this OK?
     */
    private boolean typeMatchToIDT(String type, InterpreterDataType idt) {
        if(idt instanceof StringIDT || idt instanceof NumberIDT || idt instanceof BooleanIDT ||
            idt instanceof CharIDT || idt instanceof ObjectIDT || idt instanceof ReferenceIDT) {
            if (type.equals("boolean") && idt instanceof BooleanIDT) {
                return true;
            }
            if (type.equals("string") && idt instanceof StringIDT) {
                return true;
            }
            if (type.equals("number") && idt instanceof NumberIDT) {
                return true;
            }
            if (type.equals("character") && idt instanceof CharIDT) {
                return true;
            }
            if (idt instanceof ObjectIDT) {
                ObjectIDT obj = (ObjectIDT) idt;
                if (type.equals(obj.astNode.name)) {
                    return true;
                }
                if (!obj.astNode.interfaces.isEmpty()) {
                    int numInterfaces = obj.astNode.interfaces.size();
                    for (int i = 0; i < numInterfaces; i++) {
                        if (type.equals(obj.astNode.interfaces.get(i))) {
                            return true;
                        }
                    }
                }
            }
            if (idt instanceof ReferenceIDT) {
                ReferenceIDT ref = (ReferenceIDT) idt;
                if (ref.refersTo.isEmpty())
                    return false;
                else {
                    ObjectIDT holder = ref.refersTo.get();
                    return typeMatchToIDT(type, holder);
                }
            }
            return false;
        }
        throw new RuntimeException("Unable to resolve type " + type);
    }

    /**
     * Find a method in an object that is the right match for a method call (same name, parameters match, etc. Uses doesMatch() to do most of the work)
     *
     * Given a method call, we want to loop over the methods for that class, looking for a method that matches (use DoesMatch) or throw
     * @param object - an object that we want to find a method on
     * @param mc - the method call
     * @param parameters - the parameter value list
     * @return a method or throws an exception
     */
    private MethodDeclarationNode getMethodFromObject(ObjectIDT object, MethodCallStatementNode mc, List<InterpreterDataType> parameters) {
        int numbOfMethods = object.astNode.methods.size();
        for(int i = 0; i < numbOfMethods; i++) {
            MethodDeclarationNode mde = object.astNode.methods.get(i);
            if(doesMatch(mde, mc, parameters)) {
                return mde;
            }
        }
        throw new RuntimeException("Unable to resolve method call " + mc);
    }

    /**
     * Find a class, given the name. Just loops over the TranNode's classes member, matching by name.
     *
     * Loop over each class in the top node, comparing names to find a match.
     * @param name Name of the class to find
     * @return either a class node or empty if that class doesn't exist
     */
    private Optional<ClassNode> getClassByName(String name) {
        int numberOfClasses = head.Classes.size();
        for(int i = 0; i < numberOfClasses; i++) {
            if(head.Classes.get(i).name.equals(name)) {
                ClassNode cl = head.Classes.get(i);
                return Optional.of(cl);
            }
        }
        return Optional.empty();
    }

    /**
     * Given an execution environment (the current object, the current local variables), find a variable by name.
     *
     * @param name  - the variable that we are looking for
     * @param locals - the current method's local variables
     * @param object - the current object (so we can find members)
     * @return the IDT that we are looking for or throw an exception
     */
    private InterpreterDataType findVariable(String name, HashMap<String,InterpreterDataType> locals, Optional<ObjectIDT> object) {
        if(locals.containsKey(name)) {
            InterpreterDataType placement  = locals.get(name);
            return placement;
        }
        if(object.isPresent()) {
            if(object.get().members.containsKey(name)) {
                InterpreterDataType holder = object.get().members.get(name);
                return holder;
            }
        }
        throw new RuntimeException("Unable to find variable " + name);
    }

    /**
     * Given a string (the type name), make an IDT for it.
     *
     * @param type The name of the type (string, number, boolean, character). Defaults to ReferenceIDT if not one of those.
     * @return an IDT with default values (0 for number, "" for string, false for boolean, ' ' for character)
     */
    private InterpreterDataType instantiate(String type) {
        switch (type) {
            case "string" -> {
                return new StringIDT("");
            }
            case "number" -> {
                return new NumberIDT(0);
            }
            case "boolean" -> {
                return new BooleanIDT(false);
            }
            case "character" -> {
                return new CharIDT(' ');
            }
            default -> {
                return new ReferenceIDT();
            }
        }
    }
}
